package com.example.hadoopexporter.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Equivalent of prometheus_client's GaugeMetricFamily: a metric name/help/label-schema
 * plus the samples (label values + value) collected for it during one scrape.
 */
public class MetricFamily {

	private final String name;
	private final String help;
	private final List<String> labelNames;
	private final List<Sample> samples = new ArrayList<>();

	public MetricFamily(String name, String help, List<String> labelNames) {
		this.name = name;
		this.help = help;
		this.labelNames = labelNames;
	}

	public void addMetric(List<String> labelValues, double value) {
		samples.add(new Sample(labelValues, value));
	}

	public String getName() {
		return name;
	}

	public String getHelp() {
		return help;
	}

	public List<String> getLabelNames() {
		return labelNames;
	}

	public List<Sample> getSamples() {
		return samples;
	}

	public record Sample(List<String> labelValues, double value) {
	}
}
