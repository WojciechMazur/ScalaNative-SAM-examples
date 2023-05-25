//> using dep "com.outr::scribe-cats::3.11.3"

package config

import org.typelevel.log4cats.*
import cats.effect.*

def loggerFactory: LoggerFactory[IO] = ScribeLoggerFactory

// scalafmt: { maxColumn = 120}
private class ScribeLogger(logger: scribe.Logger) extends SelfAwareStructuredLogger[IO] {
  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] = IO.delay(logger.info(msg))
  override def info(ctx: Map[String, String])(msg: => String): IO[Unit] = IO.delay(logger.info(msg))
  override def info(t: Throwable)(message: => String): IO[Unit] = IO.delay(logger.info(message))
  override def info(message: => String): IO[Unit] = IO.delay(logger.info(message))
  override def isInfoEnabled: IO[Boolean] = IO.delay(logger.includes(scribe.Level.Info))

  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] = IO.delay(logger.warn(msg))
  override def warn(ctx: Map[String, String])(msg: => String): IO[Unit] = IO.delay(logger.warn(msg))
  override def warn(t: Throwable)(message: => String): IO[Unit] = IO.delay(logger.warn(message))
  override def warn(message: => String): IO[Unit] = IO.delay(logger.warn(message))
  override def isWarnEnabled: IO[Boolean] = IO.delay(logger.includes(scribe.Level.Warn))

  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] = IO.delay(logger.trace(msg))
  override def trace(ctx: Map[String, String])(msg: => String): IO[Unit] = IO.delay(logger.trace(msg))
  override def trace(t: Throwable)(message: => String): IO[Unit] = IO.delay(logger.trace(message))
  override def trace(message: => String): IO[Unit] = IO.delay(logger.trace(message))
  override def isTraceEnabled: IO[Boolean] = IO.delay(logger.includes(scribe.Level.Trace))

  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] = IO.delay(logger.error(msg))
  override def error(ctx: Map[String, String])(msg: => String): IO[Unit] = IO.delay(logger.error(msg))
  override def error(t: Throwable)(message: => String): IO[Unit] = IO.delay(logger.error(message))
  override def error(message: => String): IO[Unit] = IO.delay(logger.error(message))
  override def isErrorEnabled: IO[Boolean] = IO.delay(logger.includes(scribe.Level.Error))

  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): IO[Unit] = IO.delay(logger.debug(msg))
  override def debug(ctx: Map[String, String])(msg: => String): IO[Unit] = IO.delay(logger.debug(msg))
  override def debug(t: Throwable)(message: => String): IO[Unit] = IO.delay(logger.debug(message))
  override def debug(message: => String): IO[Unit] = IO.delay(logger.debug(message))
  override def isDebugEnabled: IO[Boolean] = IO.delay(logger.includes(scribe.Level.Debug))
}

private object ScribeLoggerFactory extends LoggerFactory[IO] {
  import scribe.cats._

  override def fromName(name: String): IO[SelfAwareStructuredLogger[cats.effect.IO]] =
    IO.delay(getLoggerFromName(name))

  override def getLoggerFromName(name: String): LoggerType = ScribeLogger(
    scribe.Logger(name)
  )
}
