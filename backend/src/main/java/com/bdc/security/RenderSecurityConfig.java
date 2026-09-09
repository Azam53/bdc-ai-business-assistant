package com.bdc.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Public portfolio demo. Retain CSRF protection for browser requests. */
@Configuration
@Profile("render")
public class RenderSecurityConfig {
 @Bean SecurityFilterChain renderChain(HttpSecurity http)throws Exception{
  return http.authorizeHttpRequests(a->a.anyRequest().permitAll()).build();
 }
}
