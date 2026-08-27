package com.example.hadoopexporter.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricFamilyTest {

	@Test
	void exposesConstructorArguments() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "some help text", List.of("cluster", "host"));

		assertThat(family.getName()).isEqualTo("hadoop_test_metric");
		assertThat(family.getHelp()).isEqualTo("some help text");
		assertThat(family.getLabelNames()).containsExactly("cluster", "host");
		assertThat(family.getSamples()).isEmpty();
	}

	@Test
	void addMetricAppendsSamplesInInsertionOrder() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "help", List.of("cluster"));

		family.addMetric(List.of("a"), 1.0);
		family.addMetric(List.of("b"), 2.0);

		assertThat(family.getSamples()).extracting(MetricFamily.Sample::labelValues, MetricFamily.Sample::value)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(List.of("a"), 1.0),
						org.assertj.core.groups.Tuple.tuple(List.of("b"), 2.0));
	}
}
