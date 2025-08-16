package de.fuh.kn.webapp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Konfiguration für Spring Security.
 * Definiert Sicherheitskonfigurationen für die Anwendung.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();

    /**
     * Sicherheitskonfiguration für Ressourcen, die für Studierende zugänglich sind.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain studentFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/student/**")
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().hasRole("STUDENT")
            );
        
        return http.build();
    }

    /**
     * Sicherheitskonfiguration für Ressourcen, die für Kursbetreuende zugänglich sind.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain kursbetreuerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/kursbetreuer/**")
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().hasRole("KURSBETREUER")
            );
        
        return http.build();
    }

    /**
     * Allgemeine Sicherheitskonfiguration mit Login-Konfiguration.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/logout", "/registration", "/information", "/change-language", "/css/**", "/js/**", "/images/**", "/webjars/**", "/error", "/robots.txt", 
                              "/favicon.ico", "/favicon-16x16.png", "/favicon-32x32.png", 
                              "/apple-touch-icon.png", "/android-chrome-192x192.png", "/android-chrome-512x512.png", 
                              "/site.webmanifest").permitAll()
                .requestMatchers("/api/test/**").permitAll() // Allow test endpoints when test-data profile is active
                .requestMatchers("/dashboard", "/profil/**", "/aktivitaeten/htmx/filter", "/material/kurseinheit/*/bild").fullyAuthenticated()
                .anyRequest().denyAll() // Strikter Ansatz: Verweigere alle Anfragen, die nicht explizit in den anderen Chains erlaubt sind
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/test/**") // Disable CSRF for test endpoints
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .successHandler(savedRequestAwareAuthenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        return http.build();
    }


    /**
     * Bean für den PasswordEncoder, der für die Verschlüsselung von Passwörtern verwendet wird.
     *
     * @return Ein BCryptPasswordEncoder für sichere Passwort-Hashes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
