package com.ll.demo03;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ProdProfileTests {
    @Test
    void prodExcludesDevelopmentInitializerAndUsesProductionSettings() {
        // 설정과 Bean 조건만 검사한다. DataSource 자동 설정 및 Runner 실행은 하지 않는다.
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod")
                .withBean(MemberService.class, () -> mock(MemberService.class))
                .withBean(ArticleService.class, () -> mock(ArticleService.class))
                .withUserConfiguration(All.class, NotProd.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(All.class).doesNotHaveBean(NotProd.class);
                    assertThat(context).hasBean("initAll").doesNotHaveBean("initNotProd");
                    assertThat(context.getBeansOfType(ApplicationRunner.class)).containsOnlyKeys("initAll");
                    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("prod");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.driver-class-name"))
                            .isEqualTo("org.postgresql.Driver");
                    assertThat(context.getEnvironment().getProperty("spring.h2.console.enabled"))
                            .isEqualTo("false");
                    assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8090");
                    for (String logger : new String[]{"com.ll.demo03", "org.hibernate.SQL",
                            "org.hibernate.orm.jdbc.bind", "org.hibernate.orm.jdbc.extract",
                            "org.springframework.transaction.interceptor"}) {
                        assertThat(context.getEnvironment().getProperty("logging.level." + logger))
                                .isEqualTo("INFO");
                    }
                    verifyNoInteractions(context.getBean(MemberService.class), context.getBean(ArticleService.class));
                });
    }
}
