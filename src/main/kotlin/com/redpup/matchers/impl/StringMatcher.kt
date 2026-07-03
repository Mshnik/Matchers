package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.StringMatcher.CaseSensitivity
import com.redpup.matchers.proto.StringMatcher.MatcherCase

/** The compiled implementation of [com.redpup.matchers.proto.StringMatcher]. */
internal sealed class StringMatcher(proto: Matcher) : KMatcher<String>(String::class, proto) {
  protected val ignoreCase = proto.stringMatcher.caseSensitive == CaseSensitivity.CASE_INSENSITIVE

  companion object {
    /** Compiles [proto] into a [StringMatcher]. */
    fun compile(matcher: Matcher): StringMatcher {
      check(matcher.hasStringMatcher()) {
        "Expected StringMatcher, found $matcher"
      }

      return when (matcher.stringMatcher.matcherCase) {
        MatcherCase.VALUE -> ValueStringMatcher(matcher)
        MatcherCase.STARTS_WITH -> StartsWithStringMatcher(matcher)
        MatcherCase.ENDS_WITH -> EndsWithStringMatcher(matcher)
        MatcherCase.CONTAINS -> ContainsStringMatcher(matcher)
        MatcherCase.PATTERN -> PatternStringMatcher(matcher)
        MatcherCase.MATCHER_NOT_SET -> throw IllegalArgumentException("Unsupported matcher case: $matcher")
        null -> throw NullPointerException()
      }
    }
  }
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getValue] */
private class ValueStringMatcher(proto: Matcher) : StringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.value.equals(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getStartsWith] */
private class StartsWithStringMatcher(proto: Matcher) : StringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.startsWith.startsWith(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getEndsWith] */
private class EndsWithStringMatcher(proto: Matcher) : StringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.endsWith.endsWith(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getContains] */
private class ContainsStringMatcher(proto: Matcher) : StringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.contains.contains(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getPattern] */
private class PatternStringMatcher(proto: Matcher) : StringMatcher(proto) {
  private val pattern =
    Regex(
      proto.stringMatcher.pattern,
      if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else setOf()
    )

  override fun matchTyped(value: String): Boolean = pattern.matches(value)
}
