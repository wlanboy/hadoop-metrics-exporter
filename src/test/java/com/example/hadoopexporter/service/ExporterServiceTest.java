package com.example.hadoopexporter.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.hadoopexporter.config.ExporterProperties;
import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.MetricFamily;
import com.example.hadoopexporter.rules.MetricRule;
import com.example.hadoopexporter.rules.RuleSet;
import com.example.hadoopexporter.rules.RuleSetLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the collector-wiring logic in ExporterService, with RuleSetLoader
 * and JmxClient mocked out so no real rule files or JMX endpoints are touched. Wiring is
 * observed indirectly through how many times/with which service name RuleSetLoader is
 * invoked - one call per registered collector, at construction time.
 */
@ExtendWith(MockitoExtension.class)
class ExporterServiceTest {

	@Mock
	private RuleSetLoader ruleSetLoader;
	@Mock
	private JmxClient jmxClient;

	@Test
	void registersOneCollectorPerConfiguredServiceAcrossClusters() {
		when(ruleSetLoader.loadForService(anyString())).thenReturn(new RuleSet());

		ExporterProperties properties = new ExporterProperties();
		ExporterProperties.ClusterJmx clusterJmx = new ExporterProperties.ClusterJmx();
		clusterJmx.setCluster("prod");
		clusterJmx.setServices(Map.of(
				"namenode", List.of("http://nn1/jmx"),
				"datanode", List.of("http://dn1/jmx")));
		properties.setJmx(List.of(clusterJmx));

		new ExporterService(properties, ruleSetLoader, jmxClient);

		verify(ruleSetLoader).loadForService("namenode");
		verify(ruleSetLoader).loadForService("datanode");
	}

	@Test
	void ignoresUnknownServiceNamesWithoutRegisteringAnything() {
		ExporterProperties properties = new ExporterProperties();
		ExporterProperties.ClusterJmx clusterJmx = new ExporterProperties.ClusterJmx();
		clusterJmx.setCluster("prod");
		clusterJmx.setServices(Map.of("totally-unknown-service", List.of("http://x/jmx")));
		properties.setJmx(List.of(clusterJmx));

		ExporterService service = new ExporterService(properties, ruleSetLoader, jmxClient);

		verify(ruleSetLoader, never()).loadForService(anyString());
		assertThat(service.collectAll()).isEmpty();
	}

	@Test
	void serviceNameMatchingIsCaseInsensitive() {
		when(ruleSetLoader.loadForService(anyString())).thenReturn(new RuleSet());

		ExporterProperties properties = new ExporterProperties();
		ExporterProperties.ClusterJmx clusterJmx = new ExporterProperties.ClusterJmx();
		clusterJmx.setCluster("prod");
		clusterJmx.setServices(Map.of("NameNode", List.of("http://nn1/jmx")));
		properties.setJmx(List.of(clusterJmx));

		new ExporterService(properties, ruleSetLoader, jmxClient);

		verify(ruleSetLoader, times(1)).loadForService("namenode");
	}

	@Test
	void collectAllReturnsEmptyListWhenNoServicesConfigured() {
		ExporterProperties properties = new ExporterProperties();

		ExporterService service = new ExporterService(properties, ruleSetLoader, jmxClient);

		assertThat(service.collectAll()).isEmpty();
		verify(ruleSetLoader, never()).loadForService(anyString());
		verify(jmxClient, never()).getBeans(anyString());
	}

	private RuleSet ruleSetWithFooBarMetric() {
		MetricRule rule = new MetricRule();
		rule.setPattern("^Foo(.+)$");
		rule.setType("GAUGE");
		rule.setName("foo_$1");
		rule.setLabels(Map.of());
		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		rules.put("^MyBean$", List.of(rule));
		RuleSet ruleSet = new RuleSet();
		ruleSet.setRules(rules);
		return ruleSet;
	}

	private ExporterProperties propertiesWithOneDatanode(List<String> excludedMetrics) {
		ExporterProperties properties = new ExporterProperties();
		ExporterProperties.ClusterJmx clusterJmx = new ExporterProperties.ClusterJmx();
		clusterJmx.setCluster("prod");
		clusterJmx.setServices(Map.of("datanode", List.of("http://dn1/jmx")));
		properties.setJmx(List.of(clusterJmx));
		properties.setExcludedMetrics(excludedMetrics);
		return properties;
	}

	@Test
	void metricFamiliesMatchingAnExcludedMetricsPatternAreDroppedFromCollectAll() {
		when(ruleSetLoader.loadForService("datanode")).thenReturn(ruleSetWithFooBarMetric());
		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("FooBar", 1);
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		ExporterProperties properties = propertiesWithOneDatanode(List.of("hadoop_hdfs_datanode_foo_bar"));
		ExporterService service = new ExporterService(properties, ruleSetLoader, jmxClient);

		assertThat(service.collectAll()).isEmpty();
	}

	@Test
	void excludedMetricsPatternsAreFullMatchNotSubstring() {
		when(ruleSetLoader.loadForService("datanode")).thenReturn(ruleSetWithFooBarMetric());
		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("FooBar", 1);
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		// "foo_bar" is a substring of the metric name but not a full match, so it must not exclude it.
		ExporterProperties properties = propertiesWithOneDatanode(List.of("foo_bar"));
		ExporterService service = new ExporterService(properties, ruleSetLoader, jmxClient);

		assertThat(service.collectAll()).extracting(MetricFamily::getName).containsExactly("hadoop_hdfs_datanode_foo_bar");
	}

	@Test
	void excludedMetricsPatternSupportsWildcardsAcrossAWholeServicePrefix() {
		when(ruleSetLoader.loadForService("datanode")).thenReturn(ruleSetWithFooBarMetric());
		Map<String, Object> bean = new LinkedHashMap<>();
		bean.put("name", "MyBean");
		bean.put("FooBar", 1);
		when(jmxClient.getBeans(anyString())).thenReturn(List.of(bean));

		ExporterProperties properties = propertiesWithOneDatanode(List.of("hadoop_hdfs_datanode_.*"));
		ExporterService service = new ExporterService(properties, ruleSetLoader, jmxClient);

		assertThat(service.collectAll()).isEmpty();
	}
}
