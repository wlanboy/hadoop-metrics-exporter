package com.example.hadoopexporter.metrics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.rules.MetricRule;
import com.example.hadoopexporter.rules.RuleSet;
import com.example.hadoopexporter.rules.RuleSetLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the regex-substitution engine in {@link HadoopMetricCollector},
 * exercised through a minimal test subclass with a hand-built {@link RuleSet} and a
 * mocked {@link JmxClient} - no HTTP server and no real metrics/*.yaml rule files involved.
 */
@ExtendWith(MockitoExtension.class)
class HadoopMetricCollectorUnitTest {

	@Mock
	private JmxClient jmxClient;
	@Mock
	private RuleSetLoader ruleSetLoader;

	private static class TestCollector extends HadoopMetricCollector {
		TestCollector(List<String> urls, RuleSetLoader ruleSetLoader, JmxClient jmxClient, String jvmMetricsServiceTag) {
			super("hadoop_test", urls, "test", "service", ruleSetLoader, jmxClient, jvmMetricsServiceTag);
		}
	}

	private MetricRule rule(String pattern, String name, Map<String, String> labels, String help) {
		MetricRule rule = new MetricRule();
		rule.setPattern(pattern);
		rule.setType("GAUSE");
		rule.setName(name);
		rule.setLabels(labels);
		rule.setHelp(help);
		return rule;
	}

    @Test
	void collectConvertsMatchingBeanFieldsIntoMetricFamilies() {
		RuleSet ruleSet = new RuleSet();
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(rule("^Foo(.+)$", "foo_$1", Map.of(), "Foo metrics")));
		ruleSet.setRules(rules);
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);

		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("FooBar", 42);
		when(jmxClient.getBeans("http://host/jmx")).thenReturn(List.of(bean));

		TestCollector collector = new TestCollector(List.of("http://host/jmx"), ruleSetLoader, jmxClient, null);
		List<MetricFamily> families = collector.collect();

		assertThat(families).hasSize(1);
		MetricFamily family = families.get(0);
		assertThat(family.getName()).isEqualTo("hadoop_test_service_foo_bar");
		assertThat(family.getHelp()).isEqualTo("Foo metrics");
		assertThat(family.getLabelNames()).containsExactly("cluster");
		assertThat(family.getSamples()).hasSize(1);
		assertThat(family.getSamples().get(0).labelValues()).containsExactly("hadoop_test");
		assertThat(family.getSamples().get(0).value()).isEqualTo(42.0);
	}

	@Test
	void nonMatchingBeanNamesProduceNoSamples() {
		RuleSet ruleSet = new RuleSet();
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(rule("^Foo(.+)$", "foo_$1", Map.of(), "help")));
		ruleSet.setRules(rules);
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);

		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "OtherBean");
		bean.put("FooBar", 1);
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		TestCollector collector = new TestCollector(List.of("http://host/jmx"), ruleSetLoader, jmxClient, null);
		List<MetricFamily> families = collector.collect();

		// no bean matched the group pattern, so no MetricFamily was ever instantiated for it.
		assertThat(families).isEmpty();
	}

	@Test
	void nonMatchingMetricFieldsWithinAMatchingBeanAreSkipped() {
		RuleSet ruleSet = new RuleSet();
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(rule("^Foo(.+)$", "foo_$1", Map.of(), "help")));
		ruleSet.setRules(rules);
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);

		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("BarBaz", 1);
		bean.put("modelerType", "ignored");
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		TestCollector collector = new TestCollector(List.of("http://host/jmx"), ruleSetLoader, jmxClient, null);
		List<MetricFamily> families = collector.collect();

		// "BarBaz" doesn't match the rule pattern and "modelerType" is a non-metric field,
		// so no rule ever fired and no MetricFamily was created for the group.
		assertThat(families).isEmpty();
	}

	@Test
	void unparseableMetricValuesAreSkippedRatherThanThrowing() {
		RuleSet ruleSet = new RuleSet();
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(rule("^Foo(.+)$", "foo_$1", Map.of(), "help")));
		ruleSet.setRules(rules);
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);

		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("FooBar", "not-a-number");
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		TestCollector collector = new TestCollector(List.of("http://host/jmx"), ruleSetLoader, jmxClient, null);
		List<MetricFamily> families = collector.collect();

		// the family is created before the value is resolved, so it exists but stays sample-less.
		assertThat(families).hasSize(1);
		assertThat(families.get(0).getSamples()).isEmpty();
	}

	@Test
	void nonGaugeRulesAreSkippedWithoutFailing() {
		RuleSet ruleSet = new RuleSet();
		MetricRule counterRule = rule("^Foo(.+)$", "foo_$1", Map.of(), "help");
		counterRule.setType("COUNTER");
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(counterRule));
		ruleSet.setRules(rules);
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);

		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("FooBar", 1);
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		TestCollector collector = new TestCollector(List.of("http://host/jmx"), ruleSetLoader, jmxClient, null);
		List<MetricFamily> families = collector.collect();

		// the rule never even reaches matchesMetricName(), so no family is created at all.
		assertThat(families).isEmpty();
	}

	@Test
	void addsHostLabelFromJvmMetricsBeanWhenServiceTagConfigured() {
		RuleSet ruleSet = new RuleSet();
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(rule("^Foo(.+)$", "foo_$1", Map.of(), "help")));
		ruleSet.setRules(rules);
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);

		Map<String, Object> jvmBean = new LinkedHashMap<>();
		jvmBean.put("name", "Hadoop:service=MyService,name=JvmMetrics");
		jvmBean.put("tag.Hostname", "node-1");

		Map<String, Object> myBean = new LinkedHashMap<>();
		myBean.put("name", "MyBean");
		myBean.put("FooBar", 5);

		when(jmxClient.getBeans(anyString())).thenReturn(List.of(jvmBean, myBean));

		TestCollector collector = new TestCollector(List.of("http://host/jmx"), ruleSetLoader, jmxClient, "MyService");
		List<MetricFamily> families = collector.collect();

		MetricFamily family = families.get(0);
		assertThat(family.getLabelNames()).containsExactly("cluster", "host");
		assertThat(family.getSamples().get(0).labelValues()).containsExactly("hadoop_test", "node-1");
	}

	@Test
	void trailingSlashesAreStrippedFromConfiguredUrls() {
		RuleSet ruleSet = new RuleSet();
		ruleSet.setRules(new LinkedHashMap<>());
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);
		when(jmxClient.getBeans("http://host/jmx")).thenReturn(List.of());

		TestCollector collector = new TestCollector(List.of("http://host/jmx///"), ruleSetLoader, jmxClient, null);
		collector.collect();

		org.mockito.Mockito.verify(jmxClient).getBeans("http://host/jmx");
	}

	@Test
	void sameLabelValuesAreLowercasedWhenRuleSetRequestsIt() {
		RuleSet ruleSet = new RuleSet();
		ruleSet.setLowercaseOutputName(true);
		ruleSet.setLowercaseOutputLabel(true);
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(rule("^Foo(.+)$", "Foo_$1", Map.of("Kind", "$1"), "help")));
		ruleSet.setRules(rules);
		when(ruleSetLoader.loadForService("service")).thenReturn(ruleSet);

		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("FooBAR", 3);
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		TestCollector collector = new TestCollector(List.of("http://host/jmx"), ruleSetLoader, jmxClient, null);
		MetricFamily family = collector.collect().get(0);

		assertThat(family.getName()).isEqualTo("hadoop_test_service_foo_bar");
		assertThat(family.getLabelNames()).containsExactly("cluster", "kind");
	}
}
