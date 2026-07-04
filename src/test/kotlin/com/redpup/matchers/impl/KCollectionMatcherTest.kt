package com.redpup.matchers.impl

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.empty
import com.redpup.matchers.proto.CollectionMatcherKt.distinctElementsMatcher
import com.redpup.matchers.proto.MessageMatcherKt.fieldMatcher
import com.redpup.matchers.proto.collectionMatcher
import com.redpup.matchers.proto.matcher
import com.redpup.matchers.proto.messageMatcher
import com.redpup.matchers.proto.valueMatcher
import com.redpup.matchers.testing.proto.TestMessage
import com.redpup.matchers.testing.proto.testMessage
import kotlin.reflect.typeOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KCollectionMatcherTest {

  @Test
  fun `ANY matches if at least one item satisfies the sub-matcher`() {
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        any = matcher {
          valueMatcher = valueMatcher {
            int32Value = 42
          }
        }
      }
    }

    val matcher =
      KCollectionMatcher.compile<Int, List<Int>>(proto, List::class, typeOf<List<Int>>())

    assertThat(matcher.matchTyped(listOf(1, 42, 3))).isTrue()
    assertThat(matcher.matchTyped(listOf(1, 2, 3))).isFalse()
    assertThat(matcher.matchTyped(emptyList())).isFalse()
  }

  @Test
  fun `ALL matches only if every single item satisfies the sub-matcher`() {
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        all = matcher {
          valueMatcher = valueMatcher {
            boolValue = true
          }
        }
      }
    }

    val matcher = KCollectionMatcher.compile<Boolean, List<Boolean>>(
      proto,
      List::class,
      typeOf<List<Boolean>>()
    )

    assertThat(matcher.matchTyped(listOf(true, true, true))).isTrue()
    assertThat(matcher.matchTyped(listOf(true, false, true))).isFalse()
    // Vacuously true for an empty collection under universal quantification
    assertThat(matcher.matchTyped(emptyList())).isTrue()
  }

  @Test
  fun `NONE matches only if no items satisfy the sub-matcher`() {
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        none = matcher {
          valueMatcher = valueMatcher {
            stringValue = "forbidden"
          }
        }
      }
    }

    val matcher =
      KCollectionMatcher.compile<String, Set<String>>(proto, Set::class, typeOf<Set<String>>())

    assertThat(matcher.matchTyped(setOf("allowed", "safe"))).isTrue()
    assertThat(matcher.matchTyped(setOf("allowed", "forbidden"))).isFalse()
    assertThat(matcher.matchTyped(emptySet())).isTrue()
  }

  @Test
  fun `EMPTY matches if the collection contains zero elements`() {
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        empty = empty {}
      }
    }

    val matcher =
      KCollectionMatcher.compile<Int, List<Int>>(proto, List::class, typeOf<List<Int>>())

    assertThat(matcher.matchTyped(emptyList())).isTrue()
    assertThat(matcher.matchTyped(listOf(1))).isFalse()
  }

  @Test
  fun `SIZE evaluates the collection count against a size sub-matcher`() {
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        size = matcher {
          valueMatcher = valueMatcher {
            int32Value = 3
          }
        }
      }
    }

    val matcher =
      KCollectionMatcher.compile<String, List<String>>(proto, List::class, typeOf<List<String>>())

    assertThat(matcher.matchTyped(listOf("a", "b", "c"))).isTrue()
    assertThat(matcher.matchTyped(listOf("a", "b"))).isFalse()
    assertThat(matcher.matchTyped(emptyList())).isFalse()
  }

  @Test
  fun `CONTAINS_ELEMENTS enforces 1-to-1 matching for multiple conditions using complex types`() {
    // We want a collection containing unique TestMessages where one has int32_value=10 and another has int32_value=20
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        containsElements = distinctElementsMatcher {
          matchers += matcher {
            valueMatcher = valueMatcher { int32Value = 10 }
          }
          matchers += matcher {
            valueMatcher = valueMatcher { int32Value = 20 }
          }
        }
      }
    }

    // Since our sub-matchers look for exact Ints, compile against a List of Ints for easy validation
    val matcher =
      KCollectionMatcher.compile<Int, List<Int>>(proto, List::class, typeOf<List<Int>>())

    // Perfect unique assignment
    assertThat(matcher.matchTyped(listOf(10, 20))).isTrue()
    // Trailing unmatched elements are fine
    assertThat(matcher.matchTyped(listOf(0, 20, 10))).isTrue()
    // Order-independence and backtracking verification
    assertThat(matcher.matchTyped(listOf(20, 10))).isTrue()

    // Fails because '10' cannot satisfy both requested matcher nodes uniquely
    assertThat(matcher.matchTyped(listOf(10, 0))).isFalse()
    // Fails because '20' is present but '10' is completely missing
    assertThat(matcher.matchTyped(listOf(20, 20))).isFalse()
  }

  @Test
  fun `CONTAINS_ELEMENTS short circuits immediately on fail-fast collection size check`() {
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        containsElements = distinctElementsMatcher {
          matchers += matcher { valueMatcher = valueMatcher { int32Value = 1 } }
          matchers += matcher { valueMatcher = valueMatcher { int32Value = 2 } }
          matchers += matcher { valueMatcher = valueMatcher { int32Value = 3 } }
        }
      }
    }

    val matcher =
      KCollectionMatcher.compile<Int, List<Int>>(proto, List::class, typeOf<List<Int>>())

    // Collection only has 2 elements but 3 are demanded.
    // Triggers size check before ever invoking expensive sub-matcher tracks.
    assertThat(matcher.matchTyped(listOf(1, 2))).isFalse()
  }

  @Test
  fun `CONTAINS_ELEMENTS works seamlessly against complex Message layers`() {
    val proto = matcher {
      collectionMatcher = collectionMatcher {
        containsElements = distinctElementsMatcher {
          matchers += matcher {
            messageMatcher = messageMatcher {
              messageName = TestMessage.getDescriptor().fullName
              fields += fieldMatcher {
                fieldNumber = TestMessage.STRING_VALUE_FIELD_NUMBER
                matcher = matcher {
                  valueMatcher = valueMatcher { stringValue = "target-a" }
                }
              }
            }
          }
        }
      }
    }

    val matcher =
      KCollectionMatcher.compile<TestMessage, List<TestMessage>>(
        proto,
        List::class,
        typeOf<List<TestMessage>>()
      )

    val messageList = listOf(
      testMessage { stringValue = "target-a"; int32Value = 123 },
      testMessage { stringValue = "unrelated-b" }
    )

    assertThat(matcher.matchTyped(messageList)).isTrue()
  }

  @Test
  fun `Compilation crashes cleanly if collection configuration details are missing`() {
    val invalidProto = matcher {
      // Intentionally omitting the inner oneof assignment strategy
      collectionMatcher = collectionMatcher {}
    }

    val exception = assertThrows<IllegalArgumentException> {
      KCollectionMatcher.compile<Int, List<Int>>(invalidProto, List::class, typeOf<List<Int>>())
    }
    assertThat(exception).hasMessageThat().contains("CollectionMatcher has no matcher set")
  }
}
