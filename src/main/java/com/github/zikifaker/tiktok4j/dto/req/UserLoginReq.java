package com.github.zikifaker.tiktok4j.dto.req;

import lombok.Data;

@Data
public class UserLoginReq {
    private String username;
    private String password;
}
