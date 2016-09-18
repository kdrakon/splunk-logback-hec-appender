package io.policarp.logback

import java.util.concurrent.LinkedBlockingQueue

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.policarp.logback.hec.SplunkHecClient
import io.policarp.logback.hec.skinnyhttp.{ SkinnyHecClient, SkinnyHttpLogFilter }
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{ Consumer, Observable }
import org.reactivestreams.{ Publisher, Subscriber, Subscription }

import scala.beans.BeanProperty
import scala.concurrent.duration._

class SplunkHecAppender extends SplunkHecAppenderBase with SkinnyHecClient {
  implicit val ec = scala.concurrent.ExecutionContext.global // TODO replace
  this.addFilter(new SkinnyHttpLogFilter())
}

trait SplunkHecAppenderBase extends AppenderBase[ILoggingEvent] {
  self: SplunkHecClient =>

  private implicit val scheduler = Scheduler.Implicits.global // TODO replace

  @BeanProperty var queue: Int = 1000
  @BeanProperty var buffer: Int = 25
  @BeanProperty var flush: Int = 30
  @BeanProperty var parallelism: Int = 8
  @BeanProperty var layout: SplunkHecJsonLayoutBase = new SplunkHecJsonLayout()

  private lazy val logPublisher = new LogPublisher(queue)
  private lazy val logStream = Observable.fromReactivePublisher(logPublisher).bufferTimedAndCounted(flush seconds, buffer)
  private lazy val logConsumer = Consumer.foreachParallelAsync[Task[Unit]](parallelism)(task => task)

  override def start() = {
    super.start()
    implicit val impliedLayout = layout
    logStream.map(postTask).runWith(logConsumer).runAsync
  }

  override def append(event: ILoggingEvent) = {
    logPublisher.enqueue(event) // TODO add async feature and overflow strategy
  }
}

private class LogPublisher(queueSize: Int)(implicit scheduler: Scheduler) extends Publisher[ILoggingEvent] {

  val logQueue = new LinkedBlockingQueue[ILoggingEvent](queueSize)

  def enqueue(event: ILoggingEvent) = logQueue.put(event)

  override def subscribe(subscriber: Subscriber[_ >: ILoggingEvent]) = {
    subscriber.onSubscribe(new Subscription {

      @volatile var cancelled = false

      override def cancel() = { cancelled = true }

      override def request(n: Long) = if (!cancelled) {
        Task {
          for (i <- 1L to n) {
            subscriber.onNext(logQueue.take())
          }
        }.runAsync
      }
    })
  }
}
