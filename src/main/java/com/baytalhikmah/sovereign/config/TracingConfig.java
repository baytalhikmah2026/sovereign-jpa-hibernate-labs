package com.baytalhikmah.sovereign.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.baytalhikmah.sovereign.persistence.SqlSpanListener;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * With the profile tracing: an OpenTelemetry tracer whose spans go to Jaeger over OTLP (and to any other
 * SpanExporter bean), and the P6Spy listener that records every SQL statement as a span.
 */
@Configuration(proxyBeanMethods = false)
@Profile("tracing")
public class TracingConfig {

    public static final String INSTRUMENTATION_NAME = "com.baytalhikmah.sovereign";

    @Bean(destroyMethod = "close")
    SdkTracerProvider sovereignTracerProvider(ObjectProvider<SpanExporter> extraExporters,
            @Value("${sovereign.tracing.otlp-endpoint}") String otlpEndpoint,
            @Value("${sovereign.tracing.service-name}") String serviceName) {
        List<SpanExporter> exporters = new ArrayList<>(extraExporters.orderedStream().toList());
        exporters.add(OtlpHttpSpanExporter.builder().setEndpoint(otlpEndpoint).build());
        Resource service = Resource.getDefault()
            .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)));
        return SdkTracerProvider.builder()
            .setResource(service)
            .addSpanProcessor(BatchSpanProcessor.builder(SpanExporter.composite(exporters)).build())
            .build();
    }

    @Bean
    Tracer sovereignTracer(SdkTracerProvider sovereignTracerProvider) {
        return sovereignTracerProvider.get(INSTRUMENTATION_NAME);
    }

    @Bean
    SqlSpanListener sqlSpanListener(Tracer sovereignTracer) {
        return new SqlSpanListener(sovereignTracer);
    }

}
