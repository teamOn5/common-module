package com.msa.common.monitoring;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

// 모니터링 기본값 (Spring Boot 3/4 공용, Boot 버전별 API 를 참조하지 않는다)
final class MonitoringDefaults {

    static final String PROPERTY_SOURCE_NAME = "commonMonitoringDefaults";

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

    private MonitoringDefaults() {
    }

    // 최저 우선순위로 추가 (서비스 설정이 항상 우선). 이미 있으면 건너뛴다.
    static void apply(ConfigurableEnvironment environment) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
    }
}
