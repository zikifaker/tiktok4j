package com.github.zikifaker.tiktok4j.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.github.zikifaker.tiktok4j.entity.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {
    @Select("SELECT id FROM comments WHERE video_id = #{videoId} AND cancel = 0")
    List<Long> getCommentIds(Long videoId);

    void save(Comment comment);

    @Delete("DELETE FROM comments WHERE id = #{id}")
    void deleteById(Long id);

    @Select("SELECT id, user_id, content, create_time FROM comments WHERE video_id = #{videoId} AND cancel = 0")
    List<Comment> getCommentsByVideoId(Long videoId);
}
