package io.policarp.logback

import org.slf4j.LoggerFactory

object TestRunner extends App {

  val logger = LoggerFactory.getLogger("test")

  for (i <- 1 to 1023) {
    try {
      class Blah {
        val n = 0
        val x = 123 / n
      }
      new Blah()
    } catch {
      case e: Throwable => logger.error("IN THREAD", e)
    }
  }

  Thread.sleep(5000)

  logger.info("done sleep")
  logger.info("...going for longer sleep")

  Thread.sleep(60000)
}