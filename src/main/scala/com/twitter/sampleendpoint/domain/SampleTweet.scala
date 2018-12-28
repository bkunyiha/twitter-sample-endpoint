package com.twitter.sampleendpoint.domain

import java.time._
import java.time.format.DateTimeFormatter

import cats.effect.IO
import cats.{Eq, Show}
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

case class SampleTweet(created_at: ZonedDateTime,
                       id: BigInt,
                       text: String,
                       quote_count: BigInt,
                       reply_count: BigInt,
                       retweet_count: BigInt,
                       favorite_count: BigInt,
                       entities: Option[Entities] = None
                      )

object SampleTweet {

  implicit val SampleTwitDecoder: Decoder[SampleTweet] = (c: HCursor) => {
    for {
      created <- c.downField("created_at").as[ZonedDateTime]
      id <- c.downField("id").as[BigInt]
      text <- c.downField("text").as[String]
      quoted <- c.downField("quote_count").as[BigInt]
      replied <- c.downField("reply_count").as[BigInt]
      retweeted <- c.downField("retweet_count").as[BigInt]
      favorited <- c.downField("favorite_count").as[BigInt]
      entities <- c.downField("entities").as[Option[Entities]]

    } yield {
      SampleTweet(created, id, text, quoted, replied, retweeted, favorited, entities)
    }
  }

  implicit val ZonedDateTimeFormat: Encoder[ZonedDateTime] with Decoder[ZonedDateTime] =
    new Encoder[ZonedDateTime] with Decoder[ZonedDateTime] {
      override def apply(a: ZonedDateTime): Json =
        Encoder.encodeString(a.toString)

      override def apply(c: HCursor): Decoder.Result[ZonedDateTime] =
        Decoder.decodeString.map(str =>
          ZonedDateTime.parse(str, DateTimeFormatter.ofPattern("EE MMM dd HH:mm:ss xxxx uuuu")))(c)
    }

  // Entities Decoder
  implicit val configDecoder: EntityDecoder[IO, Entities] = jsonOf[IO, Entities]

  implicit val SampleTwitEq: Eq[SampleTweet] = Eq.fromUniversalEquals[SampleTweet]
  implicit val SampleTwitShow: Show[SampleTweet] = Show.fromToString[SampleTweet]

}

final case class HashTag(text: String, indices: Seq[Int])

final case class UserMention(id: Long,
                             id_str: String,
                             indices: Seq[Int] = Seq.empty,
                             name: String,
                             screen_name: String)

final case class UrlDetails(url: String, expanded_url: String, display_url: String, indices: Seq[Int])

final case class Urls(urls: Seq[UrlDetails] = Seq.empty)

final case class Media(display_url: String,
                       expanded_url: String,
                       id: Long,
                       id_str: String,
                       indices: Seq[Int],
                       media_url: String,
                       media_url_https: String,
                       sizes: Map[String, Size],
                       source_status_id: Option[Long],
                       source_status_id_str: Option[String],
                       `type`: String,
                       url: String)

final case class Size(h: Int, resize: String, w: Int)

final case class Entities(hashtags: Seq[HashTag] = Seq.empty,
                          media: Seq[Media] = Seq.empty,
                          url: Option[Urls] = None,
                          urls: Seq[UrlDetails] = Seq.empty,
                          user_mentions: Seq[UserMention] = Seq.empty,
                          description: Option[Urls] = None)

