package cn.jmi.openatom.sitepublish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_domains")
public class SiteDomain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long siteId;
    private Long userId;
    private String domain;
    private String type;
    private String status;
    private String verificationToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

