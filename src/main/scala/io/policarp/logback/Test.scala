package io.policarp.logback

import org.slf4j.LoggerFactory

object Test extends App {

  for (i <- 1 to 19) {
    try {
      class Blah {
        val n = 0
        val x = 123 / n
      }
      new Blah()
    } catch {
      case e: Throwable => LoggerFactory.getLogger("test").error("IN THREAD", e)
    }
  }

  Thread.sleep(60000)
}