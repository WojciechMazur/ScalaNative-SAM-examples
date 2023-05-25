//> using target.platform "jvm"

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.Context
import java.io.{InputStream, OutputStream}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.serverless.aws.lambda.* 
import io.circe.generic.auto.*

// JVM handler
class TapirExampleHandler extends LambdaHandler[IO, AwsRequest]{
  override protected def getAllEndpoints: List[ServerEndpoint[Any, IO]] = TapirExample.endpoints
  
  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = 
    process(input, output).unsafeRunSync()
}