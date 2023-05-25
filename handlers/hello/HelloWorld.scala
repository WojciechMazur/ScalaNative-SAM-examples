package hello

import runtime.*
import upickle.default.*

case class Event(text: String) derives Reader
case class Result(status: String) derives Writer

@main def HelloWorld = LambdaHandler: 
  (event: Event) =>
    val msg = s"GOT REQUEST ${context.getAwsRequestId()} with event data: ${event.text}"
    Result(msg)

