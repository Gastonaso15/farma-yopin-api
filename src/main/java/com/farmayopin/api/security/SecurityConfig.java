package com.farmayopin.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // Static and public image access
                        .requestMatchers(HttpMethod.GET, "/img/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/*/imagen").permitAll()
                        // Logout
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        // Productos: GET list is accessible to CLIENTE and ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/productos").hasAnyRole("CLIENTE", "ADMIN")
                        // Productos: Admin operations
                        .requestMatchers(HttpMethod.GET, "/api/productos/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/productos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/productos/{id}/compras").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/productos/{id}/imagen").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/productos/imagenes/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/{id}/imagen").hasRole("ADMIN")
                        // Carrito: Cliente only
                        .requestMatchers("/api/carrito/**").hasRole("CLIENTE")
                        // Compras: Cliente only
                        .requestMatchers(HttpMethod.GET, "/api/compras").hasRole("CLIENTE")
                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
