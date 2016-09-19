package io.policarp.logback.hec

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import io.policarp.logback.MockLoggingEvent
import org.scalatest.{ Matchers, WordSpec }

class SplunkHecClientTest extends WordSpec with Matchers {

  val layout = new LayoutBase[ILoggingEvent] {
    override def doLayout(event: ILoggingEvent): String = event.getMessage
  }

  "The Object" should {
    "prepare layouts in a line separated format" in {

      import SplunkHecClient.formatJsonEvents

      formatJsonEvents(Seq(), layout) shouldBe None

      val event1 = MockLoggingEvent("SomeClass", "test 1", Level.DEBUG)
      val event2 = MockLoggingEvent("SomeClass", "test 2", Level.DEBUG)
      formatJsonEvents(Seq(event1, event2), layout) shouldBe Some("test 1\n\ntest 2")
      formatJsonEvents(Seq(event1), layout) shouldBe Some("test 1")
      formatJsonEvents(Seq(event2), layout) shouldBe Some("test 2")
    }
  }

}
