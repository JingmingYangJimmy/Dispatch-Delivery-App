package com.laioffer.deliver.config;

import com.laioffer.deliver.security.JwtAuthFilter;
import com.laioffer.deliver.security.JwtService;
import com.laioffer.deliver.security.store.SidBlacklistStore;
import com.laioffer.deliver.security.store.TokenVersionStore;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class AppConfig {

    // 密码加密器（注册时加密保存）
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 安全规则
    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService,
                                       TokenVersionStore tokenVersionStore,
                                       SidBlacklistStore sidBlacklistStore) {
        return new JwtAuthFilter(jwtService, tokenVersionStore, sidBlacklistStore);
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   com.laioffer.deliver.security.JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // 关键：把 JwtAuthFilter 放到 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // 放行公开接口
                        .requestMatchers("/auth/request-code", "/auth/signup", "/auth/accept-invite").permitAll()
                        .requestMatchers("/auth/login", "/auth/refresh","auth/logout").permitAll()
                        // 受权接口
                        .requestMatchers(HttpMethod.POST, "/auth/invite").hasAuthority("INVITE_CREATE")
                        .requestMatchers(HttpMethod.POST, "/auth/logout-all").hasAuthority("SESSION_REVOKE_ALL")
                        // 文档接口
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/doc.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // CORS 配置（按需收紧域名/方法/头）
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("*")); // 生产环境请改为具体域名
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        cfg.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public OpenAPI deliverOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Deliver API")
                .version("v1")
                .description("Deliver service OpenAPI spec"));
    }
}
