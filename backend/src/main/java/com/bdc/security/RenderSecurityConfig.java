package com.bdc.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@Profile("render")
public class RenderSecurityConfig {
 @Bean UserDetailsService demoUser(@Value("${DEMO_USERNAME:demo}") String username,@Value("${DEMO_PASSWORD}") String password){
  if(password.length()<16)throw new IllegalArgumentException("DEMO_PASSWORD must contain at least 16 characters.");
  return new InMemoryUserDetailsManager(User.withUsername(username)
    .password(new BCryptPasswordEncoder().encode(password)).roles("DEMO").build());
 }
 @Bean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain renderChain(HttpSecurity http)throws Exception{
  var entryPoints=new java.util.LinkedHashMap<org.springframework.security.web.util.matcher.RequestMatcher,org.springframework.security.web.AuthenticationEntryPoint>();
  entryPoints.put(request->request.getRequestURI().startsWith(request.getContextPath()+"/api/"),new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
  var entryPoint=new org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint(entryPoints);
  entryPoint.setDefaultEntryPoint(new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login"));
  return http.authorizeHttpRequests(a->a.requestMatchers("/api/health","/error").permitAll().anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/",true).permitAll())
   .exceptionHandling(e->e.authenticationEntryPoint(entryPoint))
   .logout(l->l.logoutSuccessUrl("/login?logout")).build();
 }
}
