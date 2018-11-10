package com.banno.twittersampleendpoint

import cats.effect._
import cats.implicits._
import com.banno.twittersampleendpoint.domain._
import com.banno.twittersampleendpoint.service.StreamAnalyticsService
import fs2.StreamApp.ExitCode
import fs2.async.signalOf
import fs2.io.stdoutAsync
import fs2.{Stream, StreamApp}
import io.circe.Json
import jawnfs2._
import org.http4s.client.blaze._
import org.http4s.client.oauth1
import org.http4s.rho.swagger.SwaggerSupport
import org.http4s.rho.swagger.models.Info
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.{BuildInfo => _, _}
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._


import scala.concurrent.ExecutionContext.Implicits.global

object TwitterStream extends TwitterStreamApp[IO]

abstract class TwitterStreamApp[F[_] : Effect] extends StreamApp[F] {

  import fs2.Pipe
  import fs2.text.utf8Encode

  // jawn-fs2 needs to know what JSON AST you want
  implicit val f = io.circe.jawn.CirceSupportParser.facade

  private[this] def appConfig(implicit F: Effect[F]): Stream[F, AppConfig] = {
    Stream.eval(
      F.flatMap[Either[ConfigReaderFailures, AppConfig], AppConfig](F.delay(pureconfig.loadConfig[AppConfig])) {
        case Left(errors) =>
          F.raiseError(new Throwable(errors.toList.map(_.description).toString()))
        case Right(config: AppConfig) => F.pure(config)
      })
  }

  private[this] def server(serverConfig: ServerConfig, twitterSampleStreamRoute: HttpService[F]): Stream[F, ExitCode] = {
    BlazeBuilder[F]
      .bindHttp(port = serverConfig.port, host = serverConfig.ip)
      .mountService(twitterSampleStreamRoute, "/")
      .serve
  }

  /* These values are created by a Twitter developer web app.
   * OAuth signing is an effect due to generating a nonce for each `Request`.
   */
  private[this] def signIn(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
                          (req: Request[F]): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  /* Create a http client, sign the incoming `Request[F]`, stream the `Response[F]`, and
   * `parseJsonStream` the `Response[F]`.
   * `sign` returns a `F`, so we need to `Stream.eval` it to use a for-comprehension.
   */
  private[this] def twitterJsonStream(twitterCredentials: TwitterCredentials): Stream[F, Json] = {
    val req = Request[F](Method.GET, Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))
    for {
      client <- Http1Client.stream[F]()
      sr <- Stream.eval(signIn(consumerKey = twitterCredentials.consumerKey,
        consumerSecret = twitterCredentials.consumerSecret,
        accessToken = twitterCredentials.userKey,
        accessSecret = twitterCredentials.userSecret)(req))
      res <- client.streaming(sr) {
        resp =>
          resp.body.chunks.parseJsonStream
      }
    } yield res
  }

  private[this] def toStdOut: Pipe[F, SampleTweetStreamAnalytics, Unit] = {
    _.map(_.toString)
      .through(utf8Encode)
      .through(stdoutAsync)
  }

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    for {
      config <- appConfig
      signal <- Stream.eval(signalOf(SampleTweetStreamAnalytics.empty))
      swaggerMiddleware = SwaggerSupport[F].createRhoMiddleware(apiInfo = Info(
        title = SbtBuildInfo.name,
        description = Some(SbtBuildInfo.description),
        version = SbtBuildInfo.version
      ))
      staticContentRoute = new StaticContentRoute[F]().routes()
      twitterSampleStreamRoute = new TwitterSampleStreamRoute[F](signal).toService(swaggerMiddleware)
      streamAnalyticsService = new StreamAnalyticsService[F]
      httpService = twitterSampleStreamRoute <+> staticContentRoute
      serverStream = server(config.server, httpService)
      analyticsStream = twitterJsonStream(config.twitterCredentials)
        .through(streamAnalyticsService.sampleTweetStream)
        .through(streamAnalyticsService.processTweet(signal))
      _ <- serverStream merge (analyticsStream to toStdOut).drain
    } yield ExitCode.Success
  }
}
