package com.example.hadoopexporter.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.hadoopexporter.config.ExporterProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class RuleSetLoaderTest {

	@Test
	void loadForServiceMergesServiceRulesWithCommonRulesFromClasspath() {
		RuleSetLoader loader = new RuleSetLoader(new ExporterProperties());

		RuleSet ruleSet = loader.loadForService("namenode");

		assertThat(ruleSet.getRules().keySet()).anyMatch(key -> key.contains("FSNamesystemState"));
		// common.yaml supplies the JvmMetrics group shared by every service.
		assertThat(ruleSet.getRules().keySet()).anyMatch(key -> key.contains("JvmMetrics"));
	}

	@Test
	void loadForServiceReturnsCommonRulesOnlyWhenServiceFileIsMissing() {
		RuleSetLoader loader = new RuleSetLoader(new ExporterProperties());

		RuleSet ruleSet = loader.loadForService("does-not-exist");

		assertThat(ruleSet.getRules()).isNotEmpty();
		assertThat(ruleSet.getRules().keySet()).allMatch(key -> key.contains("JvmMetrics"));
	}

	@Test
	void loadForServiceReadsFromConfiguredMetricsDirWhenSet(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("myservice.yaml"), """
				lowercaseOutputName: false
				rules:
				  "^MyBean$":
				    - pattern: "^MyMetric$"
				      type: GAUSE
				      name: "my_metric"
				""");

		ExporterProperties properties = new ExporterProperties();
		properties.setMetricsDir(tempDir.toString());
		RuleSetLoader loader = new RuleSetLoader(properties);

		RuleSet ruleSet = loader.loadForService("myservice");

		assertThat(ruleSet.isLowercaseOutputName()).isFalse();
		assertThat(ruleSet.getRules()).containsKey("^MyBean$");
		assertThat(ruleSet.getRules().get("^MyBean$")).hasSize(1);
		assertThat(ruleSet.getRules().get("^MyBean$").get(0).getName()).isEqualTo("my_metric");
	}

	@Test
	void loadForServiceReturnsEmptyRuleSetWhenNeitherFileExistsInConfiguredDir(@TempDir Path tempDir) {
		ExporterProperties properties = new ExporterProperties();
		properties.setMetricsDir(tempDir.toString());
		RuleSetLoader loader = new RuleSetLoader(properties);

		RuleSet ruleSet = loader.loadForService("missing");

		assertThat(ruleSet.getRules()).isEmpty();
	}
}
