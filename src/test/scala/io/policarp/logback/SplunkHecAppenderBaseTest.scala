package io.policarp.logback

import ch.qos.logback.classic.Level
import io.policarp.logback.hec.FakeHecClient
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Minute, Seconds, Span }
import org.scalatest.{ Matchers, WordSpec }
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class SplunkHecAppenderTestWithBlockingStrategy extends SplunkHecAppenderBaseTest {
  override val strategy = BlockingSplunkHecAppenderStrategy()
}

class SplunkHecAppenderTestWithAsyncStrategy extends SplunkHecAppenderBaseTest {
  override val strategy = AsyncSplunkHecAppenderStrategy()
}

trait SplunkHecAppenderBaseTest extends WordSpec with Matchers with ScalaFutures {

  implicit val scheduler = Scheduler(scala.concurrent.ExecutionContext.global)

  val strategy: SplunkHecAppenderStrategy

  "The appender" should {

    "process a single event" in {

      val appender = new SplunkHecAppenderBase with FakeHecClient {
        this.appenderStrategy = strategy
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
        this.appenderStrategy = strategy
        this.flush = 1
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

      whenReady(f, Timeout(Span(1, Minute))) { r =>
        appender.fakeRequest.executions.get() shouldBe executions
      }
    }
  }
}

class SplunkHecAppenderTestWithSpillingStrategy extends WordSpec with Matchers with ScalaFutures {

  val logger = LoggerFactory.getLogger(classOf[SplunkHecAppenderTestWithSpillingStrategy])
  implicit val scheduler = Scheduler(scala.concurrent.ExecutionContext.global)

  "The appender" should {
    "eventually log most events that get pushed to it" in {

      val minExecutions = Integer.MAX_VALUE / 1000

      val appender = new SplunkHecAppenderBase with FakeHecClient {
        this.appenderStrategy = SpillingSplunkHecAppenderStrategy()
        this.flush = 5
        this.buffer = 1000
        this.parallelism = 8
      }

      appender.start()

      Task {
        while (true) {
          val event = MockLoggingEvent("SomeClass", "Normal stuff happening", Level.INFO)
          appender.append(event)
        }
      }.runAsync

      val f = Future {
        while (appender.fakeRequest.executions.get() < minExecutions) {
          // wait
          Thread.sleep(50)
        }
      }

      whenReady(f, Timeout(Span(1, Minute))) { r =>
        logger.info(s"Logged ${appender.fakeRequest.executions.get()} messages")
        assert(appender.fakeRequest.executions.get() >= minExecutions)
      }

    }
  }
}
