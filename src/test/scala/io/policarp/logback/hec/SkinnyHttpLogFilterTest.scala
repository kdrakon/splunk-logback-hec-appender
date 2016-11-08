package io.policarp.logback.hec

import ch.qos.logback.classic.{ Level, Logger }
import ch.qos.logback.core.spi.FilterReply._
import io.policarp.logback.MockLoggingEvent
import io.policarp.logback.hec.skinnyhttp.SkinnyHttpLogFilter
import org.scalatest.{ Matchers, WordSpec }
import org.slf4j.LoggerFactory
import skinny.http.HTTP

class SkinnyHttpLogFilterTest extends WordSpec with Matchers {

  val filter = new SkinnyHttpLogFilter()

  "The filter" should {
    "filter out logging events for Skinny" in {
      val event = MockLoggingEvent(classOf[HTTP].getName, "test 1", Level.DEBUG)
      filter.decide(event) shouldBe DENY
    }

    "be neutral on logging events for anything else" in {
      val event = MockLoggingEvent("SomeClass", "test 2", Level.DEBUG)
      filter.decide(event) shouldBe NEUTRAL
    }
  }

}
