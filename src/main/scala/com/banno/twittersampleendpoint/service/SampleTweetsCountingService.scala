package com.banno.twittersampleendpoint.service

import cats.{Eq, Monoid}
import scala.concurrent.duration._

object SampleTweetsCountingService {
  def apply(): Counter = Counter(count = 1)
}

final case class Counter(count: Long) {

  def averageFrom(start: Long, end: Long): Average = {
    val startDbl = start.toDouble
    val endDbl = end.toDouble
    val elapsed = endDbl - startDbl

    Average(rateMean = if (endDbl - startDbl == 0.0) 0.0 else count.toDouble / elapsed)
  }
}

object Counter {
  val OneMinuteMillis = 1.minute.toMillis.toDouble
  val FiveMinuteMillis = 5.minutes.toMillis.toDouble
  val FifteenMinuteMillis = 15.minutes.toMillis.toDouble

  val zero = Counter(count = 0L)

  implicit val counterInstances: Monoid[Counter] with Eq[Counter] = new Monoid[Counter] with Eq[Counter] {
    val empty = Counter.zero

    def combine(x: Counter, y: Counter): Counter = Counter(count = x.count + y.count)

    def eqv(x: Counter, y: Counter) = x == y
  }
}

final case class Average(rateMean: Double)


