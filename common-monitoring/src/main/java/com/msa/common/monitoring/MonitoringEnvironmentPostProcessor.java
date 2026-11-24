package com.msa.common.monitoring;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

// 모니터링 설정 주입
public class MonitoringEnvironmentPostProcessor implements EnvironmentPostProcessor {

    public static final String PROPERTY_SOURCE_NAME = "commonMonitoringDefaults";

    static final Map<String, Object> DEFAULTS;

    static {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("management.endpoints.web.exposure.include", "health,info,prometheus");
        defaults.put("management.metrics.tags.application", "${spring.application.name:unknown}");
        DEFAULTS = Map.copyOf(defaults);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
    }
}
