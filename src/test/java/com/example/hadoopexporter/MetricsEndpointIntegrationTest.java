package com.example.hadoopexporter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MetricsEndpointIntegrationTest {

	private static HttpServer jmxServer;

	@LocalServerPort
	private int port;

	@BeforeAll
	static void startFakeJmxServer() throws IOException {
		byte[] body = Files.readAllBytes(Path.of("src/test/resources/fixtures/namenode.json"));
		jmxServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		jmxServer.createContext("/jmx", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		jmxServer.start();
	}

	@AfterAll
	static void stopFakeJmxServer() {
		jmxServer.stop(0);
	}

	@DynamicPropertySource
	static void exporterProperties(DynamicPropertyRegistry registry) {
		registry.add("exporter.jmx[0].cluster", () -> "hadoop_it");
		registry.add("exporter.jmx[0].services.namenode[0]",
				() -> "http://127.0.0.1:" + jmxServer.getAddress().getPort() + "/jmx");
	}

	@Test
	void metricsEndpointServesConvertedPrometheusText() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/metrics")).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type")).isPresent().get(org.assertj.core.api.InstanceOfAssertFactories.STRING)
				.startsWith("text/plain");
		assertThat(response.body()).contains("hadoop_it").contains("hdfs-nn")
				.contains("hadoop_hdfs_namenode_jvmmetrics_mem");
	}
}
