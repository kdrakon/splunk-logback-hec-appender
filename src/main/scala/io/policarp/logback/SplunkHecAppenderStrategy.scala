package io.policarp.logback

import java.util.concurrent.{ Executors, ForkJoinPool }

import ch.qos.logback.classic.spi.ILoggingEvent

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext

trait SplunkHecAppenderStrategy {

  def append(event: ILoggingEvent)(implicit logPublisher: LogPublisher): Unit = {
    Option(event).foreach(e => {
      event.prepareForDeferredProcessing()
      event.getCallerData
      doAppend(event, logPublisher)
    })
  }

  def doAppend(event: ILoggingEvent, logPublisher: LogPublisher): Unit
}

/**
 * Will block on enqueue of logging if the log buffer is currently filled.
 *
 * Warning: This can cause issues with the application if the Splunk HEC API requests
 * are not executing fast enough and holding up the main execution.
 */
case class BlockingSplunkHecAppenderStrategy() extends SplunkHecAppenderStrategy {
  override def doAppend(event: ILoggingEvent, logPublisher: LogPublisher) = {
    logPublisher.enqueue(event)
  }
}

/**
 * Will create an asynchronous task to run in the background that will attempt to enqueue a log event.
 * If the queue buffer is full, that thread will block separately from the calling application thread.
 *
 * Warning: This could cause issues if the Spunk HEC API is slow. Background enqueue tasks will pile up
 * and possibly utilise CPU resources more.
 */
case class AsyncSplunkHecAppenderStrategy() extends SplunkHecAppenderStrategy {

  import monix.eval.Task
  import monix.execution.Scheduler

  @BeanProperty var parallelism: Int = Runtime.getRuntime.availableProcessors()
  private lazy val scheduler: Scheduler =
    Scheduler.apply(ExecutionContext.fromExecutorService(new ForkJoinPool(parallelism)))

  override def doAppend(event: ILoggingEvent, logPublisher: LogPublisher) = {
    Task {
      logPublisher.enqueue(event)
    }.runAsync(scheduler)
  }
}

/**
 * Will attempt to enqueue a log event if there is capacity only.
 *
 * Warning: Detecting remaining capacity is not guaranteed with the concurrent nature of the
 * [[LogPublisher]], so some enqueues will block.
 */
case class SpillingSplunkHecAppenderStrategy() extends SplunkHecAppenderStrategy {
  override def doAppend(event: ILoggingEvent, logPublisher: LogPublisher) = {
    if (logPublisher.logQueue.remainingCapacity > 0) {
      logPublisher.enqueue(event)
    }
  }
}