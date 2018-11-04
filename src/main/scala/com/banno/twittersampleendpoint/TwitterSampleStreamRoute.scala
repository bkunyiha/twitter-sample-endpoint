package com.banno.twittersampleendpoint

import cats.effect.Effect
import com.banno.twittersampleendpoint.RouteEncoders._
import com.banno.twittersampleendpoint.TwitterSampleStreamRoute._
import com.banno.twittersampleendpoint.domain.SampleTweetStreamAlytics
import com.twitter.algebird.SpaceSaver
import com.vdurmont.emoji.Emoji
import fs2.async.mutable.Signal
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpService}

class TwitterSampleStreamRoute[F[_] : Effect](signalStats: Signal[F, SampleTweetStreamAlytics]) extends Http4sDsl[F] {

  implicit lazy val respEntityEncoder: EntityEncoder[F, TwitterSampleStreamResponse] = jsonEncoderOf[F, TwitterSampleStreamResponse]

  val service: HttpService[F] = {
    HttpService[F] {

      case GET -> Root =>
        import cats.Monad
        Monad[F].flatMap(signalStats.get) {
          sampleStats: SampleTweetStreamAlytics => {
            Ok(responseObj(sampleStats))
          }
        }
    }
  }
}

object TwitterSampleStreamRoute {

  val startTime = System.currentTimeMillis

  def responseObj(sampleStats: SampleTweetStreamAlytics): TwitterSampleStreamResponse = {
    val now = System.currentTimeMillis

    val average = sampleStats.counter.averageFrom(startTime, now)

    val tweetRateMean = TweetRateMean(perHour = average.rateMean * MillisPerHour,
      perMinute = average.rateMean * MillisPerMinute,
      perSecond = average.rateMean * MillisPerSecond)
    val tweetRate = TweetRate(mean = tweetRateMean)
    val tweetsResp = TweetsResp(total = sampleStats.counter.count, rate = tweetRate)

    val topEmoji: Seq[SpaceSaver[Emoji]] = sampleStats.emoji.topEmoji.toList
    val emojiResp = EmojiResp(topEmoji = topEmoji,
      percentWithEmoji = sampleStats.emoji.percentWithEmoji)

    val topHashTags = sampleStats.hashTagStats.topHashTags.toList
    val hashTagResp = HashTagResp(topHashTags = topHashTags, percentWithHashTags = sampleStats.hashTagStats.percentWithHashTag)
    val topDomains = sampleStats.linkStats.topDomains.toList

    val linksResp = LinksResp(percentWithUrl = sampleStats.linkStats.percentWithUrl,
      percentWithPhotoUrl = sampleStats.linkStats.percentWithImageLink,
      topDomains = topDomains)

    TwitterSampleStreamResponse(uptime = System.currentTimeMillis - startTime,
      tweets = tweetsResp,
      emoji = emojiResp,
      hashTags = hashTagResp,
      links = linksResp
    )
  }

  case class TwitterSampleStreamResponse(uptime: Long,
                                         tweets: TweetsResp,
                                         emoji: EmojiResp,
                                         hashTags: HashTagResp,
                                         links: LinksResp
                                        )

  case class TweetRateMean(perHour: Double, perMinute: Double, perSecond: Double)

  case class TweetRate(mean: TweetRateMean)

  case class TweetsResp(total: Long, rate: TweetRate)

  case class TopStringSpaceSaver(topHashTags: Option[SpaceSaver[String]])

  case class EmojiResp(topEmoji: Seq[SpaceSaver[Emoji]], percentWithEmoji: Double)

  case class HashTagResp(topHashTags: Seq[SpaceSaver[String]], percentWithHashTags: Double)

  case class LinksResp(percentWithUrl: Double, percentWithPhotoUrl: Double, topDomains: Seq[SpaceSaver[String]])

}