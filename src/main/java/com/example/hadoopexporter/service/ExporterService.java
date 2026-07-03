package com.example.hadoopexporter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import com.example.hadoopexporter.config.ExporterProperties;
import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.HadoopMetricCollector;
import com.example.hadoopexporter.metrics.MetricFamily;
import com.example.hadoopexporter.metrics.collectors.DataNodeCollector;
import com.example.hadoopexporter.metrics.collectors.HiveServer2Collector;
import com.example.hadoopexporter.metrics.collectors.JournalNodeCollector;
import com.example.hadoopexporter.metrics.collectors.NameNodeCollector;
import com.example.hadoopexporter.metrics.collectors.NodeManagerCollector;
import com.example.hadoopexporter.metrics.collectors.ResourceManagerCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds one HadoopMetricCollector per (cluster, service) entry in exporter.jmx, mirroring
 * hadoop_exporter.exporter.Exporter's Service/COLLECTOR_MAPPING wiring, and drives them
 * on every /metrics scrape (each collector fetches JMX synchronously, like the original).
 */
@Service
public class ExporterService {

	private static final Logger log = LoggerFactory.getLogger(ExporterService.class);

	private final List<HadoopMetricCollector> collectors = new ArrayList<>();

	public ExporterService(ExporterProperties properties, RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		Map<String, BiFunction<String, List<String>, HadoopMetricCollector>> factories = Map.of(
				"namenode", (cluster, urls) -> new NameNodeCollector(cluster, urls, ruleSetLoader, jmxClient),
				"datanode", (cluster, urls) -> new DataNodeCollector(cluster, urls, ruleSetLoader, jmxClient),
				"journalnode", (cluster, urls) -> new JournalNodeCollector(cluster, urls, ruleSetLoader, jmxClient),
				"resourcemanager", (cluster, urls) -> new ResourceManagerCollector(cluster, urls, ruleSetLoader, jmxClient),
				"nodemanager", (cluster, urls) -> new NodeManagerCollector(cluster, urls, ruleSetLoader, jmxClient),
				"hiveserver2", (cluster, urls) -> new HiveServer2Collector(cluster, urls, ruleSetLoader, jmxClient));

		for (ExporterProperties.ClusterJmx clusterJmx : properties.getJmx()) {
			String cluster = clusterJmx.getCluster() != null ? clusterJmx.getCluster() : properties.getClusterName();
			for (Map.Entry<String, List<String>> entry : clusterJmx.getServices().entrySet()) {
				String serviceName = entry.getKey().toLowerCase(Locale.ROOT);
				BiFunction<String, List<String>, HadoopMetricCollector> factory = factories.get(serviceName);
				if (factory == null) {
					log.warn("Unknown service name: {}. Ignored", entry.getKey());
					continue;
				}
				List<String> urls = entry.getValue();
				collectors.add(factory.apply(cluster, urls));
				log.info("Registered collector (cluster: {}, service: {}, urls: {})", cluster, serviceName, urls);
			}
		}

		if (collectors.isEmpty()) {
			log.warn("No exporter.jmx services configured; /metrics will return no data. "
					+ "Configure exporter.jmx in application.yml (see the commented example).");
		}
	}

	public List<MetricFamily> collectAll() {
		List<MetricFamily> result = new ArrayList<>();
		for (HadoopMetricCollector collector : collectors) {
			result.addAll(collector.collect());
		}
		return result;
	}
}
