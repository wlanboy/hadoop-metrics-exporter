package com.example.hadoopexporter.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueMappingTest {

	@Test
	void parseResolvesFullyQualifiedName() {
		assertThat(ValueMapping.parse("hadoop_exporter.mapping.fsstate")).isEqualTo(ValueMapping.FSSTATE);
		assertThat(ValueMapping.parse("hadoop_exporter.mapping.hastate")).isEqualTo(ValueMapping.HASTATE);
		assertThat(ValueMapping.parse("hadoop_exporter.mapping.rmstate")).isEqualTo(ValueMapping.RMSTATE);
	}

	@Test
	void parseResolvesBareNameCaseInsensitively() {
		assertThat(ValueMapping.parse("fsstate")).isEqualTo(ValueMapping.FSSTATE);
		assertThat(ValueMapping.parse("FSSTATE")).isEqualTo(ValueMapping.FSSTATE);
		assertThat(ValueMapping.parse("HaState")).isEqualTo(ValueMapping.HASTATE);
	}

	@Test
	void parseThrowsForUnknownMapping() {
		assertThatThrownBy(() -> ValueMapping.parse("hadoop_exporter.mapping.unknown"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown mapping function");
	}

	@Test
	void fsstateMapsKnownAndUnknownValues() {
		assertThat(ValueMapping.FSSTATE.apply("Operational")).isEqualTo(0.0);
		assertThat(ValueMapping.FSSTATE.apply("Safemode")).isEqualTo(1.0);
		assertThat(ValueMapping.FSSTATE.apply("Bogus")).isEqualTo(9999.0);
	}

	@Test
	void hastateMapsAllKnownStates() {
		assertThat(ValueMapping.HASTATE.apply("initializing")).isEqualTo(0.0);
		assertThat(ValueMapping.HASTATE.apply("active")).isEqualTo(1.0);
		assertThat(ValueMapping.HASTATE.apply("standby")).isEqualTo(2.0);
		assertThat(ValueMapping.HASTATE.apply("stopping")).isEqualTo(3.0);
		assertThat(ValueMapping.HASTATE.apply("bogus")).isEqualTo(9999.0);
	}

	@Test
	void rmstateDelegatesToHastate() {
		assertThat(ValueMapping.RMSTATE.apply("active")).isEqualTo(ValueMapping.HASTATE.apply("active"));
		assertThat(ValueMapping.RMSTATE.apply("standby")).isEqualTo(ValueMapping.HASTATE.apply("standby"));
	}
}
