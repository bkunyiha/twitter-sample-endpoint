package com.banno.twittersampleendpoint.service

/*import org.specs2.mutable.Specification

import cats.Monoid
import cats.kernel.laws.discipline.MonoidTests*/
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import com.banno.twittersampleendpoint.UnitTestFixture._


class CounterSpec extends Specification with ScalaCheck with Discipline {

  "averageFrom" >> {
    "is zero over zero interval" >> prop { (counter: Counter, time: Long) =>
      counter.averageFrom(time, time) must_=== Average(0.0)
    }
  }
}
