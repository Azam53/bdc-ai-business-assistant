package com.bdc.security;
import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.web.SecurityFilterChain;
/** Local single-user demo. Add OIDC, roles, tenant ownership and CSRF protection before public deployment. */
@Configuration @Profile("!render") public class SecurityConfig {
 @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().permitAll()).headers(h->h.frameOptions(f->f.deny())).build();}
}
