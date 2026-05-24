package com.github.zikifaker.tiktok4j.dto.req;

import lombok.Data;

@Data
public class UserRegisterReq {
    private String username;
    private String password;
}
