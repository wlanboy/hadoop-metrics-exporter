package com.example.hadoopexporter.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.rules.MetricRule;
import com.example.hadoopexporter.rules.RuleSet;
import com.example.hadoopexporter.rules.RuleSetLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of hadoop_exporter.common.MetricCollector: consumes JMX beans from one or more
 * urls belonging to a single service (e.g. namenode) and converts them into Prometheus
 * gauge families using the regex-based rules loaded for that service.
 *
 * <p>The bean name and each of its field names are matched against the rule's regex
 * patterns; on a match a combined "{groupPattern}&lt;&gt;{metricPattern}" regex is used
 * to substitute the metric name/help/label templates ($1, $2, ...) against the
 * concatenated "{beanName}&lt;&gt;{metricName}" string - exactly like the Python original
 * builds a combined regex and uses backreference substitution.
 */
public abstract class HadoopMetricCollector {

	private static final Set<String> NON_METRIC_NAMES = Set.of("name", "modelerType", "Name", "ObjectName");

	private final Logger log;
	private final String cluster;
	private final List<String> urls;
	private final String prefix;
	private final RuleSet ruleSet;
	private final JmxClient jmxClient;

	protected HadoopMetricCollector(String cluster, List<String> urls, String component, String service,
			RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		this.log = LoggerFactory.getLogger(component + "." + service);
		this.cluster = cluster;
		this.urls = urls.stream().map(HadoopMetricCollector::stripTrailingSlash).toList();
		this.prefix = "hadoop_" + component + "_" + service;
		this.ruleSet = ruleSetLoader.loadForService(service);
		this.jmxClient = jmxClient;
	}

	public List<MetricFamily> collect() {
		Map<String, Map<String, MetricFamily>> metrics = new LinkedHashMap<>();
		for (String groupPattern : ruleSet.getRules().keySet()) {
			metrics.put(groupPattern, new LinkedHashMap<>());
		}

		for (String url : urls) {
			List<Map<String, Object>> beans = jmxClient.getBeans(url);

			CommonLabels commonLabels = new CommonLabels();
			commonLabels.add("cluster", cluster);
			addServiceCommonLabels(commonLabels, beans);

			convertMetrics(beans, commonLabels, metrics);
		}

		List<MetricFamily> result = new ArrayList<>();
		for (Map<String, MetricFamily> groupMetrics : metrics.values()) {
			result.addAll(groupMetrics.values());
		}
		return result;
	}

	/**
	 * Override to append service-specific common labels (e.g. "host" resolved from the
	 * service's JvmMetrics bean); the "cluster" label is already populated.
	 */
	protected void addServiceCommonLabels(CommonLabels labels, List<Map<String, Object>> beans) {
		// base implementation: nothing beyond "cluster"
	}

	protected static Optional<Map<String, Object>> findBean(List<Map<String, Object>> beans, String groupPattern) {
		Pattern pattern = Pattern.compile(groupPattern);
		for (Map<String, Object> bean : beans) {
			Object name = bean.get("name");
			if (name instanceof String s && pattern.matcher(s).lookingAt()) {
				return Optional.of(bean);
			}
		}
		return Optional.empty();
	}

	private void convertMetrics(List<Map<String, Object>> beans, CommonLabels commonLabels,
			Map<String, Map<String, MetricFamily>> metrics) {
		for (Map<String, Object> bean : beans) {
			Object beanNameObj = bean.get("name");
			if (!(beanNameObj instanceof String beanName)) {
				continue;
			}
			for (Map.Entry<String, List<MetricRule>> ruleEntry : ruleSet.getRules().entrySet()) {
				String groupPattern = ruleEntry.getKey();
				if (!Pattern.compile(groupPattern).matcher(beanName).lookingAt()) {
					continue;
				}
				Map<String, MetricFamily> groupMetrics = metrics.get(groupPattern);
				for (Map.Entry<String, Object> metricEntry : bean.entrySet()) {
					String metricName = metricEntry.getKey();
					if (NON_METRIC_NAMES.contains(metricName)) {
						continue;
					}
					Object metricValue = metricEntry.getValue();
					for (MetricRule rule : ruleEntry.getValue()) {
						if (!"GAUSE".equals(rule.getType())) {
							log.warn("Metric type {} not supported currently", rule.getType());
							continue;
						}
						if (!Pattern.compile(rule.getPattern()).matcher(metricName).lookingAt()) {
							continue;
						}
						applyRule(groupPattern, groupMetrics, rule, beanName, metricName, metricValue, commonLabels);
						break;
					}
				}
			}
		}
	}

