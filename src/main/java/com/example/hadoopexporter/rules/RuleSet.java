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

	/**
	 * Merges this RuleSet with {@code other} (typically the shared common.yaml), the way
	 * RuleSetLoader combines a service's rules with common.yaml: the lowercase flags come
	 * from this RuleSet, and rule groups are the union of both, with {@code other}'s groups
	 * taking precedence on a shared group pattern. {@code other} may be null.
	 */
	public RuleSet mergeWith(RuleSet other) {
		RuleSet merged = new RuleSet();
		merged.setLowercaseOutputName(this.lowercaseOutputName);
		merged.setLowercaseOutputLabel(this.lowercaseOutputLabel);

		Map<String, List<MetricRule>> mergedRules = new LinkedHashMap<>(this.rules);
		if (other != null) {
			mergedRules.putAll(other.rules);
		}
		merged.setRules(mergedRules);
		return merged;
	}
}
