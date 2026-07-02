package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher

/** The compiled implementation of [Matcher.getConstantMatcher]. */
class ConstantMatcher(proto: Matcher) : KMatcher<Any>(Any::class, proto) {
  override fun matchTyped(value: Any): Boolean = proto.constantMatcher
}
