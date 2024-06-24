package com.blog.entity;

import lombok.Data;

import java.time.LocalDateTime;

//举报
@Data
public class Report {
    private Long id;//id

    private Long userId;//来源用户id

    private String text;//文本

    private LocalDateTime createTime;//创建时间
}