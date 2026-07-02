package com.redpup.matchers.impl

import com.redpup.proto.TestEnum
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueInSetMatcher as ProtoInSet
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValueInSetMatcherTest {

  @Test
  fun `Int32ValueInSetMatcher evaluates set membership accurately`() {
    val intSet = ProtoInSet.Int32ValueSet.newBuilder().addAllValues(listOf(10, 20, 30)).build()
    val proto = Matcher.newBuilder()
      .setValueInSetMatcher(ProtoInSet.newBuilder().setInt32Values(intSet))
      .build()
    val matcher = KMatcher.compile(proto)

    assertTrue(matcher.match(20))
    assertFalse(matcher.match(15))
  }

  @Test
  fun `StringValueInSetMatcher evaluates set membership accurately`() {
    val stringSet = ProtoInSet.StringValueSet.newBuilder().addAllValues(listOf("A", "B")).build()
    val proto = Matcher.newBuilder()
      .setValueInSetMatcher(ProtoInSet.newBuilder().setStringValues(stringSet))
      .build()
    val matcher = KMatcher.compile(proto)

    assertTrue(matcher.match("B"))
    assertFalse(matcher.match("C"))
  }

  @Test
  fun `EnumValueInSetMatcher maps and evaluates set membership for TestEnum`() {
    // Map the int32 internal backing set to evaluate enum numbers
    val enumSet = ProtoInSet.Int32ValueSet.newBuilder().addAllValues(listOf(1, 2)).build()
    val proto = Matcher.newBuilder()
      .setValueInSetMatcher(ProtoInSet.newBuilder().setEnumValues(enumSet))
      .build()
    val matcher = KMatcher.compile(proto)

    assertTrue(matcher.match(TestEnum.TEST_ENUM_1))
    assertTrue(matcher.match(TestEnum.TEST_ENUM_2))
    assertFalse(matcher.match(TestEnum.TEST_ENUM_DEFAULT))
  }
}
