package io.basicspring.basic_spring_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.opentelemetry.api.common.Attributes;

@SpringBootApplication
public class BasicSpringAppApplication {

	private static final String SERVICE_NAME = "basic-spring-service";
	private static OpenTelemetry openTelemetry;

	public static void main(String[] args) {
		initializeOpenTelemetry();
		SpringApplication.run(BasicSpringAppApplication.class, args);
	}

	private static void initializeOpenTelemetry() {
		// Create a resource with service name for identifying the service in traces
		Resource resource = Resource.getDefault()
				.merge(Resource.create(
						Attributes.of(ResourceAttributes.SERVICE_NAME, SERVICE_NAME)));

		// Set up the OpenTelemetry exporter (OTLP in this case)
		OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
				.setEndpoint("http://localhost:4317") // Default OpenTelemetry Collector endpoint
				.build();

		// Create a tracer provider with the span processor and exporter
		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
				.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
				.setResource(resource)
				.build();

		openTelemetry = OpenTelemetrySdk.builder()
				.setTracerProvider(sdkTracerProvider)
				.buildAndRegisterGlobal();
	}

	@RestController
	class HelloWorldController {
		private final Tracer tracer = GlobalOpenTelemetry.getTracer("example-tracer");

		@GetMapping("/hello")
		public String hello() {
			Span span = tracer.spanBuilder("hello-span")
					.setSpanKind(SpanKind.SERVER)
					.startSpan();

			try (Scope scope = span.makeCurrent()) {
				return "Hello World!";
			} finally {
				span.end();
			}
		}
	}
}
