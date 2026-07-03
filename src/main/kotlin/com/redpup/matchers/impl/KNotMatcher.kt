package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import kotlin.reflect.KClass

/** The implementation for [com.redpup.matchers.proto.Matcher.getNotMatcher]. */
internal class KNotMatcher<in T : Any>(proto: Matcher, expectedClass: KClass<T>) :
  KMatcher<T>(expectedClass, proto) {
  private val matcher = compile(proto.notMatcher, expectedClass)

  init {
    check(proto.hasNotMatcher()) { "Expected NotMatcher, found $proto" }
  }

  override fun matchTyped(value: T): Boolean = !matcher.matchTyped(value)
}