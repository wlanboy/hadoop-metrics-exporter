package com.example.hadoopexporter.rules;

import java.util.function.Function;

/**
 * The value-mapping functions a rule's "mapping: hadoop_exporter.mapping.&lt;name&gt;" field
 * may reference. Resolved once from the raw YAML string when a {@link MetricRule} is loaded,
 * rather than re-resolved (and possibly re-thrown) on every metric.
 */
public enum ValueMapping {

	FSSTATE(ValueMappings::fsstate),
	HASTATE(ValueMappings::hastate),
	RMSTATE(ValueMappings::rmstate);

	private final Function<String, Double> function;

	ValueMapping(Function<String, Double> function) {
		this.function = function;
	}

	public double apply(String value) {
		return function.apply(value);
	}

	/** Resolves "hadoop_exporter.mapping.hastate" (or bare "hastate") to a constant. */
	static ValueMapping parse(String raw) {
		String simpleName = raw.contains(".") ? raw.substring(raw.lastIndexOf('.') + 1) : raw;
		for (ValueMapping mapping : values()) {
			if (mapping.name().equalsIgnoreCase(simpleName)) {
				return mapping;
			}
		}
		throw new IllegalArgumentException("Unknown mapping function: " + raw);
	}
}
