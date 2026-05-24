package com.github.zikifaker.tiktok4j.mapper;

import com.github.zikifaker.tiktok4j.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VideoMapper {
    List<Video> getVideosByLastTime(@Param("lastTime") LocalDateTime lastTime, @Param("limit") Integer limit);

    void save(Video video);

    @Select("SELECT COUNT(*) FROM videos WHERE author_id = #{userId}")
    Long getVideoCount(Long userId);

    @Select("SELECT * FROM videos WHERE author_id = #{userId}")
    List<Video> getUserVideos(Long userId);
}
