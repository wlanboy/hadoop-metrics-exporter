package com.example.hadoopexporter.metrics.collectors;

import java.util.List;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.HadoopMetricCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;

public class NameNodeCollector extends HadoopMetricCollector {

	public NameNodeCollector(String cluster, List<String> urls, RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		super(cluster, urls, "hdfs", "namenode", ruleSetLoader, jmxClient, "NameNode");
	}
}
