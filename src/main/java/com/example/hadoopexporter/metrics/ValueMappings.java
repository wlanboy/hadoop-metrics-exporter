package com.example.hadoopexporter.metrics;

import java.util.Map;
import java.util.function.Function;

/**
 * Port of hadoop_exporter.mapping: named value-mapping functions referenced by
 * a rule's "mapping: hadoop_exporter.mapping.<name>" field.
 */
public final class ValueMappings {

	public static final Map<String, Function<String, Double>> BY_NAME = Map.of(
			"fsstate", ValueMappings::fsstate,
			"hastate", ValueMappings::hastate,
			"rmstate", ValueMappings::rmstate);

	private ValueMappings() {
	}

	public static double fsstate(String value) {
		if ("Operational".equals(value)) {
			return 0.0;
		} else if ("Safemode".equals(value)) {
			return 1.0;
		}
		return 9999.0;
	}

	public static double hastate(String value) {
		if ("initializing".equals(value)) {
			return 0.0;
		} else if ("active".equals(value)) {
			return 1.0;
		} else if ("standby".equals(value)) {
			return 2.0;
		} else if ("stopping".equals(value)) {
			return 3.0;
		}
		return 9999.0;
	}

	public static double rmstate(String value) {
		return hastate(value);
	}

	/** Resolves "hadoop_exporter.mapping.hastate" (or bare "hastate") to a registered function. */
	public static Function<String, Double> resolve(String mapping) {
		String simpleName = mapping.contains(".") ? mapping.substring(mapping.lastIndexOf('.') + 1) : mapping;
		Function<String, Double> fn = BY_NAME.get(simpleName);
		if (fn == null) {
			throw new IllegalArgumentException("Unknown mapping function: " + mapping);
		}
		return fn;
	}
}
