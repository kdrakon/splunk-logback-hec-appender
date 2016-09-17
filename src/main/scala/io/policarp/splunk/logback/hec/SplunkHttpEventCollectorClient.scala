package io.policarp.splunk.logback.hec

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import monix.eval.Task
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.{ Charset, _ }
import scodec.bits.ByteVector

import scala.beans.BeanProperty

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

package object http4s {

  /**
   * An implementation of SplunkHttpEventCollectorClient using http4s as the HTTP client
   */
  trait Http4sHecClient extends SplunkHttpEventCollectorClient {

    private val httpClient = AsyncHttpClient()

    private lazy val splunkHeaders = Headers(
      Header("Authorization", s"Splunk ${Option(token).getOrElse("")}")
    )

    private def parse(event: ILoggingEvent, layout: LayoutBase[ILoggingEvent]): EntityBody = scalaz.stream.Process.eval {
      scalaz.concurrent.Task.now(ByteVector(layout.doLayout(event).getBytes(Charset.`UTF-8`.nioCharset)))
    }

    override def postTask(events: Seq[ILoggingEvent])(implicit layout: LayoutBase[ILoggingEvent]) = Task[Unit] {
      events.foreach(e => println(layout.doLayout(e)))
    }
  }

}