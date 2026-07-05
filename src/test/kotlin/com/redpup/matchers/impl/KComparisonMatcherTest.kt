package com.redpup.matchers.impl

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.redpup.matchers.proto.ComparisonMatcher.Comparison
import com.redpup.matchers.proto.ComparisonMatcherKt
import com.redpup.matchers.proto.comparisonMatcher
import com.redpup.matchers.proto.matcher
import com.redpup.matchers.testing.proto.TestEnum
import java.util.stream.Stream
import kotlin.reflect.KClass
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class KComparisonMatcherTest {

  data class ComparisonTestCase<T : Comparable<T>>(
    val type: KClass<T>,
    val comparison: Comparison,
    val expected: T,
    val valueToTest: T,
    val shouldMatch: Boolean,
    val protoSetupBlock: (ComparisonMatcherKt.Dsl) -> Unit,
  )

  @ParameterizedTest(name = "{0}")
  @MethodSource("comparisonMatrixProvider")
  fun <T : Comparable<T>> `evaluates comparison constraints accurately`(testCase: ComparisonTestCase<T>) {
    val proto = matcher {
      comparisonMatcher = comparisonMatcher {
        comparison = testCase.comparison
        testCase.protoSetupBlock(this)
      }
    }

    val matcher = KComparisonMatcher.compile(proto, testCase.type)

    val result = matcher.match(testCase.valueToTest)
    assertWithMessage("$proto Input=${testCase.valueToTest}").that(result).isEqualTo(testCase.shouldMatch)
  }

  @ParameterizedTest(name = "Unset/Invalid: {0}")
  @MethodSource("invalidComparisonProvider")
  fun `throws exception on invalid or unset operator permutations`(comparisonOp: Comparison) {
    val proto = matcher {
      comparisonMatcher = comparisonMatcher {
        comparison = comparisonOp
        int32Value = 42
      }
    }

    val matcher = KComparisonMatcher.compile(proto, Int::class)
    assertThrows<IllegalArgumentException> {
      matcher.match(42)
    }
  }

  companion object {
    @JvmStatic
    fun comparisonMatrixProvider(): Stream<Arguments> {
      val testCases = listOf(
        // --- Int32 Checks ---
        ComparisonTestCase(Int::class, Comparison.COMPARISON_EQ, 50, 50, true) {
          it.int32Value = 50
        },
        ComparisonTestCase(Int::class, Comparison.COMPARISON_EQ, 50, 25, false) {
          it.int32Value = 50
        },
        ComparisonTestCase(Int::class, Comparison.COMPARISON_NE, 50, 25, true) {
          it.int32Value = 50
        },
        ComparisonTestCase(Int::class, Comparison.COMPARISON_LT, 50, 25, true) {
          it.int32Value = 50
        },
        ComparisonTestCase(Int::class, Comparison.COMPARISON_LE, 50, 50, true) {
          it.int32Value = 50
        },
        ComparisonTestCase(Int::class, Comparison.COMPARISON_GT, 50, 25, false) {
          it.int32Value = 50
        },
        ComparisonTestCase(Int::class, Comparison.COMPARISON_GE, 50, 60, true) {
          it.int32Value = 50
        },

        // --- Int64 Checks ---
        ComparisonTestCase(
          Long::class,
          Comparison.COMPARISON_EQ,
          100L,
          100L,
          true
        ) { it.int64Value = 100L },
        ComparisonTestCase(
          Long::class,
          Comparison.COMPARISON_LT,
          100L,
          200L,
          false
        ) { it.int64Value = 100L },
        ComparisonTestCase(
          Long::class,
          Comparison.COMPARISON_GT,
          100L,
          150L,
          true
        ) { it.int64Value = 100L },

        // --- Float Checks ---
        ComparisonTestCase(
          Float::class,
          Comparison.COMPARISON_EQ,
          3.14f,
          3.14f,
          true
        ) { it.floatValue = 3.14f },
        ComparisonTestCase(
          Float::class,
          Comparison.COMPARISON_LE,
          3.14f,
          3.13f,
          true
        ) { it.floatValue = 3.14f },

        // --- Double Checks ---
        ComparisonTestCase(
          Double::class,
          Comparison.COMPARISON_EQ,
          2.718,
          2.718,
          true
        ) { it.doubleValue = 2.718 },
        ComparisonTestCase(
          Double::class,
          Comparison.COMPARISON_GT,
          2.718,
          2.719,
          true
        ) { it.doubleValue = 2.718 },

        // --- String Checks ---
        ComparisonTestCase(
          String::class,
          Comparison.COMPARISON_EQ,
          "beta",
          "beta",
          true
        ) { it.stringValue = "beta" },
        ComparisonTestCase(
          String::class,
          Comparison.COMPARISON_LT,
          "beta",
          "alpha",
          true
        ) { it.stringValue = "beta" }, // Lexicographical comparison
        ComparisonTestCase(
          String::class,
          Comparison.COMPARISON_GT,
          "beta",
          "gamma",
          true
        ) { it.stringValue = "beta" },

        // --- Enum Underlying Value Checks ---
        ComparisonTestCase(
          TestEnum::class,
          Comparison.COMPARISON_EQ,
          TestEnum.TEST_ENUM_1,
          TestEnum.TEST_ENUM_1,
          true
        ) { it.enumValue = 1 },
        ComparisonTestCase(
          TestEnum::class,
          Comparison.COMPARISON_GT,
          TestEnum.TEST_ENUM_1,
          TestEnum.TEST_ENUM_2,
          true
        ) { it.enumValue = 1 }, // 2 > 1
        ComparisonTestCase(
          TestEnum::class,
          Comparison.COMPARISON_LT,
          TestEnum.TEST_ENUM_2,
          TestEnum.TEST_ENUM_1,
          true
        ) { it.enumValue = 2 }  // 1 < 2
      )

      return testCases.map { case ->
        Arguments.of(
          named(
            "${case.type.simpleName} [${case.comparison.name}] returns ${case.shouldMatch}",
            case
          )
        )
      }.stream()
    }

    @JvmStatic
    fun invalidComparisonProvider(): Stream<Comparison> {
      return Stream.of(Comparison.COMPARISON_UNSET)
    }
  }
}