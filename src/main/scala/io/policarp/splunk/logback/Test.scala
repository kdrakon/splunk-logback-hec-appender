package io.policarp.splunk.logback

import org.slf4j.LoggerFactory

object Test extends App {

  val logger = LoggerFactory.getLogger("test")

  logger.info("started")
  logger.info("1")
  logger.info("2")
  logger.info("3")
  logger.info("4")

  new Thread {
    override def run = logger.warn("IN THREAD", new NoSuchElementException("fdsfsfsf"))
  }.start()

  new Thread {
    override def run = {
      try {
        class Blah {
          val n = 0
          val x = 123 / n
        }
        new Blah()
      } catch {
        case e => LoggerFactory.getLogger("test2").error("IN THREAD 2", e)
      }
    }
  }.start()

  Thread.sleep(5000)
}