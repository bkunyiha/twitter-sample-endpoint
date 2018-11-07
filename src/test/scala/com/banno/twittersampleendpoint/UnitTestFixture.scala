package com.banno.twittersampleendpoint

import io.circe.parser._
import com.banno.twittersampleendpoint.domain.SampleTweetStreamAnalytics
import com.banno.twittersampleendpoint.TwitterSampleStreamRoute._
import com.banno.twittersampleendpoint.service.{Counter, EmojiStats, HashTagStats, LinkStats}
import com.twitter.algebird.SpaceSaver
import com.vdurmont.emoji.{Emoji, EmojiManager}
import org.scalacheck.Arbitrary.{arbDouble, arbLong, arbitrary}
import org.scalacheck.Gen.{choose, chooseNum, oneOf}
import org.scalacheck.{Arbitrary, Gen}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.collection.JavaConverters._

object UnitTestFixture {

  implicit val context: ExecutionContextExecutor = ExecutionContext.global

  def genSampleTweetStreamAnalytics: Gen[SampleTweetStreamAnalytics] = {
    for {
      arbCounter <- arbitraryCounter.arbitrary
      arbEmoji <- arbitraryEmojiStats.arbitrary
      arbHashTagStats <- arbitraryHashTagStats.arbitrary
      arbLinkStats <- linkStats.arbitrary
    } yield SampleTweetStreamAnalytics(arbCounter, arbEmoji, arbHashTagStats, arbLinkStats)
  }

  def genTwitterSampleStreamResponse: Gen[TwitterSampleStreamResponse] = {
    for {
      arbUptime <- arbLong.arbitrary
      arbTweets <- genTweetsResp
      arbEmoji <- genEmojiResp
      arbHashTags <- genHashTagResp
      arbLinks <- genLinksResp
    } yield {
      TwitterSampleStreamResponse(uptime = arbUptime,
        tweets = arbTweets,
        emoji = arbEmoji,
        hashTags = arbHashTags,
        links = arbLinks
      )
    }
  }

  def genTweetMean: Gen[TweetRateMean] = {
    for {
      arbPerHour <- arbDouble.arbitrary
      arbPerMinute <- arbDouble.arbitrary
      arbPerSecond <- arbDouble.arbitrary
    } yield TweetRateMean(perHour = arbPerHour, perMinute = arbPerMinute, perSecond = arbPerSecond)
  }

  def genTweetRate: Gen[TweetRate] = {
    for {
      arbMean <- genTweetMean
      arbPerMinute <- arbDouble.arbitrary
      arbPerSecond <- arbDouble.arbitrary
    } yield TweetRate(arbMean)
  }

  def genTweetsResp: Gen[TweetsResp] = {
    for {
      arbTotal <- arbLong.arbitrary
      arbRate <- genTweetRate
    } yield TweetsResp(total = arbTotal, rate = arbRate)
  }

  def genTopStringSpaceSaver: Gen[TopStringSpaceSaver] = {
    for {
      arbSpaceSaver <- arbitrarySpaceSaver[String].arbitrary
    } yield TopStringSpaceSaver(topHashTags = Option(arbSpaceSaver))
  }

  def genEmojiResp: Gen[EmojiResp] = {
    for {
      arbSpaceSaver <- arbitrarySpaceSaver[Emoji].arbitrary
      arbDouble <- arbDouble.arbitrary
    } yield EmojiResp(topEmoji = Seq(arbSpaceSaver), percentWithEmoji = arbDouble)
  }

  def genHashTagResp: Gen[HashTagResp] = {
    for {
      arbSpaceSaver <- arbitrarySpaceSaver[String].arbitrary
      arbDouble <- arbDouble.arbitrary
    } yield HashTagResp(topHashTags = Seq(arbSpaceSaver), percentWithHashTags = arbDouble)
  }

  def genLinksResp: Gen[LinksResp] =
    for {
      arbPec <- arbDouble.arbitrary
      arbWithUrl <- arbDouble.arbitrary
      arbSpaceSaver <- arbitrarySpaceSaver[String].arbitrary
    } yield LinksResp(percentWithUrl = arbPec, percentWithPhotoUrl = arbWithUrl, topDomains = Seq(arbSpaceSaver))

  implicit def arbitraryCounter: Arbitrary[Counter] = Arbitrary(for {
    count <- arbitrary[Long]
  } yield Counter(count))

  def genSpaceSaver[A](implicit A: Arbitrary[A]): Gen[SpaceSaver[A]] =
    for {
      item <- A.arbitrary
    } yield SpaceSaver(capacity = 100, item = item)

