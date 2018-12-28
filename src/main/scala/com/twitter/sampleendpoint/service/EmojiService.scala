/* Original work from Original work from https://github.com/rossabaker/twitalytics
 *
 * Changes Made
 *  -  Using custom tweeter stream decoded object (SampleTweet)
 */
package com.twitter.sampleendpoint.service

import cats.implicits._
import cats.{Eq, Monoid}
import com.twitter.sampleendpoint.domain.SampleTweet
import com.twitter.algebird.SpaceSaver
import com.vdurmont.emoji.{Emoji, EmojiManager, EmojiParser => JEmojiParser}

import scala.collection.JavaConverters._

/** Emoji-related code.  Parsing emoji turns out to be highly
  * complicated because there are multi-character emoji and
  * Fitzpatrick modifiers and all sorts of nuances I'd never heard of.
  * Fortunately, this is the JVM, and There's a Library For That.
  */

class EmojiService private(capacity: Int) {

  /** Stats for a tweet with no emoji */

  private val NoEmojiStats = EmojiStats(count = 1L,
    hasEmoji = 0L,
    topEmoji = None
  )

  def extractEmoji(s: String): Vector[Emoji] =
    JEmojiParser.extractEmojis(s).asScala.toVector.map(EmojiManager.getByUnicode)

  def apply(tweet: SampleTweet): EmojiStats =
    extractEmoji(tweet.text) match {
      case emoji if emoji.nonEmpty =>
        EmojiStats(
          count = 1L,
          hasEmoji = 1L,
          topEmoji = emoji.map(SpaceSaver(capacity, _)).reduceLeftOption(_ |+| _)
        )
      case _ =>
        NoEmojiStats
    }
}

object EmojiService {
  /** Creates an emoji service.
    *
    * @capacity The capacity of the space saver.  Larger trades space and time
    *           for accuracy on the long tail.
    */
  def apply(capacity: Int) = new EmojiService(capacity)

  val default = apply(100)
}

/** Stats about emoji.  We keep a count here, which is redundant for now,
  * but convenient for the percentage and would let us spin this off into
  * its own service.
  *
  * A SpaceSaver can't be empty.  It has a Semigroup.  We regain a
  * Monoid by making it an Option.
  */
final case class EmojiStats(count: Long,
                            hasEmoji: Long,
                            topEmoji: Option[SpaceSaver[Emoji]]
                           ) {
  def percentWithEmoji: Double = {
    if (count > 0L) 100.0 * hasEmoji.toDouble / count.toDouble
    else 0L
  }
}

object EmojiStats {
  val zero = EmojiStats(count = 0L, hasEmoji = 0L, topEmoji = None)

  implicit val emojiStatsInstances: Monoid[EmojiStats] with Eq[EmojiStats] = new Monoid[EmojiStats] with Eq[EmojiStats] {
    def empty: EmojiStats = zero

    def combine(x: EmojiStats, y: EmojiStats): EmojiStats = EmojiStats(
      count = x.count + y.count,
      hasEmoji = x.hasEmoji + y.hasEmoji,
      topEmoji = x.topEmoji |+| y.topEmoji
    )

    def eqv(x: EmojiStats, y: EmojiStats): Boolean = {
      x.count === y.count &&
        x.hasEmoji === y.hasEmoji &&
        (x.topEmoji, y.topEmoji).mapN((xte, yte) =>
          (xte consistentWith yte) && (yte.consistentWith(xte)))
          .getOrElse(x.topEmoji.isEmpty && y.topEmoji.isEmpty)
    }
  }
}
