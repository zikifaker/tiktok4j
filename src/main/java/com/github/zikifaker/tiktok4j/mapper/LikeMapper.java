package com.github.zikifaker.tiktok4j.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LikeMapper {
    @Select("SELECT user_id FROM likes WHERE video_id = #{videoId} AND cancel = 0")
    List<Long> getLikeUserIds(Long videoId);

    @Select("SELECT video_id FROM likes WHERE user_id = #{userId} AND cancel = 0")
    List<Long> getLikeVideoIds(Long userId);
    
    @Select("SELECT COUNT(*) FROM likes WHERE user_id = #{userId} AND cancel = 0")
    Long getUserTotalLikeCount(Long userId);

    int upsertLike(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("cancel") Integer cancel);
}
