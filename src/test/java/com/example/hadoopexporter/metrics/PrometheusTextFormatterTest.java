package com.example.hadoopexporter.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrometheusTextFormatterTest {

	@Test
	void formatsHelpTypeAndLabeledSamples() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "Some help", List.of("cluster", "host"));
		family.addMetric(List.of("hadoop_test", "nn1"), 42.0);

		String text = PrometheusTextFormatter.format(List.of(family));

		assertThat(text).isEqualTo(
				"# HELP hadoop_test_metric Some help\n"
						+ "# TYPE hadoop_test_metric gauge\n"
						+ "hadoop_test_metric{cluster=\"hadoop_test\",host=\"nn1\"} 42\n");
	}

	@Test
	void familyWithoutLabelsOmitsBraces() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "help", List.of());
		family.addMetric(List.of(), 1.0);

		String text = PrometheusTextFormatter.format(List.of(family));

		assertThat(text).contains("hadoop_test_metric 1\n").doesNotContain("{");
	}

	@Test
	void familiesWithNoSamplesAreSkippedEntirely() {
		MetricFamily empty = new MetricFamily("hadoop_empty", "help", List.of());

		String text = PrometheusTextFormatter.format(List.of(empty));

		assertThat(text).isEmpty();
	}

	@Test
	void missingHelpFallsBackToMetricName() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", null, List.of());
		family.addMetric(List.of(), 1.0);

		String text = PrometheusTextFormatter.format(List.of(family));

		assertThat(text).contains("# HELP hadoop_test_metric hadoop_test_metric\n");
	}

	@Test
	void integerValuedDoublesAreFormattedWithoutDecimalPoint() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "help", List.of());
		family.addMetric(List.of(), 5.0);

		assertThat(PrometheusTextFormatter.format(List.of(family))).contains(" 5\n");
	}

	@Test
	void fractionalValuesKeepDecimalRepresentation() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "help", List.of());
		family.addMetric(List.of(), 3.14);

		assertThat(PrometheusTextFormatter.format(List.of(family))).contains(" 3.14\n");
	}

	@Test
	void nanAndInfinityAreFormattedAsPrometheusSpecials() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "help", List.of("type"));
		family.addMetric(List.of("nan"), Double.NaN);
		family.addMetric(List.of("posinf"), Double.POSITIVE_INFINITY);
		family.addMetric(List.of("neginf"), Double.NEGATIVE_INFINITY);

		String text = PrometheusTextFormatter.format(List.of(family));

		assertThat(text).contains("{type=\"nan\"} NaN\n")
				.contains("{type=\"posinf\"} +Inf\n")
				.contains("{type=\"neginf\"} -Inf\n");
	}

	@Test
	void labelValuesAreEscaped() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "help", List.of("path"));
		family.addMetric(List.of("a\\b\"c\nd"), 1.0);

		String text = PrometheusTextFormatter.format(List.of(family));

		assertThat(text).contains("path=\"a\\\\b\\\"c\\nd\"");
	}

	@Test
	void helpTextIsEscaped() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "line one\nline two \\ end", List.of());
		family.addMetric(List.of(), 1.0);

		String text = PrometheusTextFormatter.format(List.of(family));

		assertThat(text).contains("# HELP hadoop_test_metric line one\\nline two \\\\ end\n");
	}

	@Test
	void multipleFamiliesAreConcatenated() {
		MetricFamily first = new MetricFamily("hadoop_a", "help a", List.of());
		first.addMetric(List.of(), 1.0);
		MetricFamily second = new MetricFamily("hadoop_b", "help b", List.of());
		second.addMetric(List.of(), 2.0);

		String text = PrometheusTextFormatter.format(List.of(first, second));

		assertThat(text).contains("hadoop_a").contains("hadoop_b");
		assertThat(text.indexOf("hadoop_a")).isLessThan(text.indexOf("hadoop_b"));
	}
}
