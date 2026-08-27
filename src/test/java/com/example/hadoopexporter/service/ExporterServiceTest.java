package com.example.hadoopexporter.service;

import java.util.List;
import java.util.Map;

import com.example.hadoopexporter.config.ExporterProperties;
import com.example.hadoopexporter.jmx.JmxClient;
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
}
