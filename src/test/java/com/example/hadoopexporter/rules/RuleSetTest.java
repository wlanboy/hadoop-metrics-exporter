package com.example.hadoopexporter.rules;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleSetTest {

	@Test
	void mergeWithTakesLowercaseFlagsFromThisRuleSet() {
		RuleSet service = new RuleSet();
		service.setLowercaseOutputName(false);
		service.setLowercaseOutputLabel(false);

		RuleSet common = new RuleSet();
		common.setLowercaseOutputName(true);
		common.setLowercaseOutputLabel(true);

		RuleSet merged = service.mergeWith(common);

		assertThat(merged.isLowercaseOutputName()).isFalse();
		assertThat(merged.isLowercaseOutputLabel()).isFalse();
	}

	@Test
	void mergeWithUnionsRuleGroupsFromBoth() {
		RuleSet service = new RuleSet();
		MetricRule serviceRule = new MetricRule();
		service.setRules(mapOf("^ServiceGroup$", List.of(serviceRule)));

		RuleSet common = new RuleSet();
		MetricRule commonRule = new MetricRule();
		common.setRules(mapOf("^CommonGroup$", List.of(commonRule)));

		RuleSet merged = service.mergeWith(common);

		assertThat(merged.getRules()).containsOnlyKeys("^ServiceGroup$", "^CommonGroup$");
		assertThat(merged.getRules().get("^ServiceGroup$")).containsExactly(serviceRule);
		assertThat(merged.getRules().get("^CommonGroup$")).containsExactly(commonRule);
	}

	@Test
	void mergeWithLetsOtherWinOnSharedGroupPattern() {
		RuleSet service = new RuleSet();
		MetricRule serviceRule = new MetricRule();
		service.setRules(mapOf("^SharedGroup$", List.of(serviceRule)));

		RuleSet common = new RuleSet();
		MetricRule commonRule = new MetricRule();
		common.setRules(mapOf("^SharedGroup$", List.of(commonRule)));

		RuleSet merged = service.mergeWith(common);

		assertThat(merged.getRules().get("^SharedGroup$")).containsExactly(commonRule);
	}

	@Test
	void mergeWithToleratesNullOther() {
		RuleSet service = new RuleSet();
		MetricRule serviceRule = new MetricRule();
		service.setRules(mapOf("^ServiceGroup$", List.of(serviceRule)));

		RuleSet merged = service.mergeWith(null);

		assertThat(merged.getRules()).containsOnlyKeys("^ServiceGroup$");
	}

	private static Map<String, List<MetricRule>> mapOf(String key, List<MetricRule> value) {
		return new java.util.LinkedHashMap<>(Map.of(key, value));
	}
}
