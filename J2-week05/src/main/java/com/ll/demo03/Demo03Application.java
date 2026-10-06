package com.ll.demo03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication

// 엔티티의 생성일, 수정일을 자동으로 기록하는 JPA Auditing 기능을 활성화한다.
@EnableJpaAuditing
public class Demo03Application {

	public static void main(String[] args) {
		SpringApplication.run(Demo03Application.class, args);
	}
}