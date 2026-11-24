package com.msa.common.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.msa.common.web.exception.GlobalExceptionHandler;

class CommonWebAutoConfigurationTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class));

    @Test
    void 서블릿_웹_애플리케이션이면_GlobalExceptionHandler_를_등록한다() {
        webContextRunner.run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    void 사용자_정의_빈이_있으면_등록하지_않는다() {
        webContextRunner.withUserConfiguration(CustomHandlerConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
                    assertThat(context.getBean(GlobalExceptionHandler.class)).isInstanceOf(CustomHandler.class);
                });
    }

    @Test
    void 웹_애플리케이션이_아니면_등록하지_않는다() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Test
    void AutoConfiguration_imports_에_등록되어_있다() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(CommonWebAutoConfiguration.class.getName());
    }

    static class CustomHandler extends GlobalExceptionHandler {
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomHandlerConfig {

        @Bean
        GlobalExceptionHandler customHandler() {
            return new CustomHandler();
        }
    }
}
