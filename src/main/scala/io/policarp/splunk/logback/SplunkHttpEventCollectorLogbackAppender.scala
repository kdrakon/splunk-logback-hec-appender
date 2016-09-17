package io.policarp.splunk.logback

import java.util.concurrent.LinkedBlockingQueue

import ch.qos.logback.classic.spi.{ ILoggingEvent, LoggingEvent }
import ch.qos.logback.classic.{ Level, Logger }
import ch.qos.logback.core.AppenderBase
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{ Consumer, Observable }
import org.http4s._
import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import org.slf4j.LoggerFactory
import scodec.bits.ByteVector

import scala.beans.BeanProperty
import scala.concurrent.duration._

class SplunkHttpEventCollectorLogbackAppender extends AppenderBase[ILoggingEvent] {

  private implicit val scheduler = Scheduler.Implicits.global // TODO replace

  @BeanProperty var splunkUrl: String = ""
  @BeanProperty var token: String = ""
  @BeanProperty var queue: Int = 1000
  @BeanProperty var buffer: Int = 10
  @BeanProperty var flush: Int = 10
  @BeanProperty var layout: SplunkHttpEventCollectorJsonLayout = new SplunkHttpEventCollectorJsonLayout()

  private lazy val logPublisher = new LogPublisher(queue)
  private lazy val logStream = Observable.fromReactivePublisher(logPublisher).bufferTimedAndCounted(flush seconds, buffer)
  private lazy val logConsumer = Consumer.foreachParallelAsync[Task[Unit]](8)(task => task)

  //val httpClient = AsyncHttpClient()

  private lazy val splunkHeaders = Headers(
    Header("Authorization", s"Splunk ${Option(token).getOrElse("")}")
  )

  private val PostTask = (events: Seq[ILoggingEvent]) => Task { // TODO refactor this out
    //httpClient.fetch(Request(Method.POST, headers = splunkHeaders, body = parse(event)))(r => ???)
    events.foreach(e => println(layout.doLayout(e)))
  }

  private def parse(event: ILoggingEvent): EntityBody = scalaz.stream.Process.eval {
    scalaz.concurrent.Task.now(ByteVector(layout.doLayout(event).getBytes(Charset.`UTF-8`.nioCharset)))
  }

  private def ready = (splunkUrl != null) && (token != null)

  override def append(event: ILoggingEvent) = if (ready) {
    logPublisher.enqueue(event) // TODO add async feature
  }

  override def start() = {
    super.start()
    logStream.map(PostTask).runWith(logConsumer).runAsync
  }
}

private[logback] class LogPublisher(queueSize: Int)(implicit scheduler: Scheduler) extends Publisher[ILoggingEvent] {

  val logQueue = new LinkedBlockingQueue[ILoggingEvent](queueSize)

  def enqueue(event: ILoggingEvent) = logQueue.put(event)

  override def subscribe(subscriber: Subscriber[_ >: ILoggingEvent]) = {
    subscriber.onSubscribe(new Subscription {

      @volatile var cancelled = false

      override def cancel() = { cancelled = true }

      override def request(n: Long) = if (!cancelled) {
        Task {
          for (i <- 0L to n) {
            subscriber.onNext(logQueue.take())
          }
        }.runAsync
      }
    })
  }
}

object Test extends App {

  val logger = LoggerFactory.getLogger("test")

  logger.info("started")
  logger.info("1")
  logger.info("2")
  logger.info("3")
  logger.info("4")

  new Thread {
    override def run = logger.warn("IN THREAD", new NoSuchElementException("fdsfsfsf"))
  }.start()

  new Thread {
    override def run = {
      try {
        class Blah {
          val n = 0
          val x = 123 / n
        }
        new Blah()
      } catch {
        case e => logger.error("IN THREAD 2", e)
      }
    }
  }.start()

  Thread.sleep(30000)
}