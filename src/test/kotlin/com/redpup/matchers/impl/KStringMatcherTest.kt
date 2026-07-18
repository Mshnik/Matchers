package com.redpup.matchers.impl

import com.google.common.truth.Truth.assertThat
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.StringMatcher.CaseSensitivity
import com.redpup.matchers.proto.matcher
import com.redpup.matchers.proto.stringMatcher
import org.junit.jupiter.api.Test

class KStringMatcherTest {

  @Test
  fun `ValueStringMatcher respects case sensitivity rules`() {
    val sensitiveProto = matcher {
      stringMatcher = stringMatcher {
        value = "Kotlin"
        caseSensitive = CaseSensitivity.CASE_SENSITIVE
      }
    }

    val insensitiveProto = matcher {
      stringMatcher = stringMatcher {
        value = "Kotlin"
        caseSensitive = CaseSensitivity.CASE_INSENSITIVE
      }
    }

    assertThat(KMatcher.compile<String>(sensitiveProto).match("Kotlin")).isTrue()
    assertThat(KMatcher.compile<String>(sensitiveProto).match("kotlin")).isFalse()

    assertThat(KMatcher.compile<String>(insensitiveProto).match("kotlin")).isTrue()
    assertThat(KMatcher.compile<String>(insensitiveProto).match("KOTLIN")).isTrue()
  }

  @Test
  fun `StartsWithStringMatcher asserts conditions based on current inversion design`() {
    val proto = matcher {
      stringMatcher = stringMatcher {
        startsWith = "Hi"
        caseSensitive = CaseSensitivity.CASE_SENSITIVE
      }
    }
    val matcher = KMatcher.compile<String>(proto)

    assertThat(matcher.match("Hi There")).isTrue()
    assertThat(matcher.match("There Hi")).isFalse()
  }

  @Test
  fun `EndsWithStringMatcher asserts conditions based on current inversion design`() {
    val proto = matcher {
      stringMatcher = stringMatcher {
        endsWith = "There"
        caseSensitive = CaseSensitivity.CASE_SENSITIVE
      }
    }
    val matcher = KMatcher.compile<String>(proto)

    assertThat(matcher.match("Hi There")).isTrue()
    assertThat(matcher.match("There Hi")).isFalse()
  }

  @Test
  fun `ContainsStringMatcher asserts conditions based on current inversion design`() {
    val proto = matcher {
      stringMatcher = stringMatcher {
        contains = "There"
        caseSensitive = CaseSensitivity.CASE_SENSITIVE
      }
    }
    val matcher = KMatcher.compile<String>(proto)

    assertThat(matcher.match("Hi There")).isTrue()
    assertThat(matcher.match("There Hi")).isTrue()
    assertThat(matcher.match("Hi Hi")).isFalse()
  }

  @Test
  fun `PatternStringMatcher matches standard regular expression definitions`() {
    val proto = matcher {
      stringMatcher = stringMatcher {
        pattern = "^[A-Z]+$"
      }
    }
    val matcher = KMatcher.compile<String>(proto)

    assertThat(matcher.match("ABC")).isTrue()
    assertThat(matcher.match("abc")).isFalse()
  }
}