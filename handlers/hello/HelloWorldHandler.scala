//> using target.platform "jvm"

package hello

import runtime.*
import com.amazonaws.services.lambda.runtime.Context

class HelloWorldHandler extends LambdaHandler[Event, Result]:
  override def run(event: Event)(using Context): Result = 
    val msg = s"GOT REQUEST ${context.getAwsRequestId()} with event data: ${event.text}"
    Result(msg)