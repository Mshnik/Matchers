package com.redpup.matchers.impl

import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.MessageMatcher
import com.redpup.matchers.proto.MessageMatcher.FieldMatcher.FieldMatchType
import com.redpup.matchers.proto.ValueMatcher
import com.redpup.matchers.testing.proto.TestMessage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KMessageMatcherTest {

  @Test
  fun `SINGLE_FIELD matches valid singular field properties`() {
    val innerValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setInt32Value(42))
      .build()

    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldNumber(2) // int32_value
      .setMatchType(FieldMatchType.SINGLE_FIELD)
      .setMatcher(innerValue)
      .build()

    val proto = Matcher.newBuilder()
      .setMessageMatcher(
        MessageMatcher.newBuilder()
          .setMessageName("com.redpup.TestMessage")
          .addFields(fieldMatcher)
      ).build()

    val matcher = KMessageMatcher.compile<TestMessage>(proto)

    assertTrue(matcher.match(TestMessage.newBuilder().setInt32Value(42).build()))
    assertFalse(matcher.match(TestMessage.newBuilder().setInt32Value(100).build()))
  }

  @Test
  fun `SINGLE_FIELD throws IllegalStateException if assigned against repeated proto properties`() {
    val innerValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setInt32Value(42))
      .build()

    val invalidFieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldNumber(8) // int32_values
      .setMatchType(FieldMatchType.SINGLE_FIELD)
      .setMatcher(innerValue)
      .build()

    val proto = Matcher.newBuilder()
      .setMessageMatcher(
        MessageMatcher.newBuilder()
          .setMessageName("com.redpup.TestMessage")
          .addFields(invalidFieldMatcher)
      ).build()

    assertThrows<IllegalStateException> {
      KMessageMatcher.compile<TestMessage>(proto)
    }
  }

  @Test
  fun `REPEATED_FIELD_ANY matches if at least one list item passes the condition`() {
    val innerValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setInt32Value(10))
      .build()

    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldName("int32_values")
      .setMatchType(FieldMatchType.REPEATED_FIELD_ANY)
      .setMatcher(innerValue)
      .build()

    val proto = Matcher.newBuilder()
      .setMessageMatcher(MessageMatcher.newBuilder().addFields(fieldMatcher))
      .build()

    val matcher = KMessageMatcher.compile<TestMessage>(proto)

    // Contains 10 alongside other values -> true
    assertTrue(
      matcher.match(
        TestMessage.newBuilder().addAllInt32Values(listOf(5, 10, 15)).build()
      )
    )
    // Devoid of 10 -> false
    assertFalse(
      matcher.match(
        TestMessage.newBuilder().addAllInt32Values(listOf(1, 2, 3)).build()
      )
    )
    // Empty lists -> false
    assertFalse(matcher.match(TestMessage.getDefaultInstance()))
  }

  @Test
  fun `REPEATED_FIELD_ALL matches only if all list items pass the condition`() {
    val innerValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setInt32Value(7))
      .build()

    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldName("int32_values")
      .setMatchType(FieldMatchType.REPEATED_FIELD_ALL)
      .setMatcher(innerValue)
      .build()

    val proto = Matcher.newBuilder()
      .setMessageMatcher(MessageMatcher.newBuilder().addFields(fieldMatcher))
      .build()

    val matcher = KMessageMatcher.compile<TestMessage>(proto)

    // Every item is 7 -> true
    assertTrue(
      matcher.match(
        TestMessage.newBuilder().addAllInt32Values(listOf(7, 7)).build()
      )
    )
    // One element is divergent -> false
    assertFalse(
      matcher.match(
        TestMessage.newBuilder().addAllInt32Values(listOf(7, 9, 7)).build()
      )
    )
    // Empty collections vacuously evaluate to true under all criteria operations
    assertTrue(matcher.match(TestMessage.getDefaultInstance()))
  }

  @Test
  fun `REPEATED_FIELD_NONE matches only if no list items pass the condition`() {
    val innerValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setInt32Value(100))
      .build()

    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldName("int32_values")
      .setMatchType(FieldMatchType.REPEATED_FIELD_NONE)
      .setMatcher(innerValue)
      .build()

    val proto = Matcher.newBuilder()
      .setMessageMatcher(MessageMatcher.newBuilder().addFields(fieldMatcher))
      .build()

    val matcher = KMessageMatcher.compile<TestMessage>(proto)

    // Elements exclude 100 -> true
    assertTrue(
      matcher.match(
        TestMessage.newBuilder().addAllInt32Values(listOf(1, 2, 3)).build()
      )
    )
    // Collection includes 100 -> false
    assertFalse(
      matcher.match(
        TestMessage.newBuilder().addAllInt32Values(listOf(50, 100)).build()
      )
    )
    // Empty list -> true
    assertTrue(matcher.match(TestMessage.getDefaultInstance()))
  }

  @Test
  fun `REPEATED_FIELD criteria initialization throws IllegalStateException against non-repeated properties`() {
    val innerValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setInt32Value(5))
      .build()

    val invalidFieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldNumber(2) // int32_value (Singular!)
      .setMatchType(FieldMatchType.REPEATED_FIELD_ANY)
      .setMatcher(innerValue)
      .build()

    val proto = Matcher.newBuilder()
      .setMessageMatcher(MessageMatcher.newBuilder().addFields(invalidFieldMatcher))
      .build()

    assertThrows<IllegalStateException> {
      KMessageMatcher.compile<TestMessage>(proto)
    }
  }

  @Test
  fun `KMessageMatcher execution fails fast on unset matchType strategies`() {
    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldNumber(2)
      .setMatchType(FieldMatchType.FIELD_MATCH_TYPE_UNSET) // Unset strategy boundary
      .setMatcher(Matcher.newBuilder().setConstantMatcher(true))
      .build()

    val proto = Matcher.newBuilder()
      .setMessageMatcher(MessageMatcher.newBuilder().addFields(fieldMatcher))
      .build()

    assertThrows<IllegalArgumentException> {
      KMessageMatcher.compile<TestMessage>(proto)
    }
  }
}
