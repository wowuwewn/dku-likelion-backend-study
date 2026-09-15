package com.ll.demo01;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ComponentConfig {
    @Bean
    public com.ll.demo01.ComponentC componentC() {
        return new com.ll.demo01.ComponentC();
    }

    @Bean
    public com.ll.demo01.ComponentD componentD() {
        return new com.ll.demo01.ComponentD();
    }

    @Bean
    public com.ll.demo01.ComponentE componentE() {
        return new com.ll.demo01.ComponentE();
    }
}