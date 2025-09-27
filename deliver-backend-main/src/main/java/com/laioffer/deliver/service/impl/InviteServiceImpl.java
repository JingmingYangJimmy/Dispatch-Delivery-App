package com.laioffer.deliver.service.impl;

import com.laioffer.deliver.exception.BusinessException;
import com.laioffer.deliver.model.RegisterBody;
import com.laioffer.deliver.repository.RoleRepository;
import com.laioffer.deliver.repository.UserRepository;
import com.laioffer.deliver.repository.UserRoleRepository;
import com.laioffer.deliver.entity.RoleEntity;
import com.laioffer.deliver.entity.UserEntity;
import com.laioffer.deliver.entity.UserRoleEntity;
import com.laioffer.deliver.service.InviteService;
import com.laioffer.deliver.service.EmailSender;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class InviteServiceImpl implements InviteService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    private final Cache inviteCache; // CacheConfig 里的 "inviteCodeCache"

    public InviteServiceImpl(UserRepository userRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository,
                             PasswordEncoder passwordEncoder,
                             EmailSender emailSender,
                             CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.inviteCache = cacheManager.getCache("inviteCodeCache");
        if (this.inviteCache == null) {
            throw new IllegalStateException("inviteCodeCache not configured");
        }
    }

    private record InviteCacheValue(String email, Long roleId) {}

    @Override
    public void createInvite(String emailRaw, String roleCodeRaw) {
        String email = emailRaw.trim().toLowerCase(Locale.ROOT);
        String roleCode = roleCodeRaw.trim().toUpperCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", "This email is already registered", HttpStatus.CONFLICT);
        }

        Long roleId = roleRepository.findIdByCode(roleCode);
        if (roleId == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "Role not found: " + roleCode, HttpStatus.NOT_FOUND);
        }

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        inviteCache.put(token, new InviteCacheValue(email, roleId));

        String subject = "邀请您加入 Deliver";
        String acceptUrl = "http://localhost:8080/auth/accept-invite?token=" + token;
        String content = """
                您好！
                您被邀请注册并加入系统，角色：%s
                点击链接完成注册（或复制到浏览器）：
                %s
                若非本人操作请忽略此邮件。
                """.formatted(roleCode, acceptUrl);
        emailSender.send(email, subject, content);
    }

    @Transactional
    @Override
    public Long acceptInvite(String token, RegisterBody body) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("INVITE_INVALID", "Invitation code is invalid", HttpStatus.BAD_REQUEST);
        }

        InviteCacheValue val = inviteCache.get(token, InviteCacheValue.class);
        if (val == null) {
            throw new BusinessException("INVITE_EXPIRED", "Invitation code does not exist or has expired", HttpStatus.GONE);
        }

        if (userRepository.existsByEmail(val.email())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", "This email is already registered", HttpStatus.CONFLICT);
        }

        // 为了写 users.role，取出角色 code（可选）
        String roleCode = Optional.ofNullable(val.roleId())
                .flatMap(id -> roleRepository.findById(id).map(RoleEntity::code))
                .orElse("CUSTOMER");

        String encodedPwd = passwordEncoder.encode(body.password());
        OffsetDateTime now = OffsetDateTime.now();

        // UserEntity 的参数顺序与类型完全按你的 record 对齐
        UserEntity newUser = new UserEntity(
                null,
                val.email(),
                body.phone(),
                encodedPwd,
                body.firstName(),
                body.lastName(),
                roleCode,      // users.role 写入受邀角色 code（或按需写 "CUSTOMER"）
                "ACTIVE",
                now,
                now
        );

        RoleEntity role = roleRepository.findByCode("CUSTOMER");
        if (role == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "Default role missing: CUSTOMER", HttpStatus.NOT_FOUND);
        }
        Long userId = userRepository.save(newUser).id();

        // user_roles 绑定
        userRoleRepository.save(new UserRoleEntity(
                null,
                userId,
                val.roleId(),
                null
        ));

        inviteCache.evict(token);
        return userId;
    }
}
