package com.laioffer.deliver.service;

import com.laioffer.deliver.model.RegisterBody;

public interface RegistrationService {

    /** 发送邮箱验证码（5分钟有效） */
    void requestSignUpCode(String email);

    /** 使用验证码完成注册，返回 userId */
    Long register(RegisterBody body, String emailCode);
}
