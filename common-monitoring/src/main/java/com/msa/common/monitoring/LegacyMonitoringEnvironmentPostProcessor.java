package com.msa.common.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

// 모니터링 설정 주입 (Spring Boot 3)
// Boot 3 는 org.springframework.boot.env.EnvironmentPostProcessor 키만 읽는다.
// Boot 4 에서도 이 키가 (deprecated 로) 읽히지만, MonitoringDefaults.apply 가 중복 등록을 막는다.
@SuppressWarnings("removal")
public class LegacyMonitoringEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MonitoringDefaults.apply(environment);
    }
}
