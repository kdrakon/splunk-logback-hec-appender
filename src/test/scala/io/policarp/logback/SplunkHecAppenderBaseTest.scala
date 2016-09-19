package io.policarp.logback

import ch.qos.logback.classic.Level
import io.policarp.logback.hec.FakeHecClient
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Future

class SplunkHecAppenderBaseTest extends WordSpec with Matchers with ScalaFutures {

  implicit val scheduler = Scheduler(scala.concurrent.ExecutionContext.global)

  "The appender" should {

    "process a single event" in {

      val appender = new SplunkHecAppenderBase with FakeHecClient {
        this.flush = 1
        this.buffer = 1
      }

      appender.start()
      val event = MockLoggingEvent("SomeClass", "Normal stuff happening", Level.INFO)
      appender.append(event)

      val f = Future {
        while (!appender.fakeRequest.executed) {
          // wait
          Thread.sleep(50)
        }
      }

      whenReady(f, Timeout(Span(5, Seconds))) { r =>
        appender.fakeRequest.lastEvent shouldBe Some(event)
        appender.fakeRequest.executions.get() shouldBe 1
      }
    }

    "process a lot of events" in {

      val executions = Integer.MAX_VALUE / 1000

      val appender = new SplunkHecAppenderBase with FakeHecClient {
        this.flush = 10
        this.buffer = 100
        this.parallelism = 8
      }

      appender.start()

      Task {
        for (i <- 1 to executions) {
          val event = MockLoggingEvent("SomeClass", "Normal stuff happening", Level.INFO)
          appender.append(event)
        }
      }.runAsync

      val f = Future {
        while (appender.fakeRequest.executions.get() < executions) {
          // wait
          Thread.sleep(50)
        }
      }

      whenReady(f, Timeout(Span(60, Seconds))) { r =>
        appender.fakeRequest.executions.get() shouldBe executions
      }

    }
  }

}
