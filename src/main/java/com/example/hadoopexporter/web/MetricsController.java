package com.example.hadoopexporter.web;

import com.example.hadoopexporter.metrics.PrometheusTextFormatter;
import com.example.hadoopexporter.service.ExporterService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Prometheus text-format scrape endpoint (default: GET /metrics), computed
 * synchronously on every request - equivalent to how prometheus_client invokes each
 * registered collector's collect() on scrape in the Python original.
 */
@RestController
public class MetricsController {

	private static final MediaType PROMETHEUS_TEXT_TYPE =
			MediaType.parseMediaType("text/plain;version=0.0.4;charset=utf-8");

	private final ExporterService exporterService;

	public MetricsController(ExporterService exporterService) {
		this.exporterService = exporterService;
	}

	@GetMapping("${exporter.path:/metrics}")
	public ResponseEntity<String> metrics() {
		String body = PrometheusTextFormatter.format(exporterService.collectAll());
		return ResponseEntity.ok().contentType(PROMETHEUS_TEXT_TYPE).body(body);
	}
}
