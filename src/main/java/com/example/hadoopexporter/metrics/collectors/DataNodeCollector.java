package com.example.hadoopexporter.metrics.collectors;

import java.util.List;

import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.HadoopMetricCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;

public class DataNodeCollector extends HadoopMetricCollector {

	public DataNodeCollector(String cluster, List<String> urls, RuleSetLoader ruleSetLoader, JmxClient jmxClient) {
		super(cluster, urls, "hdfs", "datanode", ruleSetLoader, jmxClient, "DataNode");
	}
}
