package com.example.hadoopexporter.rules;

/**
 * Port of hadoop_exporter.mapping: the value-mapping functions a {@link ValueMapping} constant
 * dispatches to.
 */
final class ValueMappings {

	private ValueMappings() {
	}

	static double fsstate(String value) {
		if ("Operational".equals(value)) {
			return 0.0;
		} else if ("Safemode".equals(value)) {
			return 1.0;
		}
		return 9999.0;
	}

	static double hastate(String value) {
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

	static double rmstate(String value) {
		return hastate(value);
	}
}
