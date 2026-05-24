package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.config.JWTConfig;
import com.github.zikifaker.tiktok4j.entity.User;
import com.github.zikifaker.tiktok4j.mapper.UserMapper;
import com.github.zikifaker.tiktok4j.service.FollowService;
import com.github.zikifaker.tiktok4j.service.LikeService;
import com.github.zikifaker.tiktok4j.service.UserService;
import com.github.zikifaker.tiktok4j.service.VideoService;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private UserMapper userMapper;

    private JWTConfig jwtConfig;

    private FollowService followService;

    private VideoService videoService;

    private LikeService likeService;

    @Autowired
    public UserServiceImpl(
            UserMapper userMapper,
            JWTConfig jwtConfig,
            FollowService followService,
            VideoService videoService,
            LikeService likeService
    ) {
        this.userMapper = userMapper;
        this.jwtConfig = jwtConfig;
        this.followService = followService;
        this.videoService = videoService;
        this.likeService = likeService;
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }

    @Override
    public void saveUser(User user) {
        int affectedRows = userMapper.save(user);
        if (affectedRows != 1) {
            log.error("Error inserting user {}", user.getUsername());
        }
    }

    @Override
    public String encryptPassword(String password) {
        return DigestUtils.sha256Hex(password);
    }

    @Override
    public String generateToken(User user) {
        long expireMillis = jwtConfig.getExpireDays() * 24L * 60 * 60 * 1000;
        String token = Jwts.builder()
                .audience().add(user.getUsername()).and()
                .expiration(new Date(System.currentTimeMillis() + expireMillis))
                .id(String.valueOf(user.getId()))
                .issuedAt(new Date())
                .issuer("tiktok")
                .notBefore(new Date())
                .subject("token")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes()))
                .compact();
        log.info("Generate token successfully, username: {}", user.getUsername());
        return String.format("Bearer %s", token);
    }

    @Override
    public UserInfoBO getUserInfo(Long currentUserId, Long targetUserId) {
        User targetUser = userMapper.getUserById(targetUserId);
        return UserInfoBO.builder()
                .userId(targetUserId)
                .username(targetUser.getUsername())
                .followeeCount(followService.getFolloweeCount(targetUserId))
                .followerCount(followService.getFollowerCount(targetUserId))
                .isFollowed(followService.isFollowed(currentUserId, targetUserId))
                .workCount(videoService.getWorkCount(targetUserId))
                .likeCount(likeService.getUserTotalLikeCount(targetUserId))
                .build();
    }
}
