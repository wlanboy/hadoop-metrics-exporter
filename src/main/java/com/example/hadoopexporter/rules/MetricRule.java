package com.example.hadoopexporter.rules;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry under a group pattern in a metrics/*.yaml rule file.
 * Ported 1:1 from the Python exporter's rule schema (metric_def dict).
 */
public class MetricRule {

	private String pattern;
	private String type;
	private String name;
	private Map<String, String> labels = new LinkedHashMap<>();
	private String help;
	private String mapping;

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
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
