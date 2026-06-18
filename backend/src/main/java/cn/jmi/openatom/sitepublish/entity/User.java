package cn.jmi.openatom.sitepublish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String avatar;
    private String oauthSub;
    private String roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

