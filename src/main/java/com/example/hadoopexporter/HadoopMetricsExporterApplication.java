package com.example.hadoopexporter;

import com.example.hadoopexporter.config.ExporterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ExporterProperties.class)
public class HadoopMetricsExporterApplication {

	public static void main(String[] args) {
		SpringApplication.run(HadoopMetricsExporterApplication.class, args);
	}

}
