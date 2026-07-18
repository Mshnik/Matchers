package com.redpup.matchers

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Empty
import com.redpup.matchers.proto.*
import com.redpup.matchers.proto.CollectionMatcher.DistinctElementsMatcher.MatchType
import com.redpup.matchers.proto.CollectionMatcherKt.distinctElementsMatcher
import com.redpup.matchers.proto.CombiningMatcher.Combine
import com.redpup.matchers.proto.ComparisonMatcher.Comparison
import com.redpup.matchers.proto.MessageMatcherKt.fieldMatcher
import com.redpup.matchers.proto.StringMatcher.CaseSensitivity
import com.redpup.matchers.proto.ValueInSetMatcherKt.int32ValueSet
import com.redpup.matchers.proto.ValueInSetMatcherKt.stringValueSet
import com.redpup.matchers.testing.proto.TestMessage
import org.junit.Test

class MatcherDslTest {

  @Test
  fun testStandaloneTopLevelStringMatchers() {
    // 1. Built using the custom Type-Safe Domain DSL
    val dslBuilt = typedMatcher<String> {
      textEquals("hello_world", CaseSensitivity.CASE_SENSITIVE)
    }

    // 2. Built using standard vanilla Kotlin Proto DSL
    val nativeProtoBuilt = matcher {
      stringMatcher = stringMatcher {
        caseSensitive = CaseSensitivity.CASE_SENSITIVE
        value = "hello_world"
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)
  }

  @Test
  fun testStandaloneRelationalComparisons() {
    // Validate primitive bounds checks via custom DSL
    val dslBuilt = typedMatcher<Int> {
      greaterThan(100)
    }

    // Verify parity against pure Proto configuration
    val nativeProtoBuilt = matcher {
      comparisonMatcher = comparisonMatcher {
        comparison = Comparison.COMPARISON_GT
        int32Value = 100
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)

    // Validate string bounds checks via custom DSL
    val stringDslBuilt = typedMatcher<String> {
      lessThanOrEqual("beta")
    }

    val stringNativeBuilt = matcher {
      comparisonMatcher = comparisonMatcher {
        comparison = Comparison.COMPARISON_LE
        stringValue = "beta"
      }
    }

    assertThat(stringDslBuilt).isEqualTo(stringNativeBuilt)
  }

  @Test
  fun testCombinerLogicGates() {
    val dslBuilt = typedMatcher<Int> {
      anyOf {
        matches { value(10) }
        matches { anyOf(20, 30) }
      }
    }

    val nativeProtoBuilt = matcher {
      combiningMatcher = combiningMatcher {
        combine = Combine.COMBINE_ANY
        matchers += matcher { valueMatcher = valueMatcher { int32Value = 10 } }
        matchers += matcher {
          valueInSetMatcher = valueInSetMatcher {
            int32Values = int32ValueSet { values += listOf(20, 30) }
          }
        }
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)
  }

  @Test
  fun testComplexMessageTreeStructure() {
    // Verified using your updated untypedMessageMatcher mapping engine entrypoint
    val dslBuilt = matcher {
      messageMatcher(TestMessage.getDescriptor()) {
        "string_value" value "active"
        "int32_value".matches<Int> {
          not { value(0) }
        }
        "string_values" matchesCollection {
          containsDistinct(MatchType.MATCH_TYPE_EXACT) {
            matches { startsWith("prefix_") }
            matches { anyOf("match_a", "match_b") }
          }
        }
      }
    }

    val nativeProtoBuilt = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.matchers.testing.TestMessage"

        fields += fieldMatcher {
          fieldNumber = 6
          fieldName = "string_value"
          matcher = matcher { valueMatcher = valueMatcher { stringValue = "active" } }
        }

        fields += fieldMatcher {
          fieldNumber = 2
          fieldName = "int32_value"
          matcher = matcher {
            notMatcher = matcher { valueMatcher = valueMatcher { int32Value = 0 } }
          }
        }

        fields += fieldMatcher {
          fieldNumber = 24
          fieldName = "string_values"
          matcher = matcher {
            collectionMatcher = collectionMatcher {
              containsElements = distinctElementsMatcher {
                matchType = MatchType.MATCH_TYPE_EXACT
                matchers += matcher { stringMatcher = stringMatcher { startsWith = "prefix_" } }
                matchers += matcher {
                  valueInSetMatcher = valueInSetMatcher {
                    stringValues = stringValueSet { values += listOf("match_a", "match_b") }
                  }
                }
              }
            }
          }
        }
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)
  }

  @Test
  fun testRelationalComparisonsInMessageTree() {
    val dslBuilt = matcher {
      messageMatcher(TestMessage.getDescriptor()) {
        "int32_value".matches<Int> {
          lessThan(50)
        }
      }
    }

    val nativeProtoBuilt = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.matchers.testing.TestMessage"

        fields += fieldMatcher {
          fieldNumber = 2
          fieldName = "int32_value"
          matcher = matcher {
            comparisonMatcher = comparisonMatcher {
              comparison = Comparison.COMPARISON_LT
              int32Value = 50
            }
          }
        }
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)
  }

  @Test
  fun testNestedMessageMatchers() {
    val dslBuilt = matcher {
      messageMatcher(TestMessage.getDescriptor()) {
        "message_value" matchesMessage {
          "bool_value" value true
        }
      }
    }

    val nativeProtoBuilt = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.matchers.testing.TestMessage"

        fields += fieldMatcher {
          fieldNumber = 8
          fieldName = "message_value"
          matcher = matcher {
            messageMatcher = messageMatcher {
              messageTypeName = "com.redpup.matchers.testing.TestMessage"
              fields += fieldMatcher {
                fieldNumber = 1
                fieldName = "bool_value"
                matcher = matcher { valueMatcher = valueMatcher { boolValue = true } }
              }
            }
          }
        }
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)
  }

  @Test
  fun testTypedMessageMatcherEntryPoint() {
    // Verifies the TypedMatcherBuilder context receiver wrapper variant
    val dslBuilt = typedMatcher<TestMessage> {
      typedMessageMatcher(TestMessage.getDescriptor()) {
        "int32_value" value 42
      }
    }

    val nativeProtoBuilt = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.matchers.testing.TestMessage"
        fields += fieldMatcher {
          fieldNumber = 2
          fieldName = "int32_value"
          matcher = matcher { valueMatcher = valueMatcher { int32Value = 42 } }
        }
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)
  }

  @Test
  fun testCollectionIsEmptyValidation() {
    val dslBuilt = typedMatcher<Collection<Int>> {
      collectionMatcher {
        isEmpty()
      }
    }

    val nativeProtoBuilt = matcher {
      collectionMatcher = collectionMatcher {
        empty = Empty.getDefaultInstance()
      }
    }

    assertThat(dslBuilt).isEqualTo(nativeProtoBuilt)
  }
}
