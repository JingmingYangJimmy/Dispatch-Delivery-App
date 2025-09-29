package com.laioffer.deliver.service.impl;

import com.laioffer.deliver.config.JwtProperties;
import com.laioffer.deliver.exception.BusinessException;
import com.laioffer.deliver.model.LoginRequest;
import com.laioffer.deliver.model.RefreshRequest;
import com.laioffer.deliver.model.TokenResponse;
import com.laioffer.deliver.repository.UserRepository;
import com.laioffer.deliver.security.JwtService;
import com.laioffer.deliver.security.store.PermissionCache;
import com.laioffer.deliver.security.store.SessionStore;
import com.laioffer.deliver.security.store.SidBlacklistStore;
import com.laioffer.deliver.security.store.TokenVersionStore;
import com.laioffer.deliver.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;       // 统一使用这个字段名

    private final TokenVersionStore tokenVersionStore;
    private final PermissionCache permissionCache;
    private final SessionStore sessionStore;
    private final SidBlacklistStore sidBlacklistStore;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           JwtProperties jwtProperties,
                           TokenVersionStore tokenVersionStore,
                           PermissionCache permissionCache,
                           SessionStore sessionStore,
                           SidBlacklistStore sidBlacklistStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;         // 字段名与参数一致
        this.tokenVersionStore = tokenVersionStore;
        this.permissionCache = permissionCache;
        this.sessionStore = sessionStore;
        this.sidBlacklistStore = sidBlacklistStore;
    }

    /** 登录：签发 access/refresh 并持久化会话 */
    @Override
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        var user = userRepository.findByEmail(email);
        if (user == null || !passwordEncoder.matches(req.password(), user.password())) {
            throw new BusinessException("BAD_CREDENTIALS", "邮箱或密码错误");
        }

        long ver = tokenVersionStore.getCurrentVersion(user.id());
        List<String> authorities = permissionCache.getPermissions(user.id());

        String sid = UUID.randomUUID().toString().replace("-", "");

        String refresh = jwtService.generateRefreshToken(user.id(), sid);
        Instant refreshExp = Instant.now().plus(Duration.ofDays(jwtProperties.refreshTtlDays()));
        sessionStore.createSession(user.id(), sid, refresh, refreshExp, null);

        String access = jwtService.generateAccessToken(user.id(), user.email(), authorities, ver, sid);
        long expiresInSeconds = jwtProperties.accessTtlMinutes() * 60L;

        return new TokenResponse(access, refresh, expiresInSeconds, sid);
    }

    /** 刷新：轮换 refresh/会话，重签发 access */
    @Override
    public TokenResponse refresh(RefreshRequest req) {
        String refreshToken = req.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("INVALID_TOKEN", "刷新令牌不能为空");
        }

        Jws<Claims> jws = jwtService.parse(refreshToken);
        Claims claims = jws.getBody();

        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException("INVALID_TOKEN", "非法刷新令牌");
        }

        long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException("INVALID_TOKEN", "刷新令牌用户标识错误");
        }

        String oldSid = (String) claims.get("sid");
        if (oldSid == null || oldSid.isBlank()) {
            throw new BusinessException("INVALID_TOKEN", "刷新令牌缺少会话标识");
        }

        if (!sessionStore.isRefreshValid(userId, oldSid, refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "刷新令牌已失效");
        }

        // 轮换会话
        String newSid = UUID.randomUUID().toString().replace("-", "");
        String newRefresh = jwtService.generateRefreshToken(userId, newSid);
        Instant newRefreshExp = Instant.now().plus(Duration.ofDays(jwtProperties.refreshTtlDays()));
        sessionStore.rotateSession(oldSid, newSid, newRefresh, newRefreshExp);

        // 重签发 access
        var u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
        long ver = tokenVersionStore.getCurrentVersion(userId);
        List<String> authorities = permissionCache.getPermissions(userId);

        String newAccess = jwtService.generateAccessToken(userId, u.email(), authorities, ver, newSid);
        long expiresInSeconds = jwtProperties.accessTtlMinutes() * 60L;


        return new TokenResponse(newAccess, newRefresh, expiresInSeconds, newSid);
    }

    // ========= 登出（单端） =========
    @Override
    public void logout(String accessSid, String refreshToken) {
        // 1) 把 access 的 sid 拉黑，TTL 与 access 一致
        if (StringUtils.hasText(accessSid)) {
            long ttlSeconds = Duration.ofMinutes(jwtProperties.accessTtlMinutes()).getSeconds();
            sidBlacklistStore.revokeTemporarily(accessSid, ttlSeconds); // 与实现类签名一致
        }

        // 2) 若携带 refreshToken，尽量撤销其会话（容错，不影响主流程）
        if (StringUtils.hasText(refreshToken)) {
            try {
                Jws<Claims> claims = jwtService.parse(refreshToken);
                String sid = (String) claims.getBody().get("sid");
                if (StringUtils.hasText(sid)) {
                    // 如果你的 SessionStore 方法不是这个名字，请用 B2 的方式改成对应名字
                    sessionStore.revokeBySid(sid);
                }
            } catch (JwtException e) {
                throw new BusinessException("LOGOUT_FAIL", "登出识别");
            }
        }
    }


    @Override
    public void logoutAllForUser(Long userId) {
        // 1) 版本号 +1：让所有旧 access token 立刻失效（jwt 里有 version 校验）
        tokenVersionStore.bumpVersion(userId);

        // 2) 失效该用户全部 refresh 会话：彻底阻断续签
        sessionStore.revokeAll(userId);

        // 3) 清权限缓存（防止权限变更后短时间内仍命中本地缓存）
        permissionCache.invalidate(userId);
    }

    @Override
    public void logoutAllForCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("UNAUTHORIZED", "未认证");
        }

        Long userId;
        try {
            userId = Long.parseLong(auth.getName()); // JWT 的 sub = userId
        } catch (NumberFormatException e) {
            throw new BusinessException("UNAUTHORIZED", "无效的登录上下文");
        }

        // 调用上面提取的方法
        logoutAllForUser(userId);
    }

}
