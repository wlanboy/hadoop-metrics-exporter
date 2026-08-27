package com.example.hadoopexporter.rules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry under a group pattern in a metrics/*.yaml rule file.
 * Ported 1:1 from the Python exporter's rule schema (metric_def dict).
 */
public class MetricRule {

	/** The only metric type currently supported; matches the rule files' "type: GAUSE" value. */
	private static final String GAUGE_TYPE = "GAUSE";

	private String pattern;
	private String type;
	private String name;
	private Map<String, String> labels = new LinkedHashMap<>();
	private String help;
	private String mapping;

	private Pattern compiledPattern;

	public boolean isGauge() {
		return GAUGE_TYPE.equals(type);
	}

	/** Whether this rule's pattern matches (from the start of) the given metric field name. */
	public boolean matchesMetricName(String metricName) {
		return compiledPattern().matcher(metricName).lookingAt();
	}

	/** Resolves a raw JMX metric value to a double, applying this rule's mapping function if set. */
	public double resolveValue(Object metricValue) {
		if (mapping != null && !mapping.isBlank()) {
			return ValueMappings.resolve(mapping).apply(String.valueOf(metricValue));
		}
		return toDouble(metricValue);
	}

	private static double toDouble(Object value) {
		if (value instanceof Number n) {
			return n.doubleValue();
		}
		if (value instanceof Boolean b) {
			return b ? 1.0 : 0.0;
		}
		if (value instanceof String s) {
			return Double.parseDouble(s);
		}
		throw new IllegalArgumentException("Cannot convert to number: " + value);
	}

	private Pattern compiledPattern() {
		if (compiledPattern == null) {
			compiledPattern = Pattern.compile(pattern);
		}
		return compiledPattern;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
		this.compiledPattern = null;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getLabels() {
		return labels;
	}

	public void setLabels(Map<String, String> labels) {
		this.labels = labels;
	}

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public String getMapping() {
		return mapping;
	}

	@JsonProperty("mapping")
	public void setMapping(String mapping) {
		this.mapping = mapping;
	}
}
