//> using target.platform "scala-native"

package runtime

import com.amazonaws.services.lambda.runtime.*

import sttp.client4.*
import sttp.model.{StatusCode, Uri}
import util.Try
import upickle.default._
import ujson.{read => parse}

transparent inline def context(using ctx: Context): Context = ctx

// trait AWSLambda[I: Reader, O: Writer] extends RequestHandler[String, String]:
//   def apply(event: I)(using Context): O
//   final def handleRequest(input: String, context: Context): String =
//     write {
//       apply(read(input))(using context)
//     }

trait LambdaHandler[I: Reader, O: Writer]:
  def run(event: I)(using Context): O
  private val result = LambdaHandler.run(this)

object LambdaHandler {
  inline def apply[I: Reader, O: Writer](inline handler: I => Context ?=> O) =
    new LambdaHandler[I, O]:
      override def run(event: I)(using Context): O = handler(event)

  private final val RuntimeApi = sys.env
    .get("AWS_LAMBDA_RUNTIME_API")
    .getOrElse {
      throw FatalError(
        new RuntimeException("AWS_LAMBDA_RUNTIME_API env not set")
      )
    }
  private val endpoint = uri"http://$RuntimeApi/2018-06-01"
  private val backend = wrappers.TryBackend(DefaultSyncBackend())

  def run[I: Reader, O: Writer](handler: LambdaHandler[I, O]) = while (true) {
    val result: Either[LambdaException, (StatusCode, String)] = for {
      request <- basicRequest
        .get(uri"$endpoint/runtime/invocation/next")
        .send(backend)
        .toEither
        .left
        .map(FatalError(_))

      context = NativeContext(
        request.headers
          .map(h => h.name -> h.value)
          .toMap
      )
      requestId = context.getAwsRequestId()

      eventData <- request.body.left.map(InvalidEvent(requestId, _))

      parsed <- Try(read[I](eventData)).toEither.left
        .map(_ => ParsingException(requestId, eventData))

      responseUrl =
        uri"$endpoint/runtime/invocation/$requestId/response"

      response <- Try(handler.run(parsed)(using context)).toEither.left
        .map(FunctionExecutionError(requestId, _))

      res <- basicRequest
        .post(responseUrl)
        .body(write(response))
        .send(backend)
        .toEither
        .left
        .map(ResponseException(requestId, _))

    } yield (res.code, requestId)

    result.fold(reportFailure, (logSuccess _).tupled)
  }

  private class NativeContext(headers: Map[String, String]) extends Context:
    def getAwsRequestId(): String = headers("Lambda-Runtime-Aws-Request-Id")
    def getClientContext(): ClientContext = ???
    def getFunctionName(): String = ???
    def getFunctionVersion(): String = ???
    def getIdentity(): CognitoIdentity = ???
    def getInvokedFunctionArn(): String = ???
    def getLogGroupName(): String = ???
    def getLogStreamName(): String = ???
    def getLogger(): LambdaLogger = ???
    def getMemoryLimitInMB(): Int = ???
    def getRemainingTimeInMillis(): Int = ???

  private def reportFailure(ex: LambdaException): Unit = ex match {
    case request: FailedRequest =>
      scribe.error(
        s"Failed to execute request ${request.awsRequestId}: ${request.message}",
        request
      )
      basicRequest
        .post(
          uri"$endpoint/runtime/invocation/${request.awsRequestId}/error"
        )
        .body(write(new ErrorMessage(request), indent = 2))
        .send(backend)

    case fatalError: FatalError =>
      scribe.error(fatalError)
      sys.exit(1)
  }

  private def logSuccess(code: StatusCode, requestId: String): Unit =
    code match {
      case _ if code.isSuccess => ()
      case _ => scribe.error(s"Request $requestId finished with code $code")
    }

  case class ErrorMessage(errorMessage: String, errorType: String)
      derives Writer:
    def this(failedRequest: FailedRequest) =
      this(failedRequest.message, failedRequest.getClass().getName())

  sealed abstract class LambdaException(msg: String, cause: Throwable)
      extends RuntimeException(msg, cause)
  private case class FatalError(error: Throwable)
      extends LambdaException(
        s"Fatal error occurred during function invocation",
        error
      )

  sealed abstract class FailedRequest(
      val awsRequestId: String,
      val message: String,
      val cause: Throwable
  ) extends LambdaException(message, cause)
  private case class InvalidEvent(requestId: String, msg: String)
      extends FailedRequest(requestId, s"Invalid request data: $msg", null)
  private case class ParsingException(requestId: String, msg: String)
      extends FailedRequest(requestId, s"Cannot parse event: $msg", null)
  private case class FunctionExecutionError(
      requestId: String,
      wrapped: Throwable
  ) extends FailedRequest(
        requestId,
        s"Failed to execute function: ${wrapped.getMessage}",
        wrapped
      )
  private case class ResponseException(requestId: String, wrapped: Throwable)
      extends FailedRequest(
        requestId,
        s"Failed to report response: ${wrapped.getMessage}",
        wrapped
      )

}
