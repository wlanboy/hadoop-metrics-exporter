package com.example.hadoopexporter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.example.hadoopexporter.config.ExporterProperties;
import com.example.hadoopexporter.jmx.JmxClient;
import com.example.hadoopexporter.metrics.MetricFamily;
import com.example.hadoopexporter.metrics.collectors.NameNodeCollector;
import com.example.hadoopexporter.rules.RuleSetLoader;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HadoopMetricCollectorTest {

	private HttpServer server;
	private String url;

	@BeforeEach
	void startServer() throws IOException {
		byte[] body = Files.readAllBytes(Path.of("src/test/resources/fixtures/namenode.json"));
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/jmx", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		url = "http://127.0.0.1:" + server.getAddress().getPort() + "/jmx";
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void collectsNameNodeMetricsFromRealFixture() {
		ExporterProperties properties = new ExporterProperties();
		RuleSetLoader ruleSetLoader = new RuleSetLoader(properties);
		JmxClient jmxClient = new JmxClient();

		NameNodeCollector collector = new NameNodeCollector("hadoop_test", List.of(url), ruleSetLoader, jmxClient);
		List<MetricFamily> families = collector.collect();

		assertThat(families).hasSizeGreaterThan(20);
		assertThat(families.stream().mapToInt(f -> f.getSamples().size()).sum()).isGreaterThan(250);

		// common.yaml: Hadoop:service=.+,name=(JvmMetrics) group + ^(Mem|Gc|Threads)(.+) rule ->
		// combined pattern group1 is the constant "JvmMetrics" capture, so the family name embeds it.
		MetricFamily memFamily = family(families, "hadoop_hdfs_namenode_jvmmetrics_mem");
		assertThat(memFamily.getHelp()).isEqualTo("JvmMetrics metrics");
		assertThat(memFamily.getLabelNames()).containsExactly("cluster", "host", "type");
		assertThat(memFamily.getSamples()).extracting(s -> s.labelValues().get(2))
				.contains("HeapUsedM", "NonHeapUsedM", "MaxM");
		assertThat(memFamily.getSamples().stream()
				.filter(s -> s.labelValues().get(2).equals("HeapUsedM")).findFirst().orElseThrow().value())
				.isEqualTo(270.78455);
		// common labels: "cluster" from config, "host" resolved from the JvmMetrics bean's tag.Hostname
		assertThat(memFamily.getSamples().get(0).labelValues().get(0)).isEqualTo("hadoop_test");
		assertThat(memFamily.getSamples().get(0).labelValues().get(1)).isEqualTo("hdfs-nn");

		// namenode.yaml: FSNamesystemState's FSState field, mapped via hadoop_exporter.mapping.fsstate
		MetricFamily fsState = family(families, "hadoop_hdfs_namenode_fsstate");
		assertThat(fsState.getLabelNames()).containsExactly("cluster", "host");
		assertThat(fsState.getSamples()).hasSize(1);
		assertThat(fsState.getSamples().get(0).value()).isEqualTo(0.0); // "Operational" -> 0.0
	}

	private static MetricFamily family(List<MetricFamily> families, String name) {
		Optional<MetricFamily> found = families.stream().filter(f -> f.getName().equals(name)).findFirst();
		assertThat(found).as("metric family " + name).isPresent();
		return found.get();
	}
}
