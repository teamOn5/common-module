package com.msa.common.monitoring;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

// 모니터링 설정 주입 (Spring Boot 4)
public class MonitoringEnvironmentPostProcessor implements EnvironmentPostProcessor {

    public static final String PROPERTY_SOURCE_NAME = MonitoringDefaults.PROPERTY_SOURCE_NAME;

    static final Map<String, Object> DEFAULTS = MonitoringDefaults.DEFAULTS;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MonitoringDefaults.apply(environment);
    }
}
