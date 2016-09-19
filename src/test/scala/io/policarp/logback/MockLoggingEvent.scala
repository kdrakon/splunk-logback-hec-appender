package io.policarp.logback

import java.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.{ ILoggingEvent, IThrowableProxy, LoggerContextVO, StackTraceElementProxy }
import org.slf4j.Marker

import scala.collection.JavaConversions._

object MockLoggingEvent {
  def apply(loggerName: String, message: String, level: Level, stacktrace: Array[StackTraceElementProxy] = Array()): MockLoggingEvent =
    new MockLoggingEvent(loggerName, message, level, stacktrace)
}

class MockLoggingEvent(loggerName: String, message: String, level: Level, stacktrace: Array[StackTraceElementProxy]) extends ILoggingEvent {

  override def getLoggerName: String = loggerName

  override def getFormattedMessage: String = message

  override def getMessage: String = message

  override def getLoggerContextVO: LoggerContextVO = new LoggerContextVO("", Map[String, String](), 0)

  override def getLevel: Level = level

  override def getTimeStamp: Long = 0

  override def getCallerData: Array[StackTraceElement] = Array()

  override def hasCallerData: Boolean = true

  override def getMDCPropertyMap: util.Map[String, String] = Map[String, String]()

  override def getMdc: util.Map[String, String] = Map[String, String]()

  override def getThreadName: String = "fake-thread"

  override def getArgumentArray: Array[AnyRef] = Array()

  override def getMarker: Marker = null

  override def getThrowableProxy: IThrowableProxy = new IThrowableProxy {

    override def getMessage: String = message

    override def getCommonFrames: Int = 0

    override def getSuppressed: Array[IThrowableProxy] = Array()

    override def getStackTraceElementProxyArray: Array[StackTraceElementProxy] = stacktrace

    override def getClassName: String = ""

    override def getCause: IThrowableProxy = null
  }

  override def prepareForDeferredProcessing(): Unit = {}
}
