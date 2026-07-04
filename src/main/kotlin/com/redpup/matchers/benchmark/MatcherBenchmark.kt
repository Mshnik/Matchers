package com.redpup.matchers.benchmark

import com.redpup.matchers.*
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.matcher
import com.redpup.matchers.testing.proto.TestMessage
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class MatcherBenchmark {

  // --- Benchmark States ---
  private lateinit var sampleProto: Matcher
  private lateinit var compiledEngine: KMatcher<TestMessage>
  private lateinit var matchingMessage: TestMessage
  private lateinit var nonMatchingMessage: TestMessage

  @Setup(Level.Trial)
  fun setUp() {
    // 1. Build the raw Proto structural graph rule
    sampleProto = matcher {
      messageMatcher(TestMessage.getDescriptor()) {
        "string_value" value "active"
        "int32_value".matches<Int> {
          not { value(0) }
        }
        "string_values" matchesCollection {
          containsDistinct {
            matches { startsWith("prefix_") }
            matches { inSet(setOf("match_a", "match_b")) }
          }
        }
      }
    }

    // 2. Pre-compile an engine variant to isolate Match Time testing
    compiledEngine = KMatcher.compile(sampleProto)

    // 3. Pre-bake test data inputs
    matchingMessage = TestMessage.newBuilder()
      .setStringValue("active")
      .setInt32Value(42)
      .build()

    nonMatchingMessage = TestMessage.newBuilder()
      .setStringValue("inactive")
      .setInt32Value(0)
      .build()
  }

  // --- Case 1: Measuring Compile Time ---
  // Measures only the overhead of indexing and building the KMatcher engine
  @Benchmark
  fun benchmarkCompileTime(): KMatcher<TestMessage> {
    return KMatcher.compile(sampleProto)
  }

  // --- Case 2: Measuring Match Time (Success Path) ---
  // Measures the runtime speed of evaluation against a matching payload
  @Benchmark
  fun benchmarkMatchTimeSuccess(): Boolean {
    return compiledEngine.matchTyped(matchingMessage)
  }

  // --- Case 3: Measuring Match Time (Fail Fast Short-Circuit Path) ---
  // Measures how quickly your system short-circuits on the first invalid field
  @Benchmark
  fun benchmarkMatchTimeFailFast(): Boolean {
    return compiledEngine.matchTyped(TestMessage.getDefaultInstance())
  }
}
