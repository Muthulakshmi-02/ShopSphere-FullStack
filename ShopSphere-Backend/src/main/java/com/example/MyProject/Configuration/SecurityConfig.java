package com.example.MyProject.Configuration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // --- 1. ENABLE CORS (Critical fix for your error) ---
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // --- 2. Disable CSRF (Standard for Stateless JWT APIs) ---
                .csrf(csrf -> csrf.disable())

                // --- 3. Session Management ---
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // --- 4. Endpoint Authorization ---
                .authorizeHttpRequests(auth -> auth
                        // Allow all preflight OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        // Public Auth & System Endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()


                        // Public Product Browsing
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/recent-activity").permitAll()

                        // Must come BEFORE the broader "POST /api/products/** -> ADMIN"
                        // rule below (Spring Security is first-match-wins). Without
                        // this specific rule, a customer's review submission -
                        // POST /api/products/{id}/reviews - would get blocked at the
                        // security-filter level by the admin-only products rule,
                        // before ever reaching the controller's own
                        // @PreAuthorize("hasRole('USER')") check.
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reviews").hasRole("USER")

                        // Admin Management (Preserving all your original endpoints)
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // User Endpoints
                        .requestMatchers("/api/cart/**").hasRole("USER")
                        .requestMatchers("/api/addresses/**").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/checkout").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/my-orders").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/cancel").hasRole("USER")
                        .requestMatchers("/api/payments/**").authenticated()

                        // Catch-all
                        .anyRequest().authenticated()
                )

                // --- 5. JWT Filter ---
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // --- CORS CONFIGURATION BEAN ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Matches your Angular port
        configuration.setAllowedOrigins(List.of("http://localhost:4500"));

        // Allowed Methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allowed Headers - using "*" to ensure JWT and Content-Type work
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials for secure storage
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}