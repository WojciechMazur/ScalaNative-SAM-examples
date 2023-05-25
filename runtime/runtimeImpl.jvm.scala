//> using target.platform "jvm"
//> using dep "com.amazonaws:aws-lambda-java-core:1.2.2"

package runtime

import com.amazonaws.services.lambda.runtime.*
import java.io.{InputStream, OutputStream}

import upickle.default._
import java.nio.charset.StandardCharsets

transparent inline def context(using ctx: Context): Context = ctx

trait LambdaHandler[I: Reader, O: Writer] extends RequestStreamHandler:
  def run(event: I)(using Context): O

  override def handleRequest(
      input: InputStream,
      output: OutputStream,
      context: Context
  ): Unit =
    val event = read[I](input)
    val result = run(event)(using context)
    output.write(write(result).getBytes(StandardCharsets.UTF_8))

object LambdaHandler:
  inline def apply[I: Reader, O: Writer](inline handler: I => Context ?=> O) =
    new LambdaHandler[I, O]:
      override def run(event: I)(using Context): O = handler(event)