  private val allEmoji: Seq[Emoji] = EmojiManager.getAll.asScala.toVector

  implicit def arbitraryEmoji: Arbitrary[Emoji] = Arbitrary(oneOf(allEmoji))

  implicit def arbitrarySpaceSaver[A: Arbitrary]: Arbitrary[SpaceSaver[A]] =
    Arbitrary(genSpaceSaver)

  implicit def arbitraryEmojiStats: Arbitrary[EmojiStats] = Arbitrary(for {
    count <- chooseNum(0L, Long.MaxValue)
    hasEmoji <- choose(0L, count)
    topEmoji <- arbitrary[Option[SpaceSaver[Emoji]]]
  } yield EmojiStats(count, hasEmoji, topEmoji))

  implicit def arbitraryHashTagStats: Arbitrary[HashTagStats] = Arbitrary(for {
    count <- chooseNum(0L, Long.MaxValue)
    hasHasTags <- choose(0L, count)
    topHashTags <- arbitrary[Option[SpaceSaver[String]]]
  } yield HashTagStats(count, hasHasTags, topHashTags))

  implicit def linkStats: Arbitrary[LinkStats] = Arbitrary(for {
    count <- chooseNum(0L, Long.MaxValue)
    hasUri <- choose(0L, count)
    hasPicture <- choose(0L, hasUri)
    topDomains <- arbitrary[Option[SpaceSaver[String]]]
  } yield LinkStats(count, hasUri, hasPicture, topDomains))

  val sampleStreamJson = parse(sampleStreamJsonString).toOption.get

  def sampleStreamJsonString: String = {
    """{
      |	"created_at": "Wed Nov 07 07:19:28 +0000 2018",
      |	"id": 1060069206656475136,
      |	"id_str": "1060069206656475136",
      |	"text": "RT @PerezMaphuti: Catch Mercy Pakela on @METROFMSA #FreshBreakfast https://t.co/NKfmrUqIoc",
      |	"source": "<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>",
      |	"truncated": false,
      |	"in_reply_to_status_id": null,
      |	"in_reply_to_status_id_str": null,
      |	"in_reply_to_user_id": null,
      |	"in_reply_to_user_id_str": null,
      |	"in_reply_to_screen_name": null,
      |	"user": {
      |		"id": 1051776423390863361,
      |		"id_str": "1051776423390863361",
      |		"name": "Issa Vibe Entertainment",
      |		"screen_name": "IssaVibeEntert1",
      |		"location": null,
      |		"url": null,
      |		"description": null,
      |		"translator_type": "none",
      |		"protected": false,
      |		"verified": false,
      |		"followers_count": 4,
      |		"friends_count": 24,
      |		"listed_count": 0,
      |		"favourites_count": 1,
      |		"statuses_count": 3,
      |		"created_at": "Mon Oct 15 10:06:55 +0000 2018",
      |		"utc_offset": null,
      |		"time_zone": null,
      |		"geo_enabled": false,
      |		"lang": "en",
      |		"contributors_enabled": false,
      |		"is_translator": false,
      |		"profile_background_color": "F5F8FA",
      |		"profile_background_image_url": "",
      |		"profile_background_image_url_https": "",
      |		"profile_background_tile": false,
      |		"profile_link_color": "1DA1F2",
      |		"profile_sidebar_border_color": "C0DEED",
      |		"profile_sidebar_fill_color": "DDEEF6",
      |		"profile_text_color": "333333",
      |		"profile_use_background_image": true,
      |		"profile_image_url": "http://pbs.twimg.com/profile_images/1051811933953896449/AYn8F68s_normal.jpg",
      |		"profile_image_url_https": "https://pbs.twimg.com/profile_images/1051811933953896449/AYn8F68s_normal.jpg",
      |		"default_profile": true,
      |		"default_profile_image": false,
      |		"following": null,
      |		"follow_request_sent": null,
      |		"notifications": null
      |	},
      |	"geo": null,
      |	"coordinates": null,
      |	"place": null,
      |	"contributors": null,
      |	"retweeted_status": {
      |		"created_at": "Wed Nov 07 03:24:08 +0000 2018",
      |		"id": 1060009984262594563,
      |		"id_str": "1060009984262594563",
      |		"text": "Catch Mercy Pakela on @METROFMSA #FreshBreakfast https://t.co/NKfmrUqIoc",
      |		"display_text_range": [0, 48],
      |		"source": "<a href=\"https://mobile.twitter.com\" rel=\"nofollow\">Twitter Lite</a>",
      |		"truncated": false,
      |		"in_reply_to_status_id": null,
      |		"in_reply_to_status_id_str": null,
      |		"in_reply_to_user_id": null,
      |		"in_reply_to_user_id_str": null,
      |		"in_reply_to_screen_name": null,
      |		"user": {
      |			"id": 1420279004,
      |			"id_str": "1420279004",
      |			"name": "PEREZ GLOBAL 🌎",
      |			"screen_name": "PerezMaphuti",
      |			"location": "Johannesburg",
      |			"url": null,
      |			"description": "I break doors| Alpha Female |Radio|  Media liason|Brand builder| i wear a lot of crowns| @cellc| Moshito| Monash Graduate♥\n perezmothapo@gmail.com",
      |			"translator_type": "none",
      |			"protected": false,
      |			"verified": false,
      |			"followers_count": 703,
      |			"friends_count": 785,
      |			"listed_count": 10,
      |			"favourites_count": 3892,
      |			"statuses_count": 4154,
      |			"created_at": "Sat May 11 10:05:19 +0000 2013",
      |			"utc_offset": null,
      |			"time_zone": null,
      |			"geo_enabled": true,
      |			"lang": "en",
      |			"contributors_enabled": false,
      |			"is_translator": false,
      |			"profile_background_color": "000000",
      |			"profile_background_image_url": "http://abs.twimg.com/images/themes/theme10/bg.gif",
      |			"profile_background_image_url_https": "https://abs.twimg.com/images/themes/theme10/bg.gif",
      |			"profile_background_tile": false,
      |			"profile_link_color": "981CEB",
      |			"profile_sidebar_border_color": "000000",
      |			"profile_sidebar_fill_color": "000000",
      |			"profile_text_color": "000000",
      |			"profile_use_background_image": false,
      |			"profile_image_url": "http://pbs.twimg.com/profile_images/993825669019783168/HxD6Qj-Y_normal.jpg",
      |			"profile_image_url_https": "https://pbs.twimg.com/profile_images/993825669019783168/HxD6Qj-Y_normal.jpg",
      |			"profile_banner_url": "https://pbs.twimg.com/profile_banners/1420279004/1463866640",
      |			"default_profile": false,
      |			"default_profile_image": false,
      |			"following": null,
      |			"follow_request_sent": null,
      |			"notifications": null
      |		},
      |		"geo": null,
      |		"coordinates": null,
      |		"place": null,
      |		"contributors": null,
      |		"is_quote_status": false,
      |		"quote_count": 2,
      |		"reply_count": 1,
      |		"retweet_count": 3,
      |		"favorite_count": 6,
      |		"entities": {
      |			"hashtags": [{
      |				"text": "FreshBreakfast",
      |				"indices": [33, 48]
      |			}],
      |			"urls": [],
      |			"user_mentions": [{
      |				"screen_name": "METROFMSA",
      |				"name": "METROFM SABC",
      |				"id": 485688281,
      |				"id_str": "485688281",
      |				"indices": [22, 32]
      |			}],
      |			"symbols": [],
      |			"media": [{
      |				"id": 1060009965715419141,
      |				"id_str": "1060009965715419141",
      |				"indices": [49, 72],
      |				"media_url": "http://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |				"media_url_https": "https://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |				"url": "https://t.co/NKfmrUqIoc",
      |				"display_url": "pic.twitter.com/NKfmrUqIoc",
      |				"expanded_url": "https://twitter.com/PerezMaphuti/status/1060009984262594563/photo/1",
      |				"type": "photo",
      |				"sizes": {
      |					"large": {
      |						"w": 1067,
      |						"h": 1009,
      |						"resize": "fit"
      |					},
      |					"thumb": {
      |						"w": 150,
      |						"h": 150,
      |						"resize": "crop"
      |					},
      |					"medium": {
      |						"w": 1067,
      |						"h": 1009,
      |						"resize": "fit"
      |					},
      |					"small": {
      |						"w": 680,
      |						"h": 643,
      |						"resize": "fit"
      |					}
      |				}
      |			}]
      |		},
      |		"extended_entities": {
      |			"media": [{
      |				"id": 1060009965715419141,
      |				"id_str": "1060009965715419141",
      |				"indices": [49, 72],
      |				"media_url": "http://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |				"media_url_https": "https://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |				"url": "https://t.co/NKfmrUqIoc",
      |				"display_url": "pic.twitter.com/NKfmrUqIoc",
      |				"expanded_url": "https://twitter.com/PerezMaphuti/status/1060009984262594563/photo/1",
      |				"type": "photo",
      |				"sizes": {
      |					"large": {
      |						"w": 1067,
      |						"h": 1009,
      |						"resize": "fit"
      |					},
      |					"thumb": {
      |						"w": 150,
      |						"h": 150,
      |						"resize": "crop"
      |					},
      |					"medium": {
      |						"w": 1067,
      |						"h": 1009,
      |						"resize": "fit"
      |					},
      |					"small": {
      |						"w": 680,
      |						"h": 643,
      |						"resize": "fit"
      |					}
      |				}
      |			}]
      |		},
      |		"favorited": false,
      |		"retweeted": false,
      |		"possibly_sensitive": false,
      |		"filter_level": "low",
      |		"lang": "en"
      |	},
      |	"is_quote_status": false,
      |	"quote_count": 0,
      |	"reply_count": 0,
      |	"retweet_count": 0,
      |	"favorite_count": 0,
      |	"entities": {
      |		"hashtags": [{
      |			"text": "FreshBreakfast",
      |			"indices": [51, 66]
      |		}],
      |		"urls": [],
      |		"user_mentions": [{
      |			"screen_name": "PerezMaphuti",
      |			"name": "PEREZ GLOBAL 🌎",
      |			"id": 1420279004,
      |			"id_str": "1420279004",
      |			"indices": [3, 16]
      |		}, {
      |			"screen_name": "METROFMSA",
      |			"name": "METROFM SABC",
      |			"id": 485688281,
      |			"id_str": "485688281",
      |			"indices": [40, 50]
      |		}],
      |		"symbols": [],
      |		"media": [{
      |			"id": 1060009965715419141,
      |			"id_str": "1060009965715419141",
      |			"indices": [67, 90],
      |			"media_url": "http://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |			"media_url_https": "https://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |			"url": "https://t.co/NKfmrUqIoc",
      |			"display_url": "pic.twitter.com/NKfmrUqIoc",
      |			"expanded_url": "https://twitter.com/PerezMaphuti/status/1060009984262594563/photo/1",
      |			"type": "photo",
      |			"sizes": {
      |				"large": {
      |					"w": 1067,
      |					"h": 1009,
      |					"resize": "fit"
      |				},
      |				"thumb": {
      |					"w": 150,
      |					"h": 150,
      |					"resize": "crop"
      |				},
      |				"medium": {
      |					"w": 1067,
      |					"h": 1009,
      |					"resize": "fit"
      |				},
      |				"small": {
      |					"w": 680,
      |					"h": 643,
      |					"resize": "fit"
      |				}
      |			},
      |			"source_status_id": 1060009984262594563,
      |			"source_status_id_str": "1060009984262594563",
      |			"source_user_id": 1420279004,
      |			"source_user_id_str": "1420279004"
      |		}]
      |	},
      |	"extended_entities": {
      |		"media": [{
      |			"id": 1060009965715419141,
      |			"id_str": "1060009965715419141",
      |			"indices": [67, 90],
      |			"media_url": "http://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |			"media_url_https": "https://pbs.twimg.com/media/DrXpcpjXcAU04Y-.jpg",
      |			"url": "https://t.co/NKfmrUqIoc",
      |			"display_url": "pic.twitter.com/NKfmrUqIoc",
      |			"expanded_url": "https://twitter.com/PerezMaphuti/status/1060009984262594563/photo/1",
      |			"type": "photo",
      |			"sizes": {
      |				"large": {
      |					"w": 1067,
      |					"h": 1009,
      |					"resize": "fit"
      |				},
      |				"thumb": {
      |					"w": 150,
      |					"h": 150,
      |					"resize": "crop"
      |				},
      |				"medium": {
      |					"w": 1067,
      |					"h": 1009,
      |					"resize": "fit"
      |				},
      |				"small": {
      |					"w": 680,
      |					"h": 643,
      |					"resize": "fit"
      |				}
      |			},
      |			"source_status_id": 1060009984262594563,
      |			"source_status_id_str": "1060009984262594563",
      |			"source_user_id": 1420279004,
      |			"source_user_id_str": "1420279004"
      |		}]
      |	},
      |	"favorited": false,
      |	"retweeted": false,
      |	"possibly_sensitive": false,
      |	"filter_level": "low",
      |	"lang": "en",
      |	"timestamp_ms": "1541575168657"
      |}""".stripMargin.trim
  }
}
