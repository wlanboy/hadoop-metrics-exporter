package com.example.hadoopexporter.metrics.collectors;

import java.util.List;
import java.util.Map;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.HadoopMetricCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;

public class NodeManagerCollector extends HadoopMetricCollector {

	public NodeManagerCollector(String cluster, List<String> urls, RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		super(cluster, urls, "yarn", "nodemanager", ruleSetLoader, jmxClient);
	}

	@Override
	protected void addServiceCommonLabels(CommonLabels labels, List<Map<String, Object>> beans) {
		findBean(beans, "Hadoop:service=NodeManager,name=JvmMetrics")
				.ifPresent(bean -> labels.add("host", String.valueOf(bean.get("tag.Hostname"))));
	}
}
