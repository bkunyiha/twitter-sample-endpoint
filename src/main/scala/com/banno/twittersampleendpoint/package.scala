package com.banno

import fs2.{Pipe, Stream}

import scala.concurrent.duration._

package object twittersampleendpoint {

  val MillisPerHour = 1.hour.toMillis
  val MillisPerMinute = 1.minute.toMillis
  val MillisPerSecond = 1.second.toMillis

  def filterLeft[F[_], A, B]: Pipe[F, Either[A, B], B] = _.flatMap {
    case Right(r) => Stream.emit(r)
    case Left(_) => Stream.empty
  }

  object RouteEncoders {

    import com.banno.twittersampleendpoint.TwitterSampleStreamRoute._
    import com.twitter.algebird.{Approximate, SpaceSaver}
    import com.vdurmont.emoji.Emoji
    import io.circe.generic.semiauto.deriveEncoder
    import io.circe.syntax._
    import io.circe.{Encoder, Json}

    implicit def SpaceSaverEncoder[A: Encoder]: Encoder[SpaceSaver[A]] = (ss: SpaceSaver[A]) =>
      Json.fromValues(
        ss.topK(10).collect {
          case (a, Approximate(_, estimate, _, _), true) =>
            Json.obj(
              "name" -> a.asJson,
              "count" -> Json.fromLong(estimate)
            )
        }
      )

    implicit val EmojiEncoder: Encoder[Emoji] = (emoji: Emoji) => Json.fromString(emoji.getDescription)

    implicit lazy val tweetRateMeanEncoder: Encoder[TweetRateMean] = deriveEncoder
    implicit lazy val tweetRateEncoder: Encoder[TweetRate] = deriveEncoder
    implicit lazy val tweetsRespEncoder: Encoder[TweetsResp] = deriveEncoder
    implicit lazy val topStringSpaceSaverEncoder: Encoder[TopStringSpaceSaver] = deriveEncoder
    implicit lazy val emojiRespEncoder: Encoder[EmojiResp] = deriveEncoder
    implicit lazy val hashTagRespEncoder: Encoder[HashTagResp] = deriveEncoder
    implicit lazy val linksRespEncoder: Encoder[LinksResp] = deriveEncoder
    implicit lazy val respEncoder: Encoder[TwitterSampleStreamResponse] = deriveEncoder


  }
}
