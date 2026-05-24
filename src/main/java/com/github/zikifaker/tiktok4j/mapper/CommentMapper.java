package com.github.zikifaker.tiktok4j.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper {
    @Select("SELECT id FROM comments WHERE video_id = #{videoId}")
    List<Long> getCommentIds(Long videoId);
}
