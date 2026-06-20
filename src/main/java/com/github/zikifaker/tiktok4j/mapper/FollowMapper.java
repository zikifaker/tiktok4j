package com.github.zikifaker.tiktok4j.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper {
    @Select("SELECT COUNT(*) FROM follows WHERE follower_id = #{followerId} AND cancel = 0")
    Long getFolloweeCount(Long followerId);

    @Select("SELECT COUNT(*) FROM follows WHERE followee_id = #{followeeId} AND cancel = 0")
    Long getFollowerCount(Long followeeId);

    @Select("SELECT COUNT(*) FROM follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId} AND cancel = 0")
    Boolean isFollowed(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT followee_id FROM follows WHERE follower_id = #{followerId} AND cancel = 0")
    List<Long> getFolloweeIds(Long followerId);

    @Select("SELECT follower_id FROM follows WHERE followee_id = #{followeeId} AND cancel = 0")
    List<Long> getFollowerIds(Long followeeId);

    void follow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    void unfollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);
}
