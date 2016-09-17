package io.policarp.splunk.logback.hec

import java.nio.charset.Charset

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import monix.eval.Task
import skinny.http.{ HTTP, Request }

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext

trait SplunkHttpEventCollectorClient {

  @BeanProperty var splunkUrl: String = ""
  @BeanProperty var token: String = ""

  /**
   * The Task responsible for posting the logs to the Splunk Cloud API endpoint
   *
   * @param events the ILoggingEvent's in batch that are ready to be posted to Splunk
   * @param layout the layout to use when posting the events
   * @return Unit the Monix Task will asynchronously perform the job of posting the logs and will return Unit
   */
  def postTask(events: Seq[ILoggingEvent])(implicit layout: LayoutBase[ILoggingEvent]): Task[Unit]
}

package object skinnyhttp {

  /**
   * An implementation of SplunkHttpEventCollectorClient using the skinny-framework's HTTP client
   */
  trait SkinnyHttpHecClient extends SplunkHttpEventCollectorClient {

    implicit val ec: ExecutionContext

    override def postTask(events: Seq[ILoggingEvent])(implicit layout: LayoutBase[ILoggingEvent]) = Task[Unit] {

      events.foreach(event => {

        val request =
          Request(splunkUrl)
            .header("Authorization", s"Splunk ${Option(token).getOrElse("")}")
            .body(layout.doLayout(event).getBytes(Charset.forName(HTTP.DEFAULT_CHARSET)), "application/json")

        HTTP.asyncPost(request)
      })
    }
  }

}