package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.StringMatcher.CaseSensitivity
import com.redpup.matchers.proto.StringMatcher.MatcherCase

/** The compiled implementation of [com.redpup.matchers.proto.StringMatcher]. */
internal sealed class KStringMatcher(proto: Matcher) : KMatcher<String>(String::class, proto) {
  protected val ignoreCase = proto.stringMatcher.caseSensitive == CaseSensitivity.CASE_INSENSITIVE

  companion object {
    /** Compiles [proto] into a [KStringMatcher]. */
    fun compile(matcher: Matcher): KStringMatcher {
      check(matcher.hasStringMatcher()) {
        "Expected StringMatcher, found $matcher"
      }

      return when (matcher.stringMatcher.matcherCase) {
        MatcherCase.VALUE -> ValueKStringMatcher(matcher)
        MatcherCase.STARTS_WITH -> StartsWithKStringMatcher(matcher)
        MatcherCase.ENDS_WITH -> EndsWithKStringMatcher(matcher)
        MatcherCase.CONTAINS -> ContainsKStringMatcher(matcher)
        MatcherCase.PATTERN -> PatternKStringMatcher(matcher)
        MatcherCase.MATCHER_NOT_SET -> throw IllegalArgumentException("Unsupported matcher case: $matcher")
        null -> throw NullPointerException()
      }
    }
  }
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getValue] */
private class ValueKStringMatcher(proto: Matcher) : KStringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.value.equals(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getStartsWith] */
private class StartsWithKStringMatcher(proto: Matcher) : KStringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.startsWith.startsWith(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getEndsWith] */
private class EndsWithKStringMatcher(proto: Matcher) : KStringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.endsWith.endsWith(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getContains] */
private class ContainsKStringMatcher(proto: Matcher) : KStringMatcher(proto) {
  override fun matchTyped(value: String): Boolean =
    proto.stringMatcher.contains.contains(value, ignoreCase)
}

/** Implementation of [com.redpup.matchers.proto.StringMatcher.getPattern] */
private class PatternKStringMatcher(proto: Matcher) : KStringMatcher(proto) {
  private val pattern =
    Regex(
      proto.stringMatcher.pattern,
      if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else setOf()
    )

  override fun matchTyped(value: String): Boolean = pattern.matches(value)
}
