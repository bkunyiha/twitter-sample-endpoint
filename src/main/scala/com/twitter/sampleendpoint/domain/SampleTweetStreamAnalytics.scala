package com.twitter.sampleendpoint.domain

import cats.Monoid
import cats.implicits._
import com.twitter.sampleendpoint.service.{Counter, EmojiStats, HashTagStats, LinkStats}

final case class SampleTweetStreamAnalytics(counter: Counter, emoji: EmojiStats, hashTagStats: HashTagStats, linkStats: LinkStats)

object SampleTweetStreamAnalytics {

  val empty = SampleTweetStreamAnalytics(
    counter = Counter.zero,
    emoji = EmojiStats.zero,
    hashTagStats = HashTagStats.zero,
    linkStats = LinkStats.zero
  )

  implicit val analyticsInstances: Monoid[SampleTweetStreamAnalytics] = new Monoid[SampleTweetStreamAnalytics] {
    def empty: SampleTweetStreamAnalytics = SampleTweetStreamAnalytics.empty

    def combine(x: SampleTweetStreamAnalytics, y: SampleTweetStreamAnalytics) = SampleTweetStreamAnalytics(
      counter = x.counter |+| y.counter,
      emoji = x.emoji |+| y.emoji,
      hashTagStats = x.hashTagStats |+| y.hashTagStats,
      linkStats = x.linkStats |+| y.linkStats
    )
  }
}

