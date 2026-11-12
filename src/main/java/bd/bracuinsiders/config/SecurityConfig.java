package bd.bracuinsiders.config;

import bd.bracuinsiders.service.RateGuard;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwords() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService users(JdbcTemplate db) {
        return username ->
                db
                        .query(
                                "SELECT username,password_hash,role FROM admin_accounts WHERE"
                                    + " username=?",
                                (rs, n) ->
                                        User.withUsername(rs.getString(1))
                                                .password(rs.getString(2))
                                                .roles(rs.getString(3))
                                                .build(),
                                username)
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new UsernameNotFoundException("Sign-in failed"));
    }

    @Bean
    SecurityFilterChain site(HttpSecurity http, RateGuard guard) throws Exception {
        http.authorizeHttpRequests(
                        auth ->
                                auth.dispatcherTypeMatchers(
                                                DispatcherType.ERROR, DispatcherType.FORWARD)
                                        .permitAll()
                                        .requestMatchers("/admin/login", "/error")
                                        .permitAll()
                                        .requestMatchers("/admin", "/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/",
                                                "/posts",
                                                "/posts/**",
                                                "/campus-updates",
                                                "/events",
                                                "/student-stories",
                                                "/contact",
                                                "/replies",
                                                "/credits",
                                                "/media/**",
                                                "/css/**",
                                                "/js/**",
                                                "/images/**",
                                                "/fonts/**",
                                                "/video/**",
                                                "/api/posts",
                                                "/api/posts/**",
                                                "/health")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.HEAD,
                                                "/",
                                                "/media/**",
                                                "/video/**",
                                                "/images/**",
                                                "/health")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/contact", "/replies")
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .formLogin(
                        f ->
                                f.loginPage("/admin/login")
                                        .loginProcessingUrl("/admin/login")
                                        .defaultSuccessUrl("/admin", true)
                                        .failureUrl("/admin/login?error")
                                        .permitAll())
                .logout(
                        l ->
                                l.logoutUrl("/admin/logout")
                                        .logoutSuccessUrl("/admin/login?signedout")
                                        .invalidateHttpSession(true)
                                        .deleteCookies("JSESSIONID"))
                .headers(
                        h ->
                                h.contentSecurityPolicy(
                                        c ->
                                                c.policyDirectives(
                                                        "default-src 'self'; base-uri 'self';"
                                                            + " object-src 'none'; frame-ancestors"
                                                            + " 'none'; form-action 'self'; img-src"
                                                            + " 'self' data:; style-src 'self';"
                                                            + " script-src 'self'; font-src 'self';"
                                                            + " media-src 'self'; frame-src"
                                                            + " https://www.youtube-nocookie.com"
                                                            + " https://player.vimeo.com")));
        http.addFilterBefore(
                new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(
                            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                            throws ServletException, IOException {
                        if (req.getMethod().equals("POST")
                                && req.getServletPath().equals("/admin/login")
                                && !guard.allow("admin-login", req.getRemoteAddr(), 12)) {
                            res.setHeader("Retry-After", "900");
                            res.sendError(429);
                            return;
                        }
                        chain.doFilter(req, res);
                    }
                },
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
