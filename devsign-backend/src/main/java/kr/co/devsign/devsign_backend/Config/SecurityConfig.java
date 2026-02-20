package kr.co.devsign.devsign_backend.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 보안 해제 (API 방식에서는 꺼두어야 통신이 됩니다)
                .csrf(csrf -> csrf.disable())

                // 2. 세션을 사용하지 않음 (JWT 방식이므로 STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로
                        .requestMatchers(
                                "/api/members/login",
                                "/api/members/signup",
                                "/api/members/discord-send",
                                "/api/members/verify-code",
                                "/api/members/find-discord-by-info",
                                "/api/members/verify-id-pw",
                                "/api/members/reset-password-final",
                                "/api/members/check/**",
                                "/h2-console/**"
                        ).permitAll()
                        // 관리자만 접근 가능한 경로
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 4. JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 5. H2 콘솔 및 프레임 허용
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public RestTemplate RestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public BCryptPasswordEncoder BCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
