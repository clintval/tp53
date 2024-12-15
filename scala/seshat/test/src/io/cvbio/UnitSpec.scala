package io.cvbio

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest._
import org.slf4j.Logger.{ROOT_LOGGER_NAME => RootLoggerName}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.CollectionHasAsScala

/** A trait for creating a single logger for all tests which clears the loggers state in between test executions. */
trait LoggerPerTest extends BeforeAndAfterEach with BeforeAndAfterAll { self: Suite =>
  lazy val appender: ListAppender[ILoggingEvent] = new ListAppender()
  lazy val logger: Logger = LoggerFactory.getLogger(RootLoggerName).asInstanceOf[Logger]

  /** Before we run all tests, silence the internal verbosity of the logging system. */
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val _ = System.setProperty("slf4j.internal.verbosity", "WARN")
  }

  /** Before each test, add a new empty event appender to the logger. */
  override def beforeEach(): Unit = {
    logger.addAppender(appender)
    appender.start()
    super.beforeEach()
  }

  /** After each test, stop the appender, clear it, and detach it. */
  override def afterEach(): Unit = {
    try super.afterEach()
    finally {
      appender.stop()
      appender.list.clear()
      logger.detachAndStopAllAppenders()
    }
  }

  /** Get all formatted log messages as strings. */
  def logs: Seq[String] = appender.list.asScala.toSeq.map(_.getFormattedMessage)
}

/** Base class for unit testing. */
trait UnitSpec extends AnyFlatSpec with Matchers with OptionValues with TryValues with LoggerPerTest
