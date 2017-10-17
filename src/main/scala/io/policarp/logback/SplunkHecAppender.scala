package io.policarp.logback

import java.util.concurrent.ConcurrentLinkedQueue

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.policarp.logback.hec.SplunkHecClient
import io.policarp.logback.hec.skinnyhttp.{SkinnyHecClient, SkinnyHttpLogFilter}
import monix.eval.{Task, TaskCircuitBreaker}
import monix.execution.Scheduler
import monix.execution.atomic.AtomicLong
import monix.reactive.{Consumer, Observable}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.beans.BeanProperty
import scala.concurrent.duration._

class SplunkHecAppender extends SplunkHecAppenderBase with SkinnyHecClient {
  this.addFilter(new SkinnyHttpLogFilter())
}

trait SplunkHecAppenderBase extends AppenderBase[ILoggingEvent] {
  self: SplunkHecClient =>

  @BeanProperty var buffer: Int = 25
  @BeanProperty var flush: Int = 30
  @BeanProperty var parallelism: Int = Runtime.getRuntime.availableProcessors()
  @BeanProperty var layout: SplunkHecJsonLayoutBase = SplunkHecJsonLayout()

  private implicit val scheduler = Scheduler.computation(parallelism)

  private implicit lazy val logPublisher = new LogPublisher()
  private lazy val logStream = Observable.fromReactivePublisher(logPublisher).bufferTimedAndCounted(flush seconds, buffer)
  private lazy val logConsumer = Consumer.foreachParallelAsync[Task[Unit]](parallelism)(task => task)

  override def start() = {
    super.start()
    implicit val impliedLayout = layout
    logStream.map(postTask).consumeWith(logConsumer).runAsync
  }

  override def append(event: ILoggingEvent) = logPublisher.enqueue(event)
}

private[logback] class LogPublisher()(implicit scheduler: Scheduler) extends Publisher[ILoggingEvent] {

  val logQueue = new ConcurrentLinkedQueue[ILoggingEvent]()

  private val LogPublisherCircuitBreaker = TaskCircuitBreaker (
    maxFailures = 5,
    resetTimeout = 10 nanos,
    exponentialBackoffFactor = 2,
    maxResetTimeout = 1 minute
  )

  def enqueue(event: ILoggingEvent) = logQueue.add(event)

  implicit class SlowRestartingTask[A](t: Task[A]) {
    def onErrorRestartDelayedIf(delay: FiniteDuration)(p: (Throwable) => Boolean): Task[A] = {
      t.delayExecution(delay).onErrorHandleWith(ex => if (p(ex)) t.onErrorRestartDelayedIf(delay)(p) else Task.raiseError(ex))
    }
  }

  override def subscribe(subscriber: Subscriber[_ >: ILoggingEvent]) = {
    subscriber.onSubscribe(new Subscription {

      @volatile var cancelled = false

      override def cancel() = { cancelled = true }

      override def request(n: Long) = if (!cancelled) {
        val requests = AtomicLong(n)
        val pollingTask = Task {
          val currentRequests = requests.get
          for (_ <- 1L to currentRequests) {
            Option(logQueue.poll()) match {
              case None =>
                throw new IllegalStateException("logQueue is empty") // so trigger circuit breaker
              case Some(event) =>
                subscriber.onNext(event)
                requests.decrement()
            }
          }
        }
        LogPublisherCircuitBreaker.protect(pollingTask).onErrorRestartDelayedIf(20.millis)(_ => requests.get != 0).runAsync
      }
    })
  }
}
