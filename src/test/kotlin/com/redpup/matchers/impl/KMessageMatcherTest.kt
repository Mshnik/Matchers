package com.redpup.matchers.impl

import com.google.common.truth.Truth.assertThat
import com.redpup.matchers.proto.MessageMatcherKt.fieldMatcher
import com.redpup.matchers.proto.collectionMatcher
import com.redpup.matchers.proto.matcher
import com.redpup.matchers.proto.messageMatcher
import com.redpup.matchers.proto.valueMatcher
import com.redpup.matchers.testing.proto.TestMessage
import com.redpup.matchers.testing.proto.testMessage
import org.junit.jupiter.api.Test

class KMessageMatcherTest {

  @Test
  fun `SINGLE_FIELD matches valid singular field properties`() {
    val proto = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.matchers.testing.TestMessage"
        fields += fieldMatcher {
          fieldNumber = 2 // int32_value
          matcher = matcher {
            valueMatcher = valueMatcher {
              int32Value = 42
            }
          }
        }
      }
    }

    val matcher = KMessageMatcher.compile<TestMessage>(proto)

    assertThat(matcher.match(testMessage { int32Value = 42 })).isTrue()
    assertThat(matcher.match(testMessage { int32Value = 100 })).isFalse()
  }

  @Test
  fun `REPEATED_FIELD matches if at least one list item passes the condition`() {
    val proto = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.matchers.testing.TestMessage"
        fields += fieldMatcher {
          fieldName = "int32_values"
          matcher = matcher {
            collectionMatcher = collectionMatcher {
              any = matcher {
                valueMatcher = valueMatcher {
                  int32Value = 10
                }
              }
            }
          }
        }
      }
    }

    val matcher = KMessageMatcher.compile<TestMessage>(proto)

    // Contains 10 alongside other values -> true
    assertThat(matcher.match(testMessage { int32Values += listOf(5, 10, 15) })).isTrue()
    // Devoid of 10 -> false
    assertThat(matcher.match(testMessage { int32Values += listOf(1, 2, 3) })).isFalse()
    // Empty lists -> false
    assertThat(matcher.match(TestMessage.getDefaultInstance())).isFalse()
  }
}