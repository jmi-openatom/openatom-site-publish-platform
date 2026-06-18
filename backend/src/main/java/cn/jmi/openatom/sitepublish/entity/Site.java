package cn.jmi.openatom.sitepublish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sites")
public class Site {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String slug;
    private String framework;
    private String description;
    private String status;
    private String defaultDomain;
    private String customDomain;
    private String previewImage;
    private String branchName;
    private String sourceFilename;
    private String sourcePath;
    private Long latestDeploymentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

