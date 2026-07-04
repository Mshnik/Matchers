package com.redpup.matchers.benchmark

import com.google.protobuf.TextFormat
import com.redpup.matchers.*
import com.redpup.matchers.benchmark.proto.MatcherBenchmarks
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.testing.proto.TestMessage
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class MatcherBenchmark {

  // JMH parameter matrix mapping directly to the 'name' fields in your textproto.
  // Add or remove names here to match your target execution suite.
  @Param("primitive_numeric_and_string", "collection_contains_distinct", "nested_message_hierarchy")
  lateinit var benchmarkName: String

  private lateinit var sampleProto: Matcher
  private lateinit var compiledEngine: KMatcher<TestMessage>
  private lateinit var targetInputs: List<TestMessage>

  @Setup(Level.Trial)
  fun setUp() {
    // Locate and parse the textproto from the project resources
    val resourcePath = "/com/redpup/matchers/benchmark/benchmarks.textproto"
    val resourceStream = javaClass.getResourceAsStream(resourcePath)
      ?: throw IllegalArgumentException("Could not find benchmark textproto at: $resourcePath")

    val benchmarksBuilder = MatcherBenchmarks.newBuilder()
    InputStreamReader(resourceStream, Charsets.UTF_8).use { reader ->
      TextFormat.getParser().merge(reader, benchmarksBuilder)
    }

    // Filter out the specific scenario configured for this benchmark thread invocation
    val selectedCase =
      benchmarksBuilder.build().benchmarkList.firstOrNull { it.name == benchmarkName }
        ?: throw IllegalStateException("No defined textproto scenario matching parameter: '$benchmarkName'")

    // Extract rules, pre-compile engine variants, and isolate input evaluation data
    sampleProto = selectedCase.matcher
    compiledEngine = KMatcher.compile(sampleProto)
    targetInputs = selectedCase.inputList
  }

  // --- Case 1: Measuring Compile Time ---
  // Overheads of indexing and generating the KMatcher state machine for this specific rule structure
  @Benchmark
  fun benchmarkCompileTime(): KMatcher<TestMessage> {
    return KMatcher.compile(sampleProto)
  }

  // --- Case 2: Measuring Match Time ---
  // Iterates over all matching and non-matching payload variants defined inside this textproto block
  @Benchmark
  fun benchmarkMatchTime(bh: Blackhole) {
    // A standard indexed loop prevents iterator allocation overhead in the hot path
    for (element in targetInputs) {
      bh.consume(compiledEngine.matchTyped(element))
    }
  }
}
