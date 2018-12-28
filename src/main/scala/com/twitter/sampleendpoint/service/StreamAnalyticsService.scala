package com.twitter.sampleendpoint.service

import cats.effect._
import cats.implicits._
import com.twitter.sampleendpoint.domain.{SampleTweet, SampleTweetStreamAnalytics}
import com.twitter.sampleendpoint.filterLeft
import fs2.Pipe
import fs2.async.mutable.Signal
import io.circe.{Json, Printer}
import org.http4s.{BuildInfo => _}

class StreamAnalyticsService[F[_] : Effect] {

  def sampleTweetStream: Pipe[F, Json, SampleTweet] = {
    _.map { json =>
      json.as[SampleTweet].leftMap(pE => s"ParseError: ${pE.message} - ${json.pretty(Printer.noSpaces)}")
    }.through(filterLeft)
  }

  def sampleTweetStreamAnalytics(sampleTweet: SampleTweet): SampleTweetStreamAnalytics =
    SampleTweetStreamAnalytics(
      counter = SampleTweetsCountingService(),
      emoji = EmojiService.default(sampleTweet),
      hashTagStats = HashTagService.default(sampleTweet),
      linkStats = LinkService.default(sampleTweet)
    )

  def processTweetAnalysis: Pipe[F, SampleTweet, SampleTweetStreamAnalytics] = {
    jsonStream => jsonStream.map(sampleTweetStreamAnalytics)
  }

  def processTweet(signal: Signal[F, SampleTweetStreamAnalytics]): Pipe[F, SampleTweet, SampleTweetStreamAnalytics] = {
    _.through(processTweetAnalysis)
      .scan(SampleTweetStreamAnalytics.empty)(_ |+| _)
      .observe1(signal.set)
  }

}
