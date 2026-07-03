package com.redpup.matchers

import com.google.common.truth.Truth.assertThat
import com.redpup.matchers.MatcherFactory.messageMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.MessageMatcher.FieldMatcher.FieldMatchType
import com.redpup.matchers.proto.MessageMatcherKt.fieldMatcher
import com.redpup.matchers.proto.matcher
import com.redpup.matchers.proto.messageMatcher
import com.redpup.matchers.proto.valueMatcher
import com.redpup.matchers.testing.proto.TestMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MatcherDslTest {
  companion object {
    private val descriptor = TestMessage.getDescriptor()
  }

  @Test
  fun `singleField creates identical structure to raw proto builder`() {
    val expected = matcher {
      messageMatcher = messageMatcher {
        messageName = "com.redpup.matchers.testing.TestMessage"
        fields += fieldMatcher {
          fieldNumber = 2
          fieldName = "int32_value"
          matchType = FieldMatchType.SINGLE_FIELD
          matcher = matcher {
            valueMatcher = valueMatcher {
              int32Value = 42
            }
          }
        }
      }
    }

    // 2. Build using your new Kotlin Custom DSL
    val actual = Matcher.newBuilder().messageMatcher(descriptor) {
      "int32_value".matches {
        valueMatcherBuilder.setInt32Value(42)
      }
    }.build()

    // 3. Structural assertion via Google Truth
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `repeatedField overloads map correctly across all distinct collection strategies`() {
    val expected = matcher {
      messageMatcher = messageMatcher {
        messageName = "com.redpup.matchers.testing.TestMessage"

        fields += fieldMatcher {
          fieldNumber = 8
          fieldName = "int32_values"
          matchType = FieldMatchType.REPEATED_FIELD_ANY
          matcher = matcher { constantMatcher = true }
        }
        fields += fieldMatcher {
          fieldNumber = 8
          fieldName = "int32_values"
          matchType = FieldMatchType.REPEATED_FIELD_ALL
          matcher = matcher { constantMatcher = true }
        }
        fields += fieldMatcher {
          fieldNumber = 8
          fieldName = "int32_values"
          matchType = FieldMatchType.REPEATED_FIELD_NONE
          matcher = matcher { constantMatcher = true }
        }
      }
    }

    val actual = Matcher.newBuilder().messageMatcher(descriptor) {
      // Test string lookup strategy
      "int32_values".any { constantMatcher = true }
      // Test integer number lookup strategy
      8.all { constantMatcher = true }
      // Test explicit FieldDescriptor lookup strategy
      descriptor.findFieldByNumber(8).none { constantMatcher = true }
    }.build()

    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `dsl factory object method builds freestanding message matchers cleanly`() {
    val expectedFreestanding = messageMatcher {
      messageName = "com.redpup.matchers.testing.TestMessage"
      fields += fieldMatcher {
        fieldNumber = 1
        fieldName = "bool_value"
        matchType = FieldMatchType.SINGLE_FIELD
        matcher = matcher { constantMatcher = false }
      }
    }

    val actualFreestanding = messageMatcher(descriptor) {
      1.matches { constantMatcher = false }
    }

    assertThat(actualFreestanding).isEqualTo(expectedFreestanding)
  }

  @Test
  fun `dsl validation triggers exception when unknown named properties are targeted`() {
    val exception = assertThrows<IllegalArgumentException> {
      messageMatcher(descriptor) {
        "non_existent_field".matches { constantMatcher = true }
      }
    }
    assertThat(exception).hasMessageThat().contains("Field 'non_existent_field' not found")
  }
}
