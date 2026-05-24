package com.github.zikifaker.tiktok4j.service;

import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.entity.User;

public interface UserService {
    User getUserByUsername(String username);

    void saveUser(User user);

    String encryptPassword(String password);

    String generateToken(User user);

    UserInfoBO getUserInfo(Long currentUserId, Long targetUserId);
}
