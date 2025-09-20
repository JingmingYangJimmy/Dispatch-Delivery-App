package com.laioffer.deliver.security;

import com.laioffer.deliver.exception.BusinessException;
import com.laioffer.deliver.security.store.SidBlacklistStore;
import com.laioffer.deliver.security.store.TokenVersionStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenVersionStore tokenVersionStore;
    private final SidBlacklistStore sidBlacklistStore;

    public JwtAuthFilter(JwtService jwtService,
                         TokenVersionStore tokenVersionStore,
                         SidBlacklistStore sidBlacklistStore) {
        this.jwtService = jwtService;
        this.tokenVersionStore = tokenVersionStore;
        this.sidBlacklistStore = sidBlacklistStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1) 已经有认证则放行
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) 没有 Bearer 则放行（留给后续链按未认证处理）
        String authz = request.getHeader("Authorization");
        if (authz == null || !authz.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authz.substring(7).trim();
        if (token.isEmpty()) {
            throw new BusinessException("TOKEN_INVALID", "访问令牌无效");
        }

        try {
            // 3) 基础校验：签名 / issuer / 有效期 / type=access
            Jws<Claims> jws = jwtService.parse(token);
            Claims claims = jws.getBody();

            Object type = claims.get("type");
            if (type == null || !"access".equals(type.toString())) {
                throw new BusinessException("TOKEN_INVALID", "非法访问令牌");
            }

            Date exp = claims.getExpiration();
            if (exp == null || exp.before(new Date())) {
                throw new BusinessException("TOKEN_EXPIRED", "访问令牌已过期");
            }

            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new BusinessException("TOKEN_INVALID", "令牌缺少用户标识");
            }
            long userId;
            try {
                userId = Long.parseLong(sub);
            } catch (NumberFormatException nfe) {
                throw new BusinessException("TOKEN_INVALID", "令牌用户标识无效");
            }

            Object verObj = claims.get("ver");
            if (verObj == null) {
                throw new BusinessException("TOKEN_INVALID", "令牌缺少版本字段");
            }
            long tokenVer;
            try {
                tokenVer = ((Number) verObj).longValue();
            } catch (ClassCastException cce) {
                throw new BusinessException("TOKEN_INVALID", "令牌版本字段类型错误");
            }

            String sid = (String) claims.get("sid");
            if (sid == null || sid.isBlank()) {
                throw new BusinessException("TOKEN_INVALID", "令牌缺少会话标识");
            }

            // 4) 版本号校验（权限变更 / 登出全部）
            long currentVer = tokenVersionStore.getCurrentVersion(userId);
            if (tokenVer < currentVer) {
                throw new BusinessException("TOKEN_VERSION_OUTDATED", "令牌版本已过期，请重新登录");
            }

            // 5) 黑名单校验（登出当前设备）
            if (sidBlacklistStore.isRevoked(sid)) {
                throw new BusinessException("SESSION_REVOKED", "会话已登出，请重新登录");
            }

            // 6) 构建 Authentication，写入上下文
            List<?> rawAuthorities = claims.get("authorities", List.class);
            Collection<? extends GrantedAuthority> authorities =
                    rawAuthorities == null ? List.of()
                            : rawAuthorities.stream()
                            .map(Object::toString)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(s -> new SimpleGrantedAuthority(s.toUpperCase(Locale.ROOT)))
                            .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 7) 放行
            filterChain.doFilter(request, response);

        } catch (BusinessException be) {
            // 统一交给全局异常处理器
            throw be;
        } catch (JwtException | IllegalArgumentException e) {
            // JJWT 解析/验签失败等
            throw new BusinessException("TOKEN_INVALID", "访问令牌无效");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars");
    }

}
