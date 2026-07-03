package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KConstantMatcherTest {

  @Test
  fun `ConstantMatcher maps directly to its boolean configuration state`() {
    val trueProto = Matcher.newBuilder().setConstantMatcher(true).build()
    val falseProto = Matcher.newBuilder().setConstantMatcher(false).build()

    val trueMatcher = KMatcher.compile<Any>(trueProto)
    val falseMatcher = KMatcher.compile<Any>(falseProto)

    assertTrue(trueMatcher.match("Any arbitrary input payload"))
    assertFalse(falseMatcher.match(12345))
  }
}
