package com.example.hadoopexporter.metrics.collectors;

import java.util.List;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.HadoopMetricCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;

public class NodeManagerCollector extends HadoopMetricCollector {

	public NodeManagerCollector(String cluster, List<String> urls, RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		super(cluster, urls, "yarn", "nodemanager", ruleSetLoader, jmxClient, "NodeManager");
	}
}
