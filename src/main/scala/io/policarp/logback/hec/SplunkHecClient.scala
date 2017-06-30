package io.policarp.logback.hec

import java.nio.charset.Charset

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import monix.eval.{Task, TaskCircuitBreaker}
import skinny.http.{HTTP, Request}

import scala.beans.BeanProperty
import scala.concurrent.duration._

trait SplunkHecClient {
  import SplunkHecClient._

  @BeanProperty var splunkUrl: String = ""
  @BeanProperty var token: String = ""

  type AbstractRequest
  type PreparedRequest = (Seq[ILoggingEvent], LayoutBase[ILoggingEvent]) => Option[AbstractRequest]

  private[hec] def prepareRequest: PreparedRequest

  private[hec] def executeRequest(abstractRequest: AbstractRequest): Unit

  /**
   * The Task responsible for posting the logs to the Splunk Cloud API endpoint
   *
   * @param events the ILoggingEvent's in batch that are ready to be posted to Splunk
   * @param layout the layout to use when posting the events
   * @return the Monix Task will asynchronously perform the job of posting the logs and will return Unit
   */
  def postTask(events: Seq[ILoggingEvent])(implicit layout: LayoutBase[ILoggingEvent]): Task[Unit] = {
    SplunkHecClientCircuitBreaker.protect(
      Task {
        prepareRequest(events, layout).foreach(executeRequest)
      }
    )
  }
}

object SplunkHecClient {

  private[hec] val SplunkHecClientCircuitBreaker = TaskCircuitBreaker (
    maxFailures = 5,
    resetTimeout = 5 seconds,
    exponentialBackoffFactor = 2,
    maxResetTimeout = 10 minute
  )

  /**
   * Creates a newline separated list of individual Splunk JSON events
   */
  def formatJsonEvents(events: Seq[ILoggingEvent], layout: LayoutBase[ILoggingEvent]): Option[String] = {
    events match {
      case Nil => None
      case _ => Some(events.map(event => layout.doLayout(event)).mkString("\n\n"))
    }
  }
}

package object skinnyhttp {

  /**
   * An implementation of SplunkHecClient using the skinny-framework's HTTP client
   */
  trait SkinnyHecClient extends SplunkHecClient {

    import SplunkHecClient.formatJsonEvents

    override type AbstractRequest = skinny.http.Request

    override private[hec] def prepareRequest = (events, layout) => {
      formatJsonEvents(events, layout).map(jsonEvents => {
        Request(splunkUrl)
          .header("Authorization", s"Splunk ${Option(token).getOrElse("")}")
          .body(jsonEvents.getBytes(Charset.forName(HTTP.DEFAULT_CHARSET)), "application/json")
      })
    }

    override private[hec] def executeRequest(request: Request) = {
      HTTP.post(request)
    }
  }

  /**
   * This filter is needed to stop the endless feedback loop from the skinny-framework
   * HTTP Client's internal logging
   */
  class SkinnyHttpLogFilter extends Filter[ILoggingEvent] {

    val HttpLoggerName = classOf[HTTP].getName

    override def decide(event: ILoggingEvent): FilterReply = {
      // check with startsWith due to $ appended with Scala classes
      if (event.getLoggerName.startsWith(HttpLoggerName)) {
        FilterReply.DENY
      } else {
        FilterReply.NEUTRAL
      }
    }
  }

}