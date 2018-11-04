/* Original work from https://github.com/rossabaker/twitalytics
 *
 * Changes Made
 *  -  Using custom tweeter stream decoded object (SampleTweet)
 *  - Added counter for aggregating all tweets
 *  - Added aggregator for current tweets with hashtags
 *  - Added percentage tweets with hashtags
 */

package com.banno.twittersampleendpoint.service

import cats.implicits._
import cats.{Eq, Monoid}
import com.banno.twittersampleendpoint.domain.SampleTweet
import com.twitter.algebird.SpaceSaver

/** Process the hash tags in a tweet */
class HashTagService private(capacity: Int) {

  def apply(sampleTweet: SampleTweet): HashTagStats = {

    val hashtagOption = sampleTweet.entities.flatMap(_.hashtags match {
      case hashTags if hashTags.nonEmpty =>
        hashTags.map(_.text).map(SpaceSaver(capacity, _)).reduceLeftOption(_ |+| _)
      case _ =>
        None
    })

    hashtagOption match {
      case tag@Some(_) => HashTagStats(count = 1L,
        hasHashTag = 1L,
        topHashTags = tag
      )
      case None => HashTagStats(count = 1L,
        hasHashTag = 0L,
        topHashTags = None
      )
    }
  }
}

object HashTagService {
  /** Creates an Tweet service.
    *
    * @capacity The capacity of the space saver.  Larger trades space and time
    *           for accuracy on the long tail.
    */
  def apply(capacity: Int) = new HashTagService(capacity)

  val default = apply(100)
}

/** Stats about hash tags.
  *
  * A SpaceSaver can't be empty.  It has a Semigroup.  We regain a
  * Monoid by making it an Option.
  */

final case class HashTagStats(count: Long,
                              hasHashTag: Long,
                              topHashTags: Option[SpaceSaver[String]]
                             ) {

  def percentWithHashTag: Double = {
    if (count > 0L) 100.0 * hasHashTag.toDouble / count.toDouble
    else 0L
  }
}

object HashTagStats {
  val zero = HashTagStats(count = 0L, hasHashTag = 0L, topHashTags = None)

  implicit val hashTagStatsInstance: Monoid[HashTagStats] with Eq[HashTagStats] = new Monoid[HashTagStats] with Eq[HashTagStats] {
    def empty: HashTagStats = zero

    def combine(x: HashTagStats, y: HashTagStats): HashTagStats = HashTagStats(
      count = x.count |+| y.count,
      hasHashTag = x.hasHashTag |+| y.hasHashTag,
      topHashTags = x.topHashTags |+| y.topHashTags
    )

    def eqv(x: HashTagStats, y: HashTagStats) = {
      (x.topHashTags, y.topHashTags).mapN((xtht, ytht) =>
        (xtht consistentWith ytht) && (ytht.consistentWith(xtht)))
        .getOrElse(x.topHashTags.isEmpty && y.topHashTags.isEmpty)
    }
  }
}

