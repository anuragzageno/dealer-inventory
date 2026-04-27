package com.dealersautocenter.inventory.config;

import com.dealersautocenter.inventory.shared.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * <p>Authentication: HTTP Basic (stateless).
 *
 * <p>Built-in demo users (replace with a real UserDetailsService in production):
 * <ul>
 *   <li>{@code tenant1_user / password} – tenantId: {@code tenant-1}, role: USER</li>
 *   <li>{@code tenant2_user / password} – tenantId: {@code tenant-2}, role: USER</li>
 *   <li>{@code global_admin / admin}     – no tenant restriction,    role: GLOBAL_ADMIN</li>
 * </ul>
 *
 * <p>Authorization:
 * <ul>
 *   <li>{@code /admin/**} – GLOBAL_ADMIN only</li>
 *   <li>All other endpoints – any authenticated user</li>
 * </ul>
 *
 * <p>The {@link TenantFilter} is inserted after {@link BasicAuthenticationFilter} so that
 * authentication is already resolved when tenant enforcement runs.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        PasswordEncoder enc = passwordEncoder();
        return new InMemoryUserDetailsManager(
                new CustomUserDetails("tenant1_user", enc.encode("password"), "tenant-1", "ROLE_USER"),
                new CustomUserDetails("tenant2_user", enc.encode("password"), "tenant-2", "ROLE_USER"),
                new CustomUserDetails("global_admin",  enc.encode("admin"),    null,       "ROLE_GLOBAL_ADMIN")
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("GLOBAL_ADMIN")
                        .anyRequest().authenticated()
                )
                .build();
    }
}
