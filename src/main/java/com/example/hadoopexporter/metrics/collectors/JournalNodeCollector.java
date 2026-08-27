package com.example.hadoopexporter.metrics.collectors;

import java.util.List;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.HadoopMetricCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;

public class JournalNodeCollector extends HadoopMetricCollector {

	public JournalNodeCollector(String cluster, List<String> urls, RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		super(cluster, urls, "hdfs", "journalnode", ruleSetLoader, jmxClient, "JournalNode");
	}
}
