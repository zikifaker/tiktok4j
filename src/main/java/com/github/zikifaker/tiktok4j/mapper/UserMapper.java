package com.github.zikifaker.tiktok4j.mapper;

import com.github.zikifaker.tiktok4j.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE username = #{username}")
    User getUserByUsername(String username);

    @Insert("INSERT INTO users (username, password) VALUES (#{username}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User getUserById(Long id);
}
