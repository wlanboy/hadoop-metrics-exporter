package com.example.hadoopexporter.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deserialized form of a metrics/*.yaml file (e.g. namenode.yaml, common.yaml).
 */
public class RuleSet {

	private boolean lowercaseOutputName = true;
	private boolean lowercaseOutputLabel = true;
	private Map<String, List<MetricRule>> rules = new LinkedHashMap<>();

	public boolean isLowercaseOutputName() {
		return lowercaseOutputName;
	}

	public void setLowercaseOutputName(boolean lowercaseOutputName) {
		this.lowercaseOutputName = lowercaseOutputName;
	}

	public boolean isLowercaseOutputLabel() {
		return lowercaseOutputLabel;
	}

	public void setLowercaseOutputLabel(boolean lowercaseOutputLabel) {
		this.lowercaseOutputLabel = lowercaseOutputLabel;
	}

	public Map<String, List<MetricRule>> getRules() {
		return rules;
	}

	public void setRules(Map<String, List<MetricRule>> rules) {
		this.rules = rules;
	}
}
