package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueInSetMatcher
import com.redpup.matchers.testing.proto.TestEnum
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KValueInSetMatcherTest {

  @Test
  fun `Int32ValueInSetMatcher evaluates set membership accurately`() {
    val intSet =
      ValueInSetMatcher.Int32ValueSet.newBuilder().addAllValues(listOf(10, 20, 30)).build()
    val proto = Matcher.newBuilder()
      .setValueInSetMatcher(ValueInSetMatcher.newBuilder().setInt32Values(intSet))
      .build()
    val matcher = KMatcher.compile<Int>(proto)

    assertTrue(matcher.match(20))
    assertFalse(matcher.match(15))
  }

  @Test
  fun `Int64ValueInSetMatcher evaluates set membership accurately`() {
    val longSet =
      ValueInSetMatcher.Int64ValueSet.newBuilder().addAllValues(listOf(10L, 20L, 30L)).build()
    val proto = Matcher.newBuilder()
      .setValueInSetMatcher(ValueInSetMatcher.newBuilder().setInt64Values(longSet))
      .build()
    val matcher = KMatcher.compile<Long>(proto)

    assertTrue(matcher.match(20L))
    assertFalse(matcher.match(15L))
  }

  @Test
  fun `StringValueInSetMatcher evaluates set membership accurately`() {
    val stringSet =
      ValueInSetMatcher.StringValueSet.newBuilder().addAllValues(listOf("A", "B")).build()
    val proto = Matcher.newBuilder()
      .setValueInSetMatcher(ValueInSetMatcher.newBuilder().setStringValues(stringSet))
      .build()
    val matcher = KMatcher.compile<String>(proto)

    assertTrue(matcher.match("B"))
    assertFalse(matcher.match("C"))
  }

  @Test
  fun `EnumValueInSetMatcher maps and evaluates set membership for TestEnum`() {
    // Map the int32 internal backing set to evaluate enum numbers
    val enumSet =
      ValueInSetMatcher.Int32ValueSet.newBuilder().addAllValues(listOf(1, 2)).build()
    val proto = Matcher.newBuilder()
      .setValueInSetMatcher(ValueInSetMatcher.newBuilder().setEnumValues(enumSet))
      .build()
    val matcher = KMatcher.compile<TestEnum>(proto)

    assertTrue(matcher.match(TestEnum.TEST_ENUM_1))
    assertTrue(matcher.match(TestEnum.TEST_ENUM_2))
    assertFalse(matcher.match(TestEnum.TEST_ENUM_DEFAULT))
  }
}
