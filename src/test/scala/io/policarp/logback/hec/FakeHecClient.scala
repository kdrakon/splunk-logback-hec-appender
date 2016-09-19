package io.policarp.logback.hec

import java.util.concurrent.atomic.AtomicInteger

import ch.qos.logback.classic.spi.ILoggingEvent

class FakeRequest(
    var executed: Boolean = false,
    val executions: AtomicInteger = new AtomicInteger(0),
    var lastEvent: Option[ILoggingEvent] = None
) {
  var nextIncrement: () => Unit = _
}

trait FakeHecClient extends SplunkHecClient {

  override type AbstractRequest = FakeRequest

  var fakeRequest = new FakeRequest()

  override private[hec] def prepareRequest = (events, layout) => {
    if (events.nonEmpty) {
      fakeRequest.lastEvent = Some(events.reverse.head)
      fakeRequest.nextIncrement = () => fakeRequest.executions.addAndGet(events.size)
    }
    Some(fakeRequest)
  }

  override private[hec] def executeRequest(preparedRequest: FakeRequest) = {
    preparedRequest.executed = true
    preparedRequest.nextIncrement()
  }
}
