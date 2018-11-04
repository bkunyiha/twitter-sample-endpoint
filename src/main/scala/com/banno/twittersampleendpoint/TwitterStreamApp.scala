package com.banno.twittersampleendpoint

import cats.effect._
import cats.implicits._
import com.banno.twittersampleendpoint.domain.SampleTweetStreamAlytics
import fs2.StreamApp.ExitCode
import fs2.async.mutable.Signal
import fs2.async.signalOf
import fs2.io.stdout
import fs2.text.{utf8Encode}
import fs2.{Stream, StreamApp}
import io.circe.Json
import jawnfs2._
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1

object TwitterStream$ extends TwitterStreamApp[IO]

abstract class TwitterStreamApp[F[_] : Effect] extends StreamApp[F] {

  import com.banno.twittersampleendpoint.domain.SampleTweet
  import com.banno.twittersampleendpoint.service.{SampleTweetsCountingService, EmojiService, HashTagService, LinkService}
  import fs2.Pipe
  import io.circe.Printer
  import org.http4s.server.blaze.BlazeBuilder

  import scala.concurrent.ExecutionContext.Implicits.global

  // jawn-fs2 needs to know what JSON AST you want
  implicit val f = io.circe.jawn.CirceSupportParser.facade

  /* These values are created by a Twitter developer web app.
   * OAuth signing is an effect due to generating a nonce for each `Request`.
   */
  def sign(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
          (req: Request[F]): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  /* Create a http client, sign the incoming `Request[F]`, stream the `Response[F]`, and
   * `parseJsonStream` the `Response[F]`.
   * `sign` returns a `F`, so we need to `Stream.eval` it to use a for-comprehension.
   */
  def jsonStream(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
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

  def twitterStream: Stream[F, SampleTweet] = {
    val req = Request[F](Method.GET, Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))

    val s = jsonStream(consumerKey = "",
      consumerSecret = "",
      accessToken = "",
      accessSecret = "")(req)

    s.map { json =>
      json.as[SampleTweet].leftMap(pE => s"ParseError: ${pE.message} - ${json.pretty(Printer.noSpaces)}")
    }.through(filterLeft)
  }

  /*def stringToByteStream: Pipe[F, String, Byte] = {
    _.through(utf8Encode)
  }*/

  def analyticsToByteStream: Pipe[F, SampleTweetStreamAlytics, Byte] = {
    _.map(_.toString).through(utf8Encode)
  }

  def runAnalytics(sampleTweet: SampleTweet) =
    SampleTweetStreamAlytics(
      counter = SampleTweetsCountingService(),
      emoji = EmojiService.default(sampleTweet),
      hashTagStats = HashTagService.default(sampleTweet),
      linkStats = LinkService.default(sampleTweet)
    )

  def processTweetAnalysis: Pipe[F, SampleTweet, SampleTweetStreamAlytics] = {
    jsonStream => jsonStream.map(runAnalytics)
  }

  def processTweet(signal: Signal[F, SampleTweetStreamAlytics]): Pipe[F, SampleTweet, SampleTweetStreamAlytics] = {
    _.through(processTweetAnalysis)
      .scan(SampleTweetStreamAlytics.empty)(_ |+| _)
      .observe1(signal.set)
  }

  def server[F[_] : Effect](twitterSampleStreamRoute: HttpService[F]): Stream[F, ExitCode] =
    BlazeBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .mountService(twitterSampleStreamRoute, "/")
      .serve

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    for {
      signal <- Stream.eval(signalOf(SampleTweetStreamAlytics.empty))
      twitterSampleStreamRoute = new TwitterSampleStreamRoute[F](signal).service
      analyticsStream = twitterStream.through(processTweet(signal))
      analyticsResult = analyticsStream.through(analyticsToByteStream).through(stdout)
      serverStream = server(twitterSampleStreamRoute)
      stream <- serverStream merge (analyticsStream.drain) merge (analyticsResult.drain)
    } yield ExitCode.Success
  }

}