	private void applyRule(String groupPattern, Map<String, MetricFamily> groupMetrics, MetricRule rule,
			String beanName, String metricName, Object metricValue, CommonLabels commonLabels) {
		String combinedPatternStr = rstripDollar(groupPattern) + "<>" + lstripCaret(rule.getPattern());
		Pattern combined = Pattern.compile(combinedPatternStr);
		String concatStr = beanName + "<>" + metricName;

		String subName = combined.matcher(concatStr).replaceAll(rule.getName());
		List<String> subLabelNames = new ArrayList<>(rule.getLabels().keySet());

		List<String> idParts = new ArrayList<>();
		idParts.add(subName);
		List<String> sortedLabelNames = new ArrayList<>(subLabelNames);
		sortedLabelNames.sort(String::compareTo);
		idParts.addAll(sortedLabelNames);
		String metricIdentifier = String.join("_", idParts).toLowerCase(Locale.ROOT);

		MetricFamily family = groupMetrics.get(metricIdentifier);
		if (family == null) {
			String name = prefix + "_" + subName;
			if (ruleSet.isLowercaseOutputName()) {
				name = name.toLowerCase(Locale.ROOT);
			}
			List<String> labelNames = new ArrayList<>(commonLabels.names);
			labelNames.addAll(subLabelNames);
			if (ruleSet.isLowercaseOutputLabel()) {
				labelNames.replaceAll(l -> l.toLowerCase(Locale.ROOT));
			}
			String help = rule.getHelp() == null ? name : combined.matcher(concatStr).replaceAll(rule.getHelp());
			family = new MetricFamily(name, help, labelNames);
			groupMetrics.put(metricIdentifier, family);
		}

		List<String> subLabelValues = new ArrayList<>();
		for (String labelTemplate : rule.getLabels().values()) {
			subLabelValues.add(combined.matcher(concatStr).replaceAll(labelTemplate));
		}
		List<String> labelValues = new ArrayList<>(commonLabels.values);
		labelValues.addAll(subLabelValues);
		if (ruleSet.isLowercaseOutputLabel()) {
			labelValues.replaceAll(l -> l.toLowerCase(Locale.ROOT));
		}

		try {
			double resolvedValue = resolveValue(metricValue, rule.getMapping());
			family.addMetric(labelValues, resolvedValue);
		} catch (Exception e) {
			log.warn("Unparseable metric: {} - {} = {}", beanName, metricName, metricValue);
		}
	}

	private static double resolveValue(Object value, String mapping) {
		if (mapping != null && !mapping.isBlank()) {
			Function<String, Double> fn = ValueMappings.resolve(mapping);
			return fn.apply(String.valueOf(value));
		}
		return toDouble(value);
	}

	private static double toDouble(Object value) {
		if (value instanceof Number n) {
			return n.doubleValue();
		}
		if (value instanceof Boolean b) {
			return b ? 1.0 : 0.0;
		}
		if (value instanceof String s) {
			return Double.parseDouble(s);
		}
		throw new IllegalArgumentException("Cannot convert to number: " + value);
	}

	private static String stripTrailingSlash(String url) {
		int end = url.length();
		while (end > 0 && url.charAt(end - 1) == '/') {
			end--;
		}
		return url.substring(0, end);
	}

	private static String rstripDollar(String s) {
		int end = s.length();
		while (end > 0 && s.charAt(end - 1) == '$') {
			end--;
		}
		return s.substring(0, end);
	}

	private static String lstripCaret(String s) {
		int start = 0;
		while (start < s.length() && s.charAt(start) == '^') {
			start++;
		}
		return s.substring(start);
	}

	public static class CommonLabels {
		private final List<String> names = new ArrayList<>();
		private final List<String> values = new ArrayList<>();

		public void add(String name, String value) {
			names.add(name);
			values.add(value);
		}
	}
}
