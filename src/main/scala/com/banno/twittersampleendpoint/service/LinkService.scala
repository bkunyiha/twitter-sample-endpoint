/* Original work from https://github.com/rossabaker/twitalytics
 *
 * Changes Made
 *  -  Using custom tweeter stream decoded object (SampleTweet)
 */

package com.banno.twittersampleendpoint.service

import java.net.URL

import cats.implicits._
import cats.{Eq, Monoid}
import com.banno.twittersampleendpoint.domain.SampleTweet
import com.twitter.algebird.SpaceSaver

import scala.util.{Success, Try}

/** Processes the links in a tweet */

class LinkService private(capacity: Int, isPicture: URL => Boolean) {
  def apply(sampleTweet: SampleTweet): LinkStats = {
    val entities = sampleTweet.entities
    val javaUrls: Seq[Try[URL]] = entities.fold(Seq.empty[Try[URL]])(_.urls.map { url => Try(new java.net.URL(url.expanded_url)) })
    val goodUrls: Seq[URL] = javaUrls.collect {
      case Success(url) => url
    }
    val domains: Option[SpaceSaver[String]] = goodUrls.map(_.getHost).filter(_.contains(".")) match {
      case ds if ds.nonEmpty =>
        ds.toVector.map(SpaceSaver(capacity, _)).reduceLeftOption(_ |+| _)
      case _ =>
        None
    }

    LinkStats(
      count = 1L,
      hasUrl = if (javaUrls.nonEmpty) 1L else 0L, // don't care if they're valid or not,
      hasImageLink = if (goodUrls.exists(isPicture)) 1L else 0L,
      topDomains = domains
    )
  }
}

object LinkService {
  /** Creates a link service
    *
    * @capacity The capacity of the space saver.  Larger trades space and time
    *           for accuracy on the long tail.
    * @isPicture a predicate to determine if a URL is a picture.
    *            One can imagine a future version turning this into an effect
    *            and calling HEAD on the URL to get the content type.  For
    *            today, we require it to be a pure function.
    */
  def apply(capacity: Int, isPicture: URL => Boolean) = new LinkService(capacity, isPicture)

  val default = LinkService(1000,
    url => (
      url.getHost.endsWith(".instagram.com") ||
        url.getHost == "pic.twitter.com"
      ))
}

/** Stats about links.  We keep a count here, which is redundant for
  * now, but convenient for the percentage and would let us spin
  * this off into its own service.
  *
  * A SpaceSaver can't be empty.  It has a Semigroup.  We regain a
  * Monoid by making it an Option.
  */
final case class LinkStats(
                            count: Long,
                            hasUrl: Long,
                            hasImageLink: Long,
                            topDomains: Option[SpaceSaver[String]]
                          ) {
  def percentWithUrl: Double = {
    if (count > 0L) 100.0 * hasUrl.toDouble / count.toDouble
    else 0L
  }

  def percentWithImageLink: Double = {
    if (count > 0L) 100.0 * hasImageLink.toDouble / count.toDouble
    else 0L
  }
}

object LinkStats {
  val zero = LinkStats(0L, 0L, 0L, None)

  implicit val linkStatsInstances: Monoid[LinkStats] with Eq[LinkStats] = new Monoid[LinkStats] with Eq[LinkStats] {
    def empty: LinkStats = zero

    def combine(x: LinkStats, y: LinkStats): LinkStats = LinkStats(
      count = x.count + y.count,
      hasUrl = x.hasUrl + y.hasUrl,
      hasImageLink = x.hasImageLink + y.hasImageLink,
      topDomains = x.topDomains |+| y.topDomains
    )

    def eqv(x: LinkStats, y: LinkStats): Boolean = {
      x.count === y.count &&
        x.hasUrl === y.hasUrl &&
        x.hasImageLink === y.hasImageLink &&
        (x.topDomains, y.topDomains).mapN((xtd, ytd) =>
          (xtd consistentWith ytd) && (ytd.consistentWith(xtd)))
          .getOrElse(x.topDomains.isEmpty && y.topDomains.isEmpty)
    }
  }
}

