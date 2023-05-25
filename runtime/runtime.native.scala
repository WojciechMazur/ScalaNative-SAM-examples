//> using target.platform "scala-native"

package com.amazonaws.services.lambda.runtime

import java.io.{InputStream, OutputStream}
import java.util
import java.io.IOException

trait RequestHandler[I, O] {
  def handleRequest(input: I, context: Context): O
}

trait RequestStreamHandler {
  def handleRequest(
      input: InputStream,
      output: OutputStream,
      context: Context
  ): Unit
}

trait Context {
  def getAwsRequestId(): String
  def getLogGroupName(): String
  def getLogStreamName(): String
  def getFunctionName(): String
  def getFunctionVersion(): String
  def getInvokedFunctionArn(): String
  def getIdentity(): CognitoIdentity
  def getClientContext(): ClientContext
  def getRemainingTimeInMillis(): Int
  def getMemoryLimitInMB(): Int
  def getLogger(): LambdaLogger
}

trait ClientContext {
  def getClient(): Client
  def getCustom(): util.Map[String, String]
  def getEnvironment(): util.Map[String, String]
}

trait Client {
  def getInstallationId(): String
  def getAppTitle(): String
  def getAppVersionName(): String
  def getAppVersionCode(): String
  def getAppPackageName(): String
}

trait CognitoIdentity {
  def getIdentityId(): String
  def getIdentityPoolId(): String
}

trait LambdaLogger {
  def log(message: String): Unit
  def log(message: Array[Byte]): Unit
}

object LambdaRuntime {
  private object logger extends LambdaLogger() {
    override def log(message: String): Unit = System.out.print(message)
    override def log(message: Array[Byte]): Unit =
      try System.out.write(message)
      catch { case e: IOException => e.printStackTrace() }
  }
  def getLogger(): LambdaLogger = logger
}

object LambdaRuntimeInternal {
  def setUseLog4jAppender(useLog4j: Boolean): Unit = ()
  def getUseLog4jAppender(): Boolean = false
}