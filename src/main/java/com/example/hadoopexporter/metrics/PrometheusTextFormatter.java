package com.example.hadoopexporter.metrics;

import java.util.List;

/**
 * Renders MetricFamily instances as Prometheus text exposition format (version 0.0.4).
 */
public final class PrometheusTextFormatter {

	private PrometheusTextFormatter() {
	}

	public static String format(List<MetricFamily> families) {
		StringBuilder sb = new StringBuilder();
		for (MetricFamily family : families) {
			if (family.getSamples().isEmpty()) {
				continue;
			}
			String help = family.getHelp() != null ? family.getHelp() : family.getName();
			sb.append("# HELP ").append(family.getName()).append(' ').append(escapeHelp(help)).append('\n');
			sb.append("# TYPE ").append(family.getName()).append(" gauge\n");
			for (MetricFamily.Sample sample : family.getSamples()) {
				sb.append(family.getName());
				List<String> labelNames = family.getLabelNames();
				if (!labelNames.isEmpty()) {
					sb.append('{');
					for (int i = 0; i < labelNames.size(); i++) {
						if (i > 0) {
							sb.append(',');
						}
						sb.append(labelNames.get(i)).append("=\"").append(escapeLabelValue(sample.labelValues().get(i))).append('"');
					}
					sb.append('}');
				}
				sb.append(' ').append(formatValue(sample.value())).append('\n');
			}
		}
		return sb.toString();
	}

	private static String formatValue(double value) {
		if (Double.isNaN(value)) {
			return "NaN";
		}
		if (Double.isInfinite(value)) {
			return value > 0 ? "+Inf" : "-Inf";
		}
		if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e17) {
			return Long.toString((long) value);
		}
		return Double.toString(value);
	}

	private static String escapeHelp(String s) {
		return s.replace("\\", "\\\\").replace("\n", "\\n");
	}

	private static String escapeLabelValue(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}
}
