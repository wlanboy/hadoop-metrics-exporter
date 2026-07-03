package com.example.hadoopexporter.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mirrors the "server" / "jmx" sections of the original Python exporter's config.yaml.
 */
@ConfigurationProperties(prefix = "exporter")
public class ExporterProperties {

	private String clusterName = "hadoop_cluster";
	private String path = "/metrics";
	private String metricsDir;
	private List<ClusterJmx> jmx = new ArrayList<>();

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMetricsDir() {
		return metricsDir;
	}

	public void setMetricsDir(String metricsDir) {
		this.metricsDir = metricsDir;
	}

	public List<ClusterJmx> getJmx() {
		return jmx;
	}

	public void setJmx(List<ClusterJmx> jmx) {
		this.jmx = jmx;
	}

	public static class ClusterJmx {
		private String cluster;
		private Map<String, List<String>> services = new LinkedHashMap<>();

		public String getCluster() {
			return cluster;
		}

		public void setCluster(String cluster) {
			this.cluster = cluster;
		}

		public Map<String, List<String>> getServices() {
			return services;
		}

		public void setServices(Map<String, List<String>> services) {
			this.services = services;
		}
	}
}
