package com.template.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AuthTemplateApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthTemplateApplication.class, args);
	}
}
