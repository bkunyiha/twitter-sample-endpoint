package com.banno.twittersampleendpoint.domain

import cats.Monoid
import cats.implicits._
import com.banno.twittersampleendpoint.service.{Counter, EmojiStats, HashTagStats, LinkStats}

final case class SampleTweetStreamAlytics(counter: Counter, emoji: EmojiStats, hashTagStats: HashTagStats, linkStats: LinkStats)

object SampleTweetStreamAlytics {

  val empty = SampleTweetStreamAlytics(
    counter = Counter.zero,
    emoji = EmojiStats.zero,
    hashTagStats = HashTagStats.zero,
    linkStats = LinkStats.zero
  )

  implicit val alyticsInstances: Monoid[SampleTweetStreamAlytics] = new Monoid[SampleTweetStreamAlytics] {
    def empty: SampleTweetStreamAlytics = SampleTweetStreamAlytics.empty

    def combine(x: SampleTweetStreamAlytics, y: SampleTweetStreamAlytics) = SampleTweetStreamAlytics(
      counter = x.counter |+| y.counter,
      emoji = x.emoji |+| y.emoji,
      hashTagStats = x.hashTagStats |+| y.hashTagStats,
      linkStats = x.linkStats |+| y.linkStats
    )
  }
}

