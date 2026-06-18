package cn.jmi.openatom.sitepublish.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.jmi.openatom.sitepublish.common.BusinessException;
import cn.jmi.openatom.sitepublish.entity.User;
import cn.jmi.openatom.sitepublish.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private static final Set<String> ADMIN_ROLES = Set.of("site_admin", "admin", "super_admin");

    private final UserMapper userMapper;

    public User requireAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            StpUtil.logout();
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "本地用户不存在");
        }
        if (!hasAdminRole(user.getRoles())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }

    public static boolean hasAdminRole(String roles) {
        return parseRoles(roles).stream().anyMatch(ADMIN_ROLES::contains);
    }

    public static LinkedHashSet<String> parseRoles(String roles) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (!StringUtils.hasText(roles)) {
            return normalized;
        }
        Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> role.toLowerCase(Locale.ROOT))
                .forEach(normalized::add);
        return normalized;
    }

    public static boolean isManagedAdminRole(String role) {
        return ADMIN_ROLES.contains(role);
    }
}
