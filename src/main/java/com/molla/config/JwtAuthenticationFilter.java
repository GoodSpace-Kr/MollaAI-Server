package com.molla.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            // Refresh Token은 인증 수단으로 사용 불가 — Access Token만 허용
            if ("access".equals(jwtProvider.getTokenType(token))) {
                String userId = jwtProvider.getUserId(token);
                String phoneNumber = jwtProvider.getPhoneNumber(token);

                // DB 조회 없이 JWT 클레임만으로 인증 처리
                // principal = userId, credentials = phoneNumber
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        phoneNumber,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 인증 성공 — userId: {}, uri: {}", userId, request.getRequestURI());
            } else {
                log.debug("Refresh Token으로 일반 API 호출 시도 — 거부, uri: {}", request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출.
     * 형식이 맞지 않으면 null 반환.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
