package com.btc.swimpyo.backend.config;

import com.btc.swimpyo.backend.utils.jwt.filter.JwtAuthenticationFilter;
import com.btc.swimpyo.backend.utils.jwt.filter.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    // throws Exception은 모든 Exception 처리를 호출부에서 하겠다라는 의미
    @Bean
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception { 
        return httpSecurity
                .cors().and()
                .csrf().disable() // 공격자가 사용자의 의지 없이(모르게) 공격하게 만드는 보안 공격
                .httpBasic().disable() // disable 안하면 security 로그인창 뜸
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class)
                .authorizeHttpRequests()
                .requestMatchers("/",
                        "/api/admin/member/signUp",
                        "/api/admin/member/signIn",
                        "/api/admin/member/refreshToken",
                        "/api/admin/member/logout",
                        "/api/admin/member/signout").permitAll()
//                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").permitAll()
                .requestMatchers(
                        "/api/user/member/signUp",
                        "/api/user/member/signIn",
                        "/api/user/member/refreshToken",
                        "/api/user/member/logout",
                        "/api/user/member/signout",
                        "/api/user/accm/rankAccmList",
                        "/api/user/accm/mapInfoList",
                        "/api/user/accm/showAccmList",
                        "/api/user/accm/showAccmDetail",
                        "/api/user/room/showRoomList",
                        "/api/user/room/showRoomDetail",
                        "/api/user/reservation/registConfirm",
                        "/api/user/review/showReviewList",
                        "/api/user/review/showReviewListRoom",
                        "/api/user/review/showDetail",
                        "/api/user/accm/search").permitAll()
//                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers("/api/user/**").permitAll()
                .anyRequest().authenticated()
                .and().build();

    }


}
