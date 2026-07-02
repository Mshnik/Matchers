package com.redpup.com.redpup.matchers.impl

import com.redpup.com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher

/** The compiled implementation of [Matcher.getConstantMatcher]. */
class ConstantMatcher(private val matcher: Matcher) : KMatcher<Any>(Any::class, matcher) {
  override fun matchTyped(value: Any): Boolean = matcher.constantMatcher
}
