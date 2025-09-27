package com.laioffer.deliver.controller;

import com.laioffer.deliver.model.*;
import com.laioffer.deliver.service.AuthService;
import com.laioffer.deliver.service.InviteService;
import com.laioffer.deliver.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final InviteService inviteService;
    private final RegistrationService registrationService;

    public AuthController(AuthService authService,
                          InviteService inviteService,
                          RegistrationService registrationService) {
        this.authService = authService;
        this.inviteService = inviteService;
        this.registrationService = registrationService;
    }

    // ========= 登录 & 刷新 =========
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    // ========= 验证码注册（演示用） =========
    @PostMapping("/signup/request-code")
    public ResponseEntity<Void> requestCode(@RequestParam("email") String email) {
        registrationService.requestSignUpCode(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup/confirm")
    public ResponseEntity<Long> signup(@RequestBody @Valid RegisterBody body,
                                       @RequestParam("code") String code) {
        Long userId = registrationService.register(body, code);
        return ResponseEntity.ok(userId);
    }

    // ========= 邀请式注册 =========
    @PostMapping("/invite/request-code")
    public ResponseEntity<Void> invite(@RequestBody @Valid InviteCreateBody body) {
        inviteService.createInvite(body.email(), body.role());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invite/confirm")
    public ResponseEntity<Long> acceptInvite(@RequestParam("token") String token,
                                             @RequestBody @Valid RegisterBody body) {
        Long userId = inviteService.acceptInvite(token, body);
        return ResponseEntity.ok(userId);
    }


    // ===== 登出（仅当前会话）=====
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequest body) {
        authService.logout(body.accessSid(), body.refreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestBody LogoutAllRequest logoutAllRequest) {
        authService.logoutAllForUser(logoutAllRequest.userId());  // 调用服务层的批量登出方法
        return ResponseEntity.ok("All sessions logged out successfully");
    }

}
