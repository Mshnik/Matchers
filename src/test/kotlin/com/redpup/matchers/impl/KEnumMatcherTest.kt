package com.redpup.matchers.impl

import com.google.common.truth.Truth.assertThat
import com.redpup.matchers.proto.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KEnumMatcherTest {

  // Native Kotlin enum used for testing
  enum class Direction {
    NORTH, SOUTH, EAST, WEST
  }

  @Test
  fun `matches enum by ordinal number successfully`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        // Matcher targets an ordinal index equal to SOUTH (1)
        numberMatcher = matcher {
          valueMatcher = valueMatcher { int32Value = 1 }
        }
      }
    }

    val matcher = KEnumMatcher.compile<Direction>(proto)

    assertThat(matcher.match(Direction.SOUTH)).isTrue()
    assertThat(matcher.match(Direction.NORTH)).isFalse()
  }

  @Test
  fun `matches enum by ordinal comparison successfully`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        // Matcher targets an ordinal index greater than NORTH (0)
        numberMatcher = matcher {
          comparisonMatcher = comparisonMatcher {
            comparison = ComparisonMatcher.Comparison.COMPARISON_GT
            int32Value = 0
          }
        }
      }
    }

    val matcher = KEnumMatcher.compile<Direction>(proto)

    assertThat(matcher.match(Direction.SOUTH)).isTrue() // ordinal 1 > 0
    assertThat(matcher.match(Direction.NORTH)).isFalse() // ordinal 0 not > 0
  }

  // ============================================================================
  // 2. Name Matcher Strategy Tests
  // ============================================================================

  @Test
  fun `matches enum by exact name successfully`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        nameMatcher = matcher {
          valueMatcher = valueMatcher { stringValue = "EAST" }
        }
      }
    }

    val matcher = KEnumMatcher.compile<Direction>(proto)

    assertThat(matcher.match(Direction.EAST)).isTrue()
    assertThat(matcher.match(Direction.WEST)).isFalse()
  }

  @Test
  fun `matches enum by name string pattern operations successfully`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        nameMatcher = matcher {
          stringMatcher = stringMatcher {
            startsWith = "SOU"
          }
        }
      }
    }

    val matcher = KEnumMatcher.compile<Direction>(proto)

    assertThat(matcher.match(Direction.SOUTH)).isTrue()
    assertThat(matcher.match(Direction.NORTH)).isFalse()
  }

  // ============================================================================
  // 3. Type Name Validation Constraints
  // ============================================================================

  @Test
  fun `matches successfully when explicit enum type name matches`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        enumTypeName = Direction::class.java.canonicalName
        nameMatcher = matcher {
          valueMatcher = valueMatcher { stringValue = "NORTH" }
        }
      }
    }

    val matcher = KEnumMatcher.compile<Direction>(proto)
    assertThat(matcher.match(Direction.NORTH)).isTrue()
  }

  @Test
  fun `fails matching when explicit enum type name mismatches`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        enumTypeName = "com.different.package.FakeEnum"
        nameMatcher = matcher {
          valueMatcher = valueMatcher { stringValue = "NORTH" }
        }
      }
    }

    val matcher = KEnumMatcher.compile<Direction>(proto)

    // The name matches, but the structural enum type constraint does not
    assertThat(matcher.match(Direction.NORTH)).isFalse()
  }

  // ============================================================================
  // 4. Invalid Inputs and Compilation Failures
  // ============================================================================

  @Test
  fun `throws IllegalArgumentException if non-enum value is evaluated at runtime`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        nameMatcher = matcher {
          valueMatcher = valueMatcher { stringValue = "NORTH" }
        }
      }
    }

    val matcher = KEnumMatcher.compile<Any>(proto, Any::class)

    assertThrows<IllegalArgumentException> {
      matcher.match("JUST_A_STRING")
    }
  }

  @Test
  fun `compile fails fast if neither number nor name matcher is provided`() {
    val proto = matcher {
      enumMatcher = enumMatcher {
        enumTypeName = "com.redpup.matchers.impl.KEnumMatcherTest.Direction"
      }
    }

    val exception = assertThrows<IllegalArgumentException> {
      KEnumMatcher.compile<Direction>(proto)
    }
    assertThat(exception).hasMessageThat()
      .contains("must configure either a number_matcher or a name_matcher")
  }

  @Test
  fun `compile fails fast if outer matcher is missing enum matcher block`() {
    val proto = matcher {
      constantMatcher = true
    }

    val exception = assertThrows<IllegalArgumentException> {
      KEnumMatcher.compile<Direction>(proto)
    }
    assertThat(exception).hasMessageThat().contains("does not contain an enum_matcher block")
  }
}
