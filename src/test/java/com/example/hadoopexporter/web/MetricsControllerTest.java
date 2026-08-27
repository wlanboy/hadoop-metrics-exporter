package com.example.hadoopexporter.web;

import java.util.List;

import com.example.hadoopexporter.metrics.MetricFamily;
import com.example.hadoopexporter.service.ExporterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsControllerTest {

	@Mock
	private ExporterService exporterService;

	@Test
	void rendersCollectedFamiliesAsPrometheusTextWithOkStatus() {
		MetricFamily family = new MetricFamily("hadoop_test_metric", "help", List.of());
		family.addMetric(List.of(), 1.0);
		when(exporterService.collectAll()).thenReturn(List.of(family));

		MetricsController controller = new MetricsController(exporterService);
		ResponseEntity<String> response = controller.metrics();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType())
				.isEqualTo(MediaType.parseMediaType("text/plain;version=0.0.4;charset=utf-8"));
		assertThat(response.getBody()).contains("hadoop_test_metric 1\n");
	}

	@Test
	void rendersEmptyBodyWhenNothingWasCollected() {
		when(exporterService.collectAll()).thenReturn(List.of());

		MetricsController controller = new MetricsController(exporterService);
		ResponseEntity<String> response = controller.metrics();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEmpty();
	}
}
