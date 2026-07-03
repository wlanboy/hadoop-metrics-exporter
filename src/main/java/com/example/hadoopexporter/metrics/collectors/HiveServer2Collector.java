package com.example.hadoopexporter.metrics.collectors;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.HadoopMetricCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;

public class HiveServer2Collector extends HadoopMetricCollector {

	private static final Pattern HOST_NAME_PATTERN = Pattern.compile(".*hostName=(.+),.*");

	public HiveServer2Collector(String cluster, List<String> urls, RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		super(cluster, urls, "hive", "hiveserver2", ruleSetLoader, jmxClient);
	}

	@Override
	protected void addServiceCommonLabels(CommonLabels labels, List<Map<String, Object>> beans) {
		findBean(beans, "org.apache.logging.log4j2:type=AsyncContext@(\\w{8})$").ifPresent(bean -> {
			Object configProperties = bean.get("ConfigProperties");
			if (configProperties instanceof String s) {
				Matcher matcher = HOST_NAME_PATTERN.matcher(s);
				if (matcher.matches()) {
					labels.add("host", matcher.group(1));
				}
			}
		});
	}
}
