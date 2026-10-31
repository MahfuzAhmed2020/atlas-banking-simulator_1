package com.atlas.banking;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Configuration @EnableMethodSecurity
class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
 @Bean UserDetailsService userDetailsService(Users users) {
  return username -> {
   BankUser u=users.findByEmailIgnoreCase(username).orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
   return User.withUsername(u.email).password(u.passwordHash)
     .authorities(List.of(new SimpleGrantedAuthority("ROLE_"+u.role))).disabled(!u.active).build();
  };
 }
 @Bean SecurityFilterChain filterChain(HttpSecurity http, CodesService codes) throws Exception {
  http
    .csrf(csrf->csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
      .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler()))
    .authorizeHttpRequests(auth->auth
      .requestMatchers("/api/auth/register","/api/auth/verify-email","/api/auth/password/request","/api/auth/password/reset","/api/auth/csrf","/api/health").permitAll()
      .requestMatchers("/api/auth/**").authenticated()
      .requestMatchers("/api/admin/**","/v3/api-docs/**","/v3/api-docs.yaml","/swagger-ui/**","/swagger-ui.html").hasRole("ADMIN")
      .requestMatchers("/api/employee/**").hasAnyRole("EMPLOYEE","ADMIN")
      .requestMatchers("/api/**").authenticated()
      .anyRequest().permitAll())
    .formLogin(form->form.loginProcessingUrl("/api/auth/login")
      .usernameParameter("email").passwordParameter("password")
      .successHandler((req,res,authentication)-> {
       req.getSession(true).setAttribute("MFA_OK",false);
       codes.deliver(authentication.getName(),"LOGIN");
       res.setStatus(200); res.setContentType("application/json");
       res.getWriter().write("{\"message\":\"Enter the 6-digit code sent to the registered email\",\"mfaRequired\":true}");
      })
      .failureHandler((req,res,ex)-> {
       res.setStatus(401);res.setContentType("application/json");
       res.getWriter().write("{\"error\":\"Invalid credentials\"}");
      }))
    .logout(logout->logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
      .logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
    .exceptionHandling(errors->errors.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Sign in required\"}");}).accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Role or CSRF check failed\"}");}));
  http.addFilterAfter(new MfaGate(),org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
 static class MfaGate extends OncePerRequestFilter {
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
   String path=request.getRequestURI();
   if(((path.startsWith("/api/") && !path.startsWith("/api/auth/") && !path.equals("/api/health")) || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs"))) {
    Authentication a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if(a!=null && a.isAuthenticated() && !(a instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
       && !Boolean.TRUE.equals(request.getSession(false)==null?null:request.getSession(false).getAttribute("MFA_OK"))) {
      response.setStatus(403);response.setContentType("application/json");response.getWriter().write("{\"error\":\"MFA_REQUIRED\"}");return;
    }
   }
   chain.doFilter(request,response);
  }
 }
}
