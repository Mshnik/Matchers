package com.redpup.matchers.impl

import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.MessageMatcher
import com.redpup.matchers.proto.ValueMatcher
import com.redpup.proto.TestEnum
import com.redpup.proto.TestMessage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KMessageMatcherTest {

  @Test
  fun `MessageMatcher matches when a field selected by number passes target constraint`() {
    val int32TargetValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setInt32Value(42))
      .build()

    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldNumber(2)
      .setMatcher(int32TargetValue)
      .build()

    val messageMatcherProto = Matcher.newBuilder()
      .setMessageMatcher(
        MessageMatcher.newBuilder()
          .setMessageName(TestMessage.getDescriptor().fullName)
          .addFields(fieldMatcher)
      ).build()

    val compiledMatcher = KMessageMatcher.compile<TestMessage>(messageMatcherProto)

    val matchingMessage = TestMessage.newBuilder().setInt32Value(42).build()
    val failingMessage = TestMessage.newBuilder().setInt32Value(99).build()

    assertTrue(compiledMatcher.match(matchingMessage))
    assertFalse(compiledMatcher.match(failingMessage))
  }

  @Test
  fun `MessageMatcher matches when a field selected by name passes target constraint`() {
    val stringTargetValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setStringValue("Kotlin"))
      .build()

    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldName("string_value")
      .setMatcher(stringTargetValue)
      .build()

    val messageMatcherProto = Matcher.newBuilder()
      .setMessageMatcher(
        MessageMatcher.newBuilder()
          .setMessageName(TestMessage.getDescriptor().fullName)
          .addFields(fieldMatcher)
      ).build()

    val compiledMatcher = KMessageMatcher.compile<TestMessage>(messageMatcherProto)

    val matchingMessage = TestMessage.newBuilder().setStringValue("Kotlin").build()
    val failingMessage = TestMessage.newBuilder().setStringValue("Java").build()

    assertTrue(compiledMatcher.match(matchingMessage))
    assertFalse(compiledMatcher.match(failingMessage))
  }

  @Test
  fun `MessageMatcher combines multiple fields with AND logic functionality`() {
    val intMatcher =
      Matcher.newBuilder().setValueMatcher(ValueMatcher.newBuilder().setInt32Value(100))
        .build()
    val field1 =
      MessageMatcher.FieldMatcher.newBuilder().setFieldNumber(2).setMatcher(intMatcher).build()

    val stringMatcher =
      Matcher.newBuilder().setValueMatcher(ValueMatcher.newBuilder().setStringValue("Valid"))
        .build()
    val field2 = MessageMatcher.FieldMatcher.newBuilder().setFieldName("string_value")
      .setMatcher(stringMatcher).build()

    val messageMatcherProto = Matcher.newBuilder()
      .setMessageMatcher(
        MessageMatcher.newBuilder()
          .setMessageName(TestMessage.getDescriptor().fullName)
          .addFields(field1)
          .addFields(field2)
      ).build()

    val compiledMatcher = KMessageMatcher.compile<TestMessage>(messageMatcherProto)

    val fullyMatchingMessage =
      TestMessage.newBuilder().setInt32Value(100).setStringValue("Valid").build()
    val partiallyMatchingMessage =
      TestMessage.newBuilder().setInt32Value(100).setStringValue("Invalid").build()
    val completelyFailingMessage =
      TestMessage.newBuilder().setInt32Value(50).setStringValue("Invalid").build()

    assertTrue(compiledMatcher.match(fullyMatchingMessage))
    assertFalse(compiledMatcher.match(partiallyMatchingMessage))
    assertFalse(compiledMatcher.match(completelyFailingMessage))
  }

  @Test
  fun `MessageMatcher successfully resolves enum fields and tests boundaries`() {
    val enumTargetValue = Matcher.newBuilder()
      .setValueMatcher(ValueMatcher.newBuilder().setEnumValue(TestEnum.TEST_ENUM_1_VALUE))
      .build()

    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setFieldName("enum_value")
      .setMatcher(enumTargetValue)
      .build()

    val messageMatcherProto = Matcher.newBuilder()
      .setMessageMatcher(
        MessageMatcher.newBuilder()
          .setMessageName(TestMessage.getDescriptor().fullName)
          .addFields(fieldMatcher)
      ).build()

    val compiledMatcher = KMessageMatcher.compile<TestMessage>(messageMatcherProto)

    val matchingMessage = TestMessage.newBuilder().setEnumValue(TestEnum.TEST_ENUM_1).build()
    val failingMessage = TestMessage.newBuilder().setEnumValue(TestEnum.TEST_ENUM_2).build()

    assertTrue(compiledMatcher.match(matchingMessage))
    assertFalse(compiledMatcher.match(failingMessage))
  }

  @Test
  fun `MessageMatcher throws IllegalArgumentException if field properties are fully omitted`() {
    val fieldMatcher = MessageMatcher.FieldMatcher.newBuilder()
      .setMatcher(Matcher.newBuilder().setConstantMatcher(true))
      .build()

    val invalidMessageMatcherProto = Matcher.newBuilder()
      .setMessageMatcher(
        MessageMatcher.newBuilder()
          .setMessageName(TestMessage.getDescriptor().fullName)
          .addFields(fieldMatcher)
      ).build()

    assertThrows<IllegalArgumentException> {
      KMessageMatcher.compile<TestMessage>(invalidMessageMatcherProto)
    }
  }
}