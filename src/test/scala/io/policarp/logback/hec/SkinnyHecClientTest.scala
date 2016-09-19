package io.policarp.logback.hec

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import io.policarp.logback.MockLoggingEvent
import io.policarp.logback.hec.skinnyhttp.SkinnyHecClient
import monix.execution.Scheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }
import skinny.http.{ HTTP, Request }

class SkinnyHecClientTest extends WordSpec with Matchers with ScalaFutures {

  val layout = new LayoutBase[ILoggingEvent] {
    override def doLayout(event: ILoggingEvent): String = event.getMessage
  }

  trait TestClient extends SkinnyHecClient {
    this.setSplunkUrl("https://somewhere.com")
    this.setToken("a-real-token")
  }

  class SomeClass()

  "This client" should {

    "properly prepare a request" in {

      val client = new TestClient {}

      val event = MockLoggingEvent("SomeClass", "test", Level.DEBUG)

      val request = client.prepareRequest.apply(Seq(event), layout).get
      request.url shouldBe "https://somewhere.com"
      request.header("Authorization") shouldBe Some("Splunk a-real-token")
      new String(request.bodyBytes.get, HTTP.DEFAULT_CHARSET) shouldBe "test"
    }

    "properly prepare a batch of requests" in {

      val client = new TestClient {}

      val events = (1 to 10).map(i => {
        MockLoggingEvent("SomeClass", "test", Level.DEBUG)
      })

      val request = client.prepareRequest.apply(events, layout).get
      request.url shouldBe "https://somewhere.com"
      request.header("Authorization") shouldBe Some("Splunk a-real-token")
      new String(request.bodyBytes.get, HTTP.DEFAULT_CHARSET) shouldBe (for (i <- 1 to 10) yield "test").mkString("\n\n")
    }

    "execute a request" in {

      var executed = false
      val client = new TestClient {
        override def executeRequest(request: Request) = {
          request.url shouldBe "https://somewhere.com"
          request.header("Authorization") shouldBe Some("Splunk a-real-token")
          executed = true
        }
      }

      val event = MockLoggingEvent("SomeClass", "test", Level.DEBUG)

      val future = client.postTask(Seq(event))(layout).runAsync(Scheduler.global)

      whenReady(future) { r =>
        executed shouldBe true
      }
    }
  }
}
