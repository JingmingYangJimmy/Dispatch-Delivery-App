package com.laioffer.deliver.service.impl;

import com.laioffer.deliver.exception.BusinessException;
import com.laioffer.deliver.model.RegisterBody;
import com.laioffer.deliver.repository.RoleRepository;
import com.laioffer.deliver.repository.UserRepository;
import com.laioffer.deliver.repository.UserRoleRepository;
import com.laioffer.deliver.entity.UserEntity;
import com.laioffer.deliver.entity.UserRoleEntity;
import com.laioffer.deliver.service.EmailSender;
import com.laioffer.deliver.service.RegistrationService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    private final Cache emailCodeCache; // CacheConfig 里的 "emailCodeCache"
    private final SecureRandom random = new SecureRandom();

    public RegistrationServiceImpl(UserRepository userRepository,
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
        this.emailCodeCache = cacheManager.getCache("emailCodeCache");
        if (this.emailCodeCache == null) {
            throw new IllegalStateException("emailCodeCache not configured");
        }
    }

    @Override
    public void requestSignUpCode(String emailRaw) {
        String email = emailRaw.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", "This email is already registered", HttpStatus.CONFLICT);

        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        emailCodeCache.put(email, code);

        String subject = "Deliver 注册验证码";
        String content = "您的验证码是：" + code + "，5分钟内有效。";
        emailSender.send(email, subject, content);
    }

    @Transactional
    @Override
    public Long register(RegisterBody body, String code) {
        String email = body.email().trim().toLowerCase(Locale.ROOT);

        String cached = emailCodeCache.get(email, String.class);
        if (cached == null || !cached.equals(code)) {
            throw new BusinessException("EMAIL_CODE_INVALID", "Verification code is invalid or expired", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", "This email is already registered", HttpStatus.CONFLICT);
        }

        // users 表要求 role/status/OffsetDateTime
        String encodedPwd = passwordEncoder.encode(body.password());
        OffsetDateTime now = OffsetDateTime.now();

        UserEntity newUser = new UserEntity(
                null,
                email,
                body.phone(),
                encodedPwd,
                body.firstName(),
                body.lastName(),
                "CUSTOMER",        // 默认角色：USER
                "ACTIVE",      // 默认状态
                now,
                now
        );
        Long userId = userRepository.save(newUser).id();

        // user_roles 绑定角色：优先用 findIdByCode
        Long roleId = roleRepository.findIdByCode("CUSTOMER");
        if (roleId == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "Default role missing: CUSTOMER", HttpStatus.NOT_FOUND);
        }

        // UserRoleEntity(id, userId, roleId, hubId)
        userRoleRepository.save(new UserRoleEntity(
                null,
                userId,
                roleId,
                null
        ));

        // 验证码一次性使用
        emailCodeCache.evict(email);

        return userId;
    }
}
