package com.example.hadoopexporter.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricRuleTest {

	@Test
	void isGaugeIsTrueOnlyForGauseType() {
		MetricRule rule = new MetricRule();
		assertThat(rule.isGauge()).isFalse();

		rule.setType("GAUSE");
		assertThat(rule.isGauge()).isTrue();

		rule.setType("COUNTER");
		assertThat(rule.isGauge()).isFalse();

		rule.setType(null);
		assertThat(rule.isGauge()).isFalse();
	}

	@Test
	void matchesMetricNameLooksAtStartOfName() {
		MetricRule rule = new MetricRule();
		rule.setPattern("^Cache(.+)$");

		assertThat(rule.matchesMetricName("CacheUpdated")).isTrue();
		assertThat(rule.matchesMetricName("NotCacheUpdated")).isFalse();
	}

	@Test
	void changingPatternInvalidatesCompiledPatternCache() {
		MetricRule rule = new MetricRule();
		rule.setPattern("^Foo(.+)$");
		assertThat(rule.matchesMetricName("FooBar")).isTrue();

		rule.setPattern("^Baz(.+)$");
		assertThat(rule.matchesMetricName("FooBar")).isFalse();
		assertThat(rule.matchesMetricName("BazQux")).isTrue();
	}

	@Test
	void resolveValueConvertsNumbersBooleansAndStrings() {
		MetricRule rule = new MetricRule();
		assertThat(rule.resolveValue(42)).isEqualTo(42.0);
		assertThat(rule.resolveValue(3.5)).isEqualTo(3.5);
		assertThat(rule.resolveValue(true)).isEqualTo(1.0);
		assertThat(rule.resolveValue(false)).isEqualTo(0.0);
		assertThat(rule.resolveValue("12.5")).isEqualTo(12.5);
	}

	@Test
	void resolveValueThrowsForUnconvertibleValue() {
		MetricRule rule = new MetricRule();
		assertThatThrownBy(() -> rule.resolveValue(new Object()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void resolveValueAppliesConfiguredMappingInsteadOfRawConversion() {
		MetricRule rule = new MetricRule();
		rule.setMapping("hadoop_exporter.mapping.fsstate");

		assertThat(rule.resolveValue("Operational")).isEqualTo(0.0);
		assertThat(rule.resolveValue("Safemode")).isEqualTo(1.0);
		assertThat(rule.resolveValue("SomethingElse")).isEqualTo(9999.0);
	}

	@Test
	void blankOrNullMappingFallsBackToNumericConversion() {
		MetricRule rule = new MetricRule();
		rule.setMapping("");
		assertThat(rule.resolveValue(7)).isEqualTo(7.0);

		rule.setMapping(null);
		assertThat(rule.resolveValue(8)).isEqualTo(8.0);
	}
}
