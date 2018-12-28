package com.twitter.sampleendpoint

import cats.effect.IO
import com.twitter.sampleendpoint.TwitterSampleStreamRoute._
import com.twitter.sampleendpoint.UnitTestFixture._
import com.twitter.sampleendpoint.domain.SampleTweetStreamAnalytics
import com.twitter.algebird.SpaceSaver
import com.vdurmont.emoji.{Emoji, EmojiManager, EmojiParser => JEmojiParser}
import fs2.async._
import fs2.async.mutable.Signal
import io.circe.{Decoder, DecodingFailure, HCursor}
import io.circe.generic.semiauto.deriveDecoder
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.implicits._
import org.specs2.matcher.MatchResult

import scala.collection.JavaConverters._

class TwitterSampleStreamRouteSpec extends org.specs2.mutable.Specification {

  implicit val spaceSaverDecoder: Decoder[SpaceSaver[String]] = (c: HCursor) => {
    Right(SpaceSaver[String](10, "spaceSaver"))
  }

  implicit val spaceSaverEmojiDecoder: Decoder[SpaceSaver[Emoji]] = (c: HCursor) => {
    Right(SpaceSaver[Emoji](10, EmojiManager.getForAlias("thinking")))
  }

  implicit lazy val decodeEmoji: Decoder[Emoji] = Decoder.instance { c =>
    c.as[String].right.flatMap { s =>
      JEmojiParser
        .extractEmojis(s)
        .asScala.toVector
        .map(EmojiManager.getByUnicode) match {
        case emoji if emoji.nonEmpty =>
          Right(emoji.head)
        case _ =>
          Left(DecodingFailure("emoji", c.history))
      }
    }
  }
  implicit lazy val tweetRateMeanDecoder: Decoder[TweetRateMean] = deriveDecoder
  implicit lazy val tweetRateDecoder: Decoder[TweetRate] = deriveDecoder
  implicit lazy val tweetsRespDecoder: Decoder[TweetsResp] = deriveDecoder
  implicit lazy val topStringSpaceSaverDecoder: Decoder[TopStringSpaceSaver] = deriveDecoder
  implicit lazy val emojiRespDecoder: Decoder[EmojiResp] = deriveDecoder
  implicit lazy val hashTagRespDecoderr: Decoder[HashTagResp] = deriveDecoder
  implicit lazy val linksRespDecoder: Decoder[LinksResp] = deriveDecoder
  implicit lazy val twitterSampleStreamResponseDecoder: Decoder[TwitterSampleStreamResponse] = deriveDecoder
  implicit lazy val respEntityDecoder: EntityDecoder[IO, TwitterSampleStreamResponse] = jsonOf[IO, TwitterSampleStreamResponse]

  "GET analytics" >> {
    "return 200" >> {
      uriReturns200()
    }
    "Should Decode TwitterSampleStreamResponse" >> {
      uriReturnsHelloWorld()
    }
  }

  val sampleTweetSignal: Signal[IO, SampleTweetStreamAnalytics] =
    signalOf[IO, SampleTweetStreamAnalytics](genSampleTweetStreamAnalytics.sample.get).unsafeRunSync()

  private[this] val retTweetAnalytics: Response[IO] = {
    val getHW = Request[IO](Method.GET, Uri.uri("/analytics"))
    new TwitterSampleStreamRoute[IO](sampleTweetSignal).toService().orNotFound(getHW).unsafeRunSync()
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retTweetAnalytics.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsHelloWorld(): MatchResult[TwitterSampleStreamResponse] =
    retTweetAnalytics.as[TwitterSampleStreamResponse].unsafeRunSync() must beAnInstanceOf[TwitterSampleStreamResponse]

  val response: TwitterSampleStreamResponse = genTwitterSampleStreamResponse.sample.get

}
