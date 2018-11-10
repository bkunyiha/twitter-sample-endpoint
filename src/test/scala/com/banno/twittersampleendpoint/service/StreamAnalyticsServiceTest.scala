package com.banno.twittersampleendpoint.service

import cats.effect.IO
import com.banno.twittersampleendpoint.UnitTestFixture._
import com.banno.twittersampleendpoint.domain.{SampleTweet, SampleTweetStreamAnalytics}
import fs2.Stream
import fs2.async.signalOf
import org.scalatest.{FlatSpec, Matchers}

class StreamAnalyticsServiceTest extends FlatSpec with Matchers {

  val streamAnalyticsService = new StreamAnalyticsService[IO]

  "twitterStream" should "convert Json To SampleTweet Object" in {
    val sampleTweet: SampleTweet = Stream
      .emit(sampleStreamJson)
      .covary[IO]
      .through(streamAnalyticsService.sampleTweetStream)
      .compile
      .last
      .unsafeRunSync()
      .get

    sampleTweet.id shouldBe 1060069206656475136L
  }

  "processTweet" should "perform anaytics and get a count of just one tweet" in {

    val signal = signalOf[IO, SampleTweetStreamAnalytics](SampleTweetStreamAnalytics.empty).unsafeRunSync()

    val sampleTweetAnalytics: SampleTweetStreamAnalytics = Stream
      .emit(sampleStreamJson)
      .covary[IO]
      .through(streamAnalyticsService.sampleTweetStream)
      .through(streamAnalyticsService.processTweet(signal))
      .compile
      .last
      .unsafeRunSync()
      .get

    sampleTweetAnalytics.counter.count shouldBe 1
  }
}
