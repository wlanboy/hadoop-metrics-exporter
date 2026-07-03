package com.example.hadoopexporter.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.hadoopexporter.config.ExporterProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

/**
 * Loads and merges a service's metrics/{service}.yaml rule file with the shared
 * metrics/common.yaml file, mirroring hadoop_exporter.common.MetricCollector.__init__:
 * rules = service.rules; rules.update(common.rules); lowercase flags come from the service file only.
 */
@Component
public class RuleSetLoader {

	private static final String COMMON_FILE = "common.yaml";

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	private final ExporterProperties properties;

	public RuleSetLoader(ExporterProperties properties) {
		this.properties = properties;
	}

	public RuleSet loadForService(String serviceName) {
		RuleSet serviceRules = readRuleFile(serviceName + ".yaml");
		RuleSet commonRules = readRuleFile(COMMON_FILE);

		RuleSet merged = new RuleSet();
		merged.setLowercaseOutputName(serviceRules != null ? serviceRules.isLowercaseOutputName() : true);
		merged.setLowercaseOutputLabel(serviceRules != null ? serviceRules.isLowercaseOutputLabel() : true);

		Map<String, List<MetricRule>> rules = new LinkedHashMap<>();
		if (serviceRules != null) {
			rules.putAll(serviceRules.getRules());
		}
		if (commonRules != null) {
			rules.putAll(commonRules.getRules());
		}
		merged.setRules(rules);
		return merged;
	}

	private RuleSet readRuleFile(String fileName) {
		try (InputStream in = openStream(fileName)) {
			if (in == null) {
				return null;
			}
			return yamlMapper.readValue(in, RuleSet.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load metrics rule file: " + fileName, e);
		}
	}

	private InputStream openStream(String fileName) throws IOException {
		String metricsDir = properties.getMetricsDir();
		if (metricsDir != null && !metricsDir.isBlank()) {
			Path path = Path.of(metricsDir, fileName);
			if (Files.exists(path)) {
				return Files.newInputStream(path);
			}
			return null;
		}
		return getClass().getClassLoader().getResourceAsStream("metrics/" + fileName);
	}
}
