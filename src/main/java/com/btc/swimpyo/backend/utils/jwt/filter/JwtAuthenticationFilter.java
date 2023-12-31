package com.btc.swimpyo.backend.utils.jwt.filter;

import com.btc.swimpyo.backend.utils.jwt.entity.ErrorCode;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
//@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${secret-key}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Header의 Authorization 값이 비어있으면 => Jwt Token을 전송하지 않음 => 로그인 하지 않음
        // Header의 Authorization 값이 'Bearer '로 시작하지 않으면 => 잘못된 토큰
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.error("authorization이 없습니다 또는 'Bearer '로 시작하지 않는 잘못된 토큰입니다.");
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorization.substring(7);

        System.out.println("tp");

        // token 만료되었는지 확인
        if (validate(secretKey, token)) {
            log.error("token이 만료되었습니다.");
//            filterChain.doFilter(request, response);
//            return;
        }

        // getUserEmail
        String email = getUserEmail(secretKey, token);
        // 권한 정보 추출
        String rolesStr = (String) extractClaims(secretKey, token).get("authorities");
        List<String> roles = Arrays.asList(rolesStr.split(","));
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());

        AbstractAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(email, null, authorities);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 인증 요청에 대한 세부정보 작성 가능

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticationToken);

        SecurityContextHolder.setContext(securityContext);

        filterChain.doFilter(request, response); // 다음 필터로

    }


    // Claims에서 login email 꺼내기
    public static String getUserEmail(String secretKey, String token) {
        log.info("getUserEmail in");
        return extractClaims(secretKey, token).get("email").toString();
    }

    // 밝급된 Token이 만료 시간이 지났는지 체크
    public static boolean validate(String secretKey, String token) {
        log.info("validate in");
        try {
            Date expiredDate = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getExpiration();
            // Token의 만료 날짜가 지금보다 이전인지 check
            return expiredDate.before(new Date());
        } catch (SignatureException e) {
            log.info("SignatureException");
            throw new JwtException(ErrorCode.WRONG_TYPE_TOKEN.getMessage());
        } catch (MalformedJwtException e) {
            log.info("MalformedJwtException");
            throw new JwtException(ErrorCode.UNSUPPORTED_TOKEN.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("ExpiredJwtException");
            throw new JwtException(ErrorCode.EXPIRED_TOKEN.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("IllegalArgumentException");
            throw new JwtException(ErrorCode.UNKNOWN_ERROR.getMessage());
        }

    }

    //     SecretKey를 사용해 Token Parsing
    private static Claims extractClaims(String secretKey, String token) {
        log.info("extractClaims in");

        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

}
