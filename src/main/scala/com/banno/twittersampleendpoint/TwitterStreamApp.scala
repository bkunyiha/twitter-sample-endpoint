package com.banno.twittersampleendpoint

import cats.effect._
import cats.implicits._
import com.banno.twittersampleendpoint.domain._
import com.banno.twittersampleendpoint.service.{EmojiService, HashTagService, LinkService, SampleTweetsCountingService}
import fs2.StreamApp.ExitCode
import fs2.async.mutable.Signal
import fs2.async.signalOf
import fs2.io.stdout
import fs2.text.utf8Encode
import fs2.{Pipe, Stream, StreamApp}
import io.circe.{Json, Printer}
import jawnfs2._
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1
import org.http4s.server.blaze.BlazeBuilder
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext.Implicits.global

object TwitterStream$ extends TwitterStreamApp[IO]

abstract class TwitterStreamApp[F[_] : Effect] extends StreamApp[F] {

  // jawn-fs2 needs to know what JSON AST you want
  implicit val f = io.circe.jawn.CirceSupportParser.facade

  /* These values are created by a Twitter developer web app.
   * OAuth signing is an effect due to generating a nonce for each `Request`.
   */
  private[this] def sign(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
          (req: Request[F]): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  /* Create a http client, sign the incoming `Request[F]`, stream the `Response[F]`, and
   * `parseJsonStream` the `Response[F]`.
   * `sign` returns a `F`, so we need to `Stream.eval` it to use a for-comprehension.
   */
  private[this] def jsonStream(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
                (req: Request[F]): Stream[F, Json] =
    for {
      client <- Http1Client.stream[F]()
      sr <- Stream.eval(sign(consumerKey = consumerKey,
        consumerSecret = consumerSecret,
        accessToken = accessToken,
        accessSecret = accessSecret)(req))
      res <- client.streaming(sr) {
        resp =>
          resp.body.chunks.parseJsonStream
      }
    } yield res

  private[this] def twitterStream(twitterCredentials: TwitterCredentials): Stream[F, SampleTweet] = {
    val req = Request[F](Method.GET, Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))

    val s = jsonStream(consumerKey = twitterCredentials.consumerKey,
      consumerSecret = twitterCredentials.consumerSecret,
      accessToken = twitterCredentials.userKey,
      accessSecret = twitterCredentials.userSecret)(req)

    s.map { json =>
      json.as[SampleTweet].leftMap(pE => s"ParseError: ${pE.message} - ${json.pretty(Printer.noSpaces)}")
    }.through(filterLeft)
  }

  /*def stringToByteStream: Pipe[F, String, Byte] = {
    _.through(utf8Encode)
  }*/

  private[this] def analyticsToByteStream: Pipe[F, SampleTweetStreamAnalytics, Byte] = {
    _.map(_.toString).through(utf8Encode)
  }

  private[this] def sampleTweetStreamAnalytics(sampleTweet: SampleTweet): SampleTweetStreamAnalytics =
    SampleTweetStreamAnalytics(
      counter = SampleTweetsCountingService(),
      emoji = EmojiService.default(sampleTweet),
      hashTagStats = HashTagService.default(sampleTweet),
      linkStats = LinkService.default(sampleTweet)
    )

  private[this] def processTweetAnalysis: Pipe[F, SampleTweet, SampleTweetStreamAnalytics] = {
    jsonStream => jsonStream.map(sampleTweetStreamAnalytics)
  }

  private[this] def processTweet(signal: Signal[F, SampleTweetStreamAnalytics]): Pipe[F, SampleTweet, SampleTweetStreamAnalytics] = {
    _.through(processTweetAnalysis)
      .scan(SampleTweetStreamAnalytics.empty)(_ |+| _)
      .observe1(signal.set)
  }

  private[this] def server(serverConfig: ServerConfig, twitterSampleStreamRoute: HttpService[F]): Stream[F, ExitCode] =
    BlazeBuilder[F]
      .bindHttp(port = serverConfig.port, host = serverConfig.ip)
      .mountService(twitterSampleStreamRoute, "/")
      .serve

  private[this] def appConfig(implicit F: Effect[F]): Stream[F, AppConfig] = {
    Stream.eval(
      F.flatMap[Either[ConfigReaderFailures, AppConfig], AppConfig](F.delay(pureconfig.loadConfig[AppConfig])) {
        case Left(errors) =>
          F.raiseError(new Throwable(errors.toList.map(_.description).toString()))
        case Right(config: AppConfig) => F.pure(config)
      })
  }

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    for {
      config <- appConfig
      signal <- Stream.eval(signalOf(SampleTweetStreamAnalytics.empty))
      twitterSampleStreamRoute = new TwitterSampleStreamRoute[F](signal).service
      analyticsStream = twitterStream(config.twitterCredentials).through(processTweet(signal))
      analyticsResult = analyticsStream.through(analyticsToByteStream).through(stdout)
      serverStream = server(config.server, twitterSampleStreamRoute)
      _ <- serverStream merge (analyticsStream.drain) merge (analyticsResult.drain)
    } yield ExitCode.Success
  }
}
