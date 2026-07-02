package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.CombiningMatcher.Combine
import com.redpup.matchers.proto.Matcher

/** The implementation for [com.redpup.matchers.proto.CombiningMatcher]. */
class CombiningMatcher(proto: Matcher) : KMatcher<Any>(Any::class, proto) {
  private val matchers = proto.combiningMatcher.matchersList.map { compile(it) }

  init {
    check(proto.hasCombiningMatcher()) { "Expected CombiningMatcher, found $proto" }
  }

  override fun matchTyped(value: Any): Boolean = when (proto.combiningMatcher.combine) {
    Combine.COMBINE_ANY -> matchers.any { it(value) }
    Combine.COMBINE_ALL -> matchers.all { it(value) }
    Combine.COMBINE_NONE -> matchers.none { it(value) }
    Combine.COMBINE_UNSET, Combine.UNRECOGNIZED -> throw IllegalArgumentException("Unsupported combine case: $proto")
    null -> throw NullPointerException()
  }
}