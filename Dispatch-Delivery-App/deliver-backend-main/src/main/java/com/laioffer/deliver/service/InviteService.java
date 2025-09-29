package com.laioffer.deliver.service;

import com.laioffer.deliver.model.RegisterBody;

public interface InviteService {

    /** 管理员创建邀请（写入缓存 + 发送邮件） */
    void createInvite(String email, String roleCode);

    /** 被邀请者接受邀请并注册，返回 userId */
    Long acceptInvite(String token, RegisterBody body);
}
