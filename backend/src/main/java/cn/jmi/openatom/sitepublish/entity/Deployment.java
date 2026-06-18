package cn.jmi.openatom.sitepublish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("deployments")
public class Deployment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long siteId;
    private Long userId;
    private String status;
    private String environment;
    private String commitHash;
    private String sourceFilename;
    private String outputPath;
    private String buildLog;
    private Integer durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}

