package com.msa.common.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.StringUtils;

class MonitoringEnvironmentPostProcessorTest {

    private final MonitoringEnvironmentPostProcessor processor = new MonitoringEnvironmentPostProcessor();

    @Test
    void 기본_모니터링_설정을_적용한다() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("spring.application.name", "order-service")));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,prometheus");
        assertThat(environment.getProperty("management.metrics.tags.application"))
                .isEqualTo("order-service");
        assertThat(environment.getProperty("management.metrics.distribution.percentiles-histogram.http.server.requests"))
                .isEqualTo("true");
        assertThat(environment.getProperty("eureka.instance.metadata-map[prometheus.scrape]")).isEqualTo("true");
        assertThat(environment.getProperty("eureka.instance.metadata-map[prometheus.path]"))
                .isEqualTo("/actuator/prometheus");
        assertThat(environment.getProperty("eureka.instance.prefer-ip-address")).isEqualTo("true");
    }

    @Test
    void 서비스_설정이_기본값보다_우선한다() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("management.endpoints.web.exposure.include", "health")));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(environment.getProperty("management.metrics.tags.application")).isEqualTo("unknown");
    }

    @Test
    void spring_factories_에_등록되어_있다() throws IOException {
        List<String> registered = new ArrayList<>();
        Enumeration<URL> resources = getClass().getClassLoader()
                .getResources(SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION);
        while (resources.hasMoreElements()) {
            Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(resources.nextElement()));
            String value = properties.getProperty(EnvironmentPostProcessor.class.getName());
            if (value != null) {
                registered.addAll(List.of(StringUtils.commaDelimitedListToStringArray(value)));
            }
        }

        assertThat(registered).contains(MonitoringEnvironmentPostProcessor.class.getName());
    }
}
