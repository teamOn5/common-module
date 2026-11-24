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
        defaults.put("management.metrics.distribution.percentiles-histogram.http.server.requests", "true");
        // Prometheus eureka_sd_configs 가 수집 대상을 찾을 수 있도록 Eureka metadata 등록
        defaults.put("eureka.instance.metadata-map[prometheus.scrape]", "true");
        defaults.put("eureka.instance.metadata-map[prometheus.path]", "/actuator/prometheus");
        // 컨테이너 환경에서 Prometheus 가 IP 로 접근하도록
        defaults.put("eureka.instance.prefer-ip-address", "true");
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
