package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.CombiningMatcher
import com.redpup.matchers.proto.CombiningMatcher.Combine
import com.redpup.matchers.proto.Matcher
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KCombiningMatcherTest {

  private val trueComponent = Matcher.newBuilder().setConstantMatcher(true).build()
  private val falseComponent = Matcher.newBuilder().setConstantMatcher(false).build()

  @Test
  fun `COMBINE_ANY evaluates like an OR logic configuration`() {
    val proto = Matcher.newBuilder().setCombiningMatcher(
      CombiningMatcher.newBuilder()
        .setCombine(Combine.COMBINE_ANY)
        .addAllMatchers(listOf(trueComponent, falseComponent))
    ).build()

    assertTrue(KMatcher.compile<String>(proto).match("arbitrary"))
  }

  @Test
  fun `COMBINE_ALL evaluates like an AND logic configuration`() {
    val allTrueProto = Matcher.newBuilder().setCombiningMatcher(
      CombiningMatcher.newBuilder()
        .setCombine(Combine.COMBINE_ALL)
        .addAllMatchers(listOf(trueComponent, trueComponent))
    ).build()

    val mixedProto = Matcher.newBuilder().setCombiningMatcher(
      CombiningMatcher.newBuilder()
        .setCombine(Combine.COMBINE_ALL)
        .addAllMatchers(listOf(trueComponent, falseComponent))
    ).build()

    assertTrue(KMatcher.compile<String>(allTrueProto).match("arbitrary"))
    assertFalse(KMatcher.compile<String>(mixedProto).match("arbitrary"))
  }

  @Test
  fun `COMBINE_NONE evaluates like a NOR logic configuration`() {
    val allFalseProto = Matcher.newBuilder().setCombiningMatcher(
      CombiningMatcher.newBuilder()
        .setCombine(Combine.COMBINE_NONE)
        .addAllMatchers(listOf(falseComponent, falseComponent))
    ).build()

    val mixedProto = Matcher.newBuilder().setCombiningMatcher(
      CombiningMatcher.newBuilder()
        .setCombine(Combine.COMBINE_NONE)
        .addAllMatchers(listOf(trueComponent, falseComponent))
    ).build()

    assertTrue(KMatcher.compile<String>(allFalseProto).match("arbitrary"))
    assertFalse(KMatcher.compile<String>(mixedProto).match("arbitrary"))
  }
}
