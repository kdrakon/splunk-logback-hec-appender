package io.policarp.splunk.logback

import org.slf4j.LoggerFactory

object Test extends App {

  new Thread {
    override def run = {
      try {
        class Blah {
          val n = 0
          val x = 123 / n
        }
        new Blah()
      } catch {
        case e: Throwable => LoggerFactory.getLogger("test").error("IN THREAD 2", e)
      }
    }
  }.start()

  Thread.sleep(5000)
}