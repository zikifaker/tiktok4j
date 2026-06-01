package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.dto.req.UserLoginReq;
import com.github.zikifaker.tiktok4j.dto.req.UserRegisterReq;
import com.github.zikifaker.tiktok4j.dto.resp.UserInfoResp;
import com.github.zikifaker.tiktok4j.dto.resp.UserLoginResp;
import com.github.zikifaker.tiktok4j.dto.resp.UserRegisterResp;
import com.github.zikifaker.tiktok4j.entity.User;
import com.github.zikifaker.tiktok4j.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegisterResp> register(@RequestBody UserRegisterReq req) {
        String username = req.getUsername();
        String password = req.getPassword();

        User user = userService.getUserByUsername(username);
        if (user != null) {
            UserRegisterResp respBody = UserRegisterResp.builder()
                    .resp(BaseResponse.USER_ALREADY_EXISTS)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(respBody);
        }

        User newUser = User.builder()
                .username(username)
                .password(userService.encryptPassword(password))
                .build();
        userService.saveUser(newUser);

        UserRegisterResp respBody = UserRegisterResp.builder()
                .resp(BaseResponse.SUCCESS)
                .userId(newUser.getId())
                .token(userService.generateToken(newUser))
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respBody);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResp> login(@RequestBody UserLoginReq req) {
        String username = req.getUsername();
        String password = req.getPassword();

        User user = userService.getUserByUsername(username);
        String encryptedPassword = userService.encryptPassword(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            UserLoginResp respBody = UserLoginResp.builder()
                    .resp(BaseResponse.USER_PASSWORD_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(respBody);
        }

        UserLoginResp respBody = UserLoginResp.builder()
                .resp(BaseResponse.SUCCESS)
                .userId(user.getId())
                .token(userService.generateToken(user))
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(respBody);
    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoResp> getInfo(@RequestParam("user_id") Long userId, HttpServletRequest request) {
        Long curUserId = (Long) request.getAttribute(ContextKeys.USER_ID);
        UserInfoBO userInfoBO = userService.getUserInfo(curUserId, userId);
        UserInfoResp respBody = UserInfoResp.builder()
                .resp(BaseResponse.SUCCESS)
                .userInfoBO(userInfoBO)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(respBody);
    }
}
