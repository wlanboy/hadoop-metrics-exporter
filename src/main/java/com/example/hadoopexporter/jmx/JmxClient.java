package com.example.hadoopexporter.jmx;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches the "beans" array from a Hadoop JMX HTTP endpoint (e.g. http://host:9870/jmx),
 * equivalent to hadoop_exporter.utils.get_metrics.
 */
@Component
public class JmxClient {

	private static final Logger log = LoggerFactory.getLogger(JmxClient.class);
	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private final RestClient restClient;

	public JmxClient() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) TIMEOUT.toMillis());
		requestFactory.setReadTimeout((int) TIMEOUT.toMillis());
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	public List<Map<String, Object>> getBeans(String url) {
		try {
			JmxResponse response = restClient.get()
					.uri(url)
					.retrieve()
					.body(JmxResponse.class);
			if (response == null || response.beans() == null) {
				log.warn("no metrics get in the {}.", url);
				return List.of();
			}
			return response.beans();
		} catch (Exception e) {
			log.warn("Can't scrape metrics from url: {} ({})", url, e.getMessage());
			return List.of();
		}
	}

	private record JmxResponse(List<Map<String, Object>> beans) {
	}
}
