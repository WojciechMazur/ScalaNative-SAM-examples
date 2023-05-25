//> using target.platform "scala-native"
//> using nativeLinking "-lssl", "-lcrypto"
//> using nativeLinking "--target=x86_64-unknown-linux-gnu", "-static-libstdc++", "-L/usr/lib64/"
//> using dep "com.armanbilge::epollcat::0.1.4"
//> using dep "org.typelevel::cats-effect::3.5.0"


// Requires: smithy4s generate --dependencies com.disneystreaming.smithy:aws-dynamodb-spec:2023.02.10 -o ./handlers/wildrides

package wildrides

import runtime.*

import smithy4s.aws.*
import smithy4s.aws.http4s.AwsHttp4sBackend
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.dynamodb.*

import upickle.default.*
import upickle.implicits.key

import java.time.LocalDateTime
import scala.jdk.CollectionConverters.*
import scala.util.Try

import cats.effect.*
import cats.effect.unsafe.IORuntime
import epollcat.unsafe.EpollRuntime
import org.http4s.ember.client.EmberClientBuilder
import scribe.*

given IORuntime = EpollRuntime.global

case class Unicorn(
    name: String,
    color: String,
    gender: String
) derives Writer
case class Input(pickupLocation: PickupLocation) derives Reader
case class PickupLocation(
    latitude: Double,
    longitude: Double
) derives Reader

enum Response derives Writer:
  case Failure(
      error: String,
      reference: String
  )
  case Success(
      rideId: String,
      unicorn: Unicorn,
      unicornName: String,
      eta: String,
      rider: String
  )
  lazy val asEvent =
    APIGatewayProxyResponseEvent(
      statusCode = this match
        case _: Success => 201
        case _: Failure => 500
      ,
      headers = Map(
        "Content-Type" -> "application/json",
        "Access-Control-Allow-Origin" -> "*"
      ),
      body = write(this)
    )

case class RequestContext(authorizer: Authorizer) derives Reader
case class Authorizer(claims: Map[String, String]) derives Reader
case class APIGatewayProxyRequestEvent(
    requestContext: RequestContext,
    body: String
) derives Reader {
  val input: Input = read(body)
}
case class APIGatewayProxyResponseEvent(
    headers: Map[String, String],
    statusCode: Int,
    body: String
) derives Writer


@main def RequestUnicorn =
  new LambdaHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent]:
    override def run(
        event: APIGatewayProxyRequestEvent
    )(using Context): APIGatewayProxyResponseEvent = {
      println(event)
      val authorizer = event.requestContext.authorizer
      if authorizer == null
      then
        return Response
          .Failure("Authorization not configured", context.getAwsRequestId())
          .asEvent

      val rideId =
        scala.util.Random.alphanumeric
          .take(16)
          .mkString // UUID.randomUUID().toString()
      val username = authorizer.claims("cognito:username")
      scribe.info(s"Received event: rideId=$rideId, event=$event")

      val Input(pickupLocation) = event.input
      (for
        unicorn <- findUnicorn(event.input.pickupLocation)
        _ <- recordRide(rideId, username, unicorn)
      yield Response.Success(
        rideId = rideId,
        unicorn = unicorn,
        unicornName = unicorn.name,
        eta = "30 seconds",
        rider = username
      ))
        .recover { err =>
          scribe.error(err)
          Response.Failure(err.getMessage(), context.getAwsRequestId())
        }
        .get
        .asEvent
    }

lazy val fleet = Seq(
  Unicorn("Bucephalus", "Golden", "Male"),
  Unicorn("Shadowfax", "White", "Male"),
  Unicorn("Rocinante", "Yellow", "Female")
)

def findUnicorn(pickupLocation: PickupLocation): Try[Unicorn] = Try:
  scribe.info(s"Finding unicorn for ${pickupLocation.latitude},${pickupLocation.longitude}")
  scala.util.Random.shuffle(fleet).head

import smithy4s.aws.http4s.ServiceOps
val dynamoDbClient = for {
  logger <- Resource.make(config.loggerFactory.fromName("client-dynamoDb"))(_ => IO.unit)
  httpClient <- EmberClientBuilder.default[IO].withLogger(logger).build
  dynamodb <- DynamoDB.simpleAwsClient(httpClient, AwsRegion.EU_CENTRAL_1)
} yield dynamodb

implicit class NativeIOOps[T](val io: IO[T]) extends AnyVal {
  def runSync()(implicit runtime: IORuntime): T = {
    val promise = scala.concurrent.Promise[T]
    io.unsafeRunAsync(v => promise.complete(v.toTry))
    while(!promise.isCompleted) scala.scalanative.runtime.loop()
    promise.future.value.get.get
  }
}

def recordRide(rideId: String, username: String, unicorn: Unicorn): Try[Unit] = Try:
  dynamoDbClient
    .use { dynamoDb =>
      dynamoDb
        .putItem(
          tableName = TableName("Rides"),
          item = Map(
            "RideId" -> rideId,
            "User" -> username,
            "Unicorn" -> write(unicorn),
            "UnicornName" -> unicorn.name,
            "RequestTime" -> LocalDateTime.now().toString()
          ).map: (k, v) =>
            AttributeName(k) -> AttributeValue.SCase(StringAttributeValue(v))
        ).flatMap(res => IO.println(s"Recorded ride with id=$rideId, res=$res"))
    }
    .runSync()
