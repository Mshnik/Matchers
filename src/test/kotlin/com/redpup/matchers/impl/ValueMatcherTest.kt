package com.redpup.matchers.impl

import com.redpup.proto.TestEnum
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueMatcher as ProtoValueMatcher
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValueMatcherTest {

  @Test
  fun `Int32ValueMatcher evaluates variations accurately`() {
    val proto = Matcher.newBuilder()
      .setValueMatcher(ProtoValueMatcher.newBuilder().setInt32Value(50))
      .build()
    val matcher = KMatcher.compile(proto)

    assertTrue(matcher.match(50))
    assertFalse(matcher.match(25))
  }

  @Test
  fun `StringValueMatcher evaluates variations accurately`() {
    val proto = Matcher.newBuilder()
      .setValueMatcher(ProtoValueMatcher.newBuilder().setStringValue("target"))
      .build()
    val matcher = KMatcher.compile(proto)

    assertTrue(matcher.match("target"))
    assertFalse(matcher.match("different"))
  }

  @Test
  fun `BoolValueMatcher evaluates variations accurately`() {
    val proto = Matcher.newBuilder()
      .setValueMatcher(ProtoValueMatcher.newBuilder().setBoolValue(true))
      .build()
    val matcher = KMatcher.compile(proto)

    assertTrue(matcher.match(true))
    assertFalse(matcher.match(false))
  }

  @Test
  fun `EnumValueMatcher extracts numbers using real TestEnum instances`() {
    // Configure the value matcher to look for the enum variant with number 2 (TEST_ENUM_2)
    val proto = Matcher.newBuilder()
      .setValueMatcher(ProtoValueMatcher.newBuilder().setEnumValue(2))
      .build()
    val matcher = KMatcher.compile(proto)

    assertTrue(matcher.match(TestEnum.TEST_ENUM_2))
    assertFalse(matcher.match(TestEnum.TEST_ENUM_1))
  }
}
