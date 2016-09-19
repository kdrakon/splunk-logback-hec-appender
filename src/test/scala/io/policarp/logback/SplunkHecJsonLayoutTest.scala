package io.policarp.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.StackTraceElementProxy
import org.scalatest.{ Matchers, WordSpec }

class SplunkHecJsonLayoutTest extends WordSpec with Matchers {

  "SplunkHecJsonLayout.parseStackTrace" should {

    import SplunkHecJsonLayout.parseStackTrace

    "parse stacktraces" in {

      val stacktrace = (1 to 100).map(i => {
        new StackTraceElementProxy(new StackTraceElement("errorClass", "someMethod", "someFile", i))
      }).toArray

      val event = MockLoggingEvent("SomeClass", "failure", Level.ERROR, stacktrace)

      parseStackTrace(event, 500) match {
        case Some(head :: tail) =>
          1 + tail.size shouldBe 100
          (head :: tail).forall(s => s.startsWith("errorClass.someMethod(someFile:")) shouldBe true
          head shouldBe "errorClass.someMethod(someFile:1)"
          tail.reverse.head shouldBe "errorClass.someMethod(someFile:100)"
        case None | Some(Nil) => fail("Should not be empty or None")
      }

      parseStackTrace(event, 50) match {
        case Some(head :: tail) =>
          1 + tail.size shouldBe 51
          (head :: tail).reverse.tail.forall(s => s.startsWith("errorClass.someMethod(someFile:")) shouldBe true
          head shouldBe "errorClass.someMethod(someFile:1)"
          tail.reverse.head shouldBe "..."
        case None | Some(Nil) => fail("Should not be empty or None")
      }

      val emptyEvent = MockLoggingEvent("SomeClass", "failure", Level.ERROR)

      parseStackTrace(emptyEvent, 25) match {
        case Some(Nil) => assert(true, "Should be Nil")
        case Some(_) => fail("Should be Nil")
        case None => fail("Should not be None")
      }
    }
  }

  "SplunkHecJsonLayout" should {

    "layout events" in {

      val layout = new SplunkHecJsonLayout
      layout.setMaxStackTrace(15)

      val stacktrace = (1 to 20).map(i => {
        new StackTraceElementProxy(new StackTraceElement("errorClass", "someMethod", "someFile", i))
      }).toArray

      val event = MockLoggingEvent("SomeClass", "failure", Level.ERROR, stacktrace)

      layout.doLayout(event) shouldBe {
        """{"time":0,"event":{"message":"failure","level":"ERROR","thread":"fake-thread","logger":"SomeClass","exception":": failure\n","stacktrace":["errorClass.someMethod(someFile:1)","errorClass.someMethod(someFile:2)","errorClass.someMethod(someFile:3)","errorClass.someMethod(someFile:4)","errorClass.someMethod(someFile:5)","errorClass.someMethod(someFile:6)","errorClass.someMethod(someFile:7)","errorClass.someMethod(someFile:8)","errorClass.someMethod(someFile:9)","errorClass.someMethod(someFile:10)","errorClass.someMethod(someFile:11)","errorClass.someMethod(someFile:12)","errorClass.someMethod(someFile:13)","errorClass.someMethod(someFile:14)","errorClass.someMethod(someFile:15)","..."]}}"""
      }
    }

    "format set bean properties" in {

      val layout = new SplunkHecJsonLayout
      layout.setMaxStackTrace(5)
      layout.setCustom("custom1=val1")
      layout.setCustom("custom2=val2")
      layout.setHost("test-host")
      layout.setIndex("test-index")
      layout.setSource("test-source")
      layout.setSourcetype("test-sourcetype")

      val stacktrace = (1 to 1000).map(i => {
        new StackTraceElementProxy(new StackTraceElement("errorClass", "someMethod", "someFile", i))
      }).toArray

      val event = MockLoggingEvent("SomeClass", "failure", Level.ERROR, stacktrace)

      layout.doLayout(event) shouldBe {
        """{"time":0,"event":{"message":"failure","level":"ERROR","thread":"fake-thread","logger":"SomeClass","exception":": failure\n","stacktrace":["errorClass.someMethod(someFile:1)","errorClass.someMethod(someFile:2)","errorClass.someMethod(someFile:3)","errorClass.someMethod(someFile:4)","errorClass.someMethod(someFile:5)","..."],"customFields":{"custom2":"val2","custom1":"val1"}},"host":"test-host","source":"test-source","sourcetype":"test-sourcetype","index":"test-index"}"""
      }
    }
  }

}
