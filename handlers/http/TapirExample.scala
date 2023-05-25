import cats.effect.IO
import cats.syntax.all.*
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.serverless.aws.lambda.* 
import sttp.tapir.serverless.aws.lambda.runtime.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*

case class Response(msg: String)

object TapirExample extends AwsLambdaIORuntime {
  val helloEndpoint: ServerEndpoint[Any, IO] = endpoint.get
    .in("api" / "hello" / paths)
    .errorOut(stringBody)
    .out(jsonBody[Response])
    .serverLogic { args =>
      val name = args.headOption
      IO.pure:
        Response:
          s"Hello ${name.getOrElse("anonymous")}. Welcome to Serverless Lambda!"
        .asRight[String]
    }

  val wildcardEndpoint: ServerEndpoint[Any, IO] = endpoint.get.in(paths).out(stringBody).serverLogic: 
    input =>
      val msg = s"Unknown endpoint: ${input.mkString("/")}"
      IO.pure(msg.asRight[Unit])  
    
  override val endpoints: List[ServerEndpoint[Any, IO]] = List(helloEndpoint, wildcardEndpoint)
  override val serverOptions: AwsServerOptions[IO] = AwsCatsEffectServerOptions.noEncoding[IO]
}

