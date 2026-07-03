package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.StringMatcher as ProtoStr
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StringMatcherTest {

  @Test
  fun `ValueStringMatcher respects case sensitivity rules`() {
    val sensitiveProto = Matcher.newBuilder().setStringMatcher(
      ProtoStr.newBuilder().setValue("Kotlin").setCaseSensitive(ProtoStr.CaseSensitivity.CASE_SENSITIVE)
    ).build()

    val insensitiveProto = Matcher.newBuilder().setStringMatcher(
      ProtoStr.newBuilder().setValue("Kotlin").setCaseSensitive(ProtoStr.CaseSensitivity.CASE_INSENSITIVE)
    ).build()

    assertTrue(KMatcher.compile<String>(sensitiveProto).match("Kotlin"))
    assertFalse(KMatcher.compile<String>(sensitiveProto).match("kotlin"))

    assertTrue(KMatcher.compile<String>(insensitiveProto).match("kotlin"))
    assertTrue(KMatcher.compile<String>(insensitiveProto).match("KOTLIN"))
  }

  @Test
  fun `StartsWithStringMatcher asserts conditions based on current inversion design`() {
    // Adhering to implementation target: protoValue.startsWith(runtimeInput)
    val proto = Matcher.newBuilder().setStringMatcher(
      ProtoStr.newBuilder().setStartsWith("Framework").setCaseSensitive(ProtoStr.CaseSensitivity.CASE_SENSITIVE)
    ).build()
    val matcher = KMatcher.compile<String>(proto)

    assertTrue(matcher.match("Frame"))
    assertFalse(matcher.match("Work"))
  }

  @Test
  fun `PatternStringMatcher matches standard regular expression definitions`() {
    val proto = Matcher.newBuilder().setStringMatcher(
      ProtoStr.newBuilder().setPattern("^[A-Z]+$")
    ).build()
    val matcher = KMatcher.compile<String>(proto)

    assertTrue(matcher.match("ABC"))
    assertFalse(matcher.match("abc"))
  }
}
