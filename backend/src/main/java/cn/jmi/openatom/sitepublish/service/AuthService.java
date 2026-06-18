package cn.jmi.openatom.sitepublish.service;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.jmi.openatom.sitepublish.common.BusinessException;
import cn.jmi.openatom.sitepublish.config.OidcProperties;
import cn.jmi.openatom.sitepublish.dto.AuthDtos;
import cn.jmi.openatom.sitepublish.entity.User;
import cn.jmi.openatom.sitepublish.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OidcProperties properties;
    private final UserMapper userMapper;
    private final RestClient restClient = RestClient.create();
    private final Map<String, PendingOAuth> pendingStates = new ConcurrentHashMap<>();

    public AuthDtos.AuthConfig config() {
        return new AuthDtos.AuthConfig(
                properties.issuer(),
                properties.clientId(),
                properties.scope(),
                properties.redirectUri(),
                properties.devLoginEnabled()
        );
    }

    public AuthDtos.OAuthStartResponse start(AuthDtos.OAuthStartRequest request) {
        validateRedirectUri(request.redirectUri());
        pendingStates.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));

        String state = UUID.randomUUID().toString();
        pendingStates.put(state, new PendingOAuth(request.redirectUri(), request.nonce(), Instant.now().plusSeconds(300)));

        String authorizeUrl = UriComponentsBuilder
                .fromUriString(properties.issuer() + "/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", request.redirectUri())
                .queryParam("scope", properties.scope())
                .queryParam("state", state)
                .queryParam("nonce", request.nonce())
                .queryParam("code_challenge", request.codeChallenge())
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();
        return new AuthDtos.OAuthStartResponse(authorizeUrl, state);
    }

    public AuthDtos.LoginResponse callback(AuthDtos.OAuthCallbackRequest request) {
        PendingOAuth pending = pendingStates.remove(request.state());
        if (pending == null || pending.expiresAt().isBefore(Instant.now())
                || !pending.redirectUri().equals(request.redirectUri())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "OAuth state 无效或已过期，请重新登录");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("code", request.code());
        form.add("redirect_uri", request.redirectUri());
        form.add("code_verifier", request.codeVerifier());
        if (StringUtils.hasText(properties.clientSecret())) {
            form.add("client_secret", properties.clientSecret());
        }

        Map<?, ?> tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(properties.issuer() + "/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "OpenAtom OAuth 换取令牌失败");
        }

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "OpenAtom OAuth 未返回访问令牌");
        }

        Map<?, ?> profile = tokenResponse.get("user") instanceof Map<?, ?> userMap
                ? userMap
                : loadUserInfo(String.valueOf(tokenResponse.get("access_token")));
        return issueLocalLogin(mapUser(profile));
    }

    public AuthDtos.LoginResponse devLogin() {
        if (!properties.devLoginEnabled()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "开发登录未启用");
        }
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getOauthSub, "dev-user"));
        if (user == null) {
            user = new User();
            user.setUsername("dev");
            user.setDisplayName("林同学");
            user.setEmail("dev@jmi-openatom.cn");
            user.setOauthSub("dev-user");
            user.setRoles("member,site_admin");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }
        return issueLocalLogin(user);
    }

    public AuthDtos.UserView currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            StpUtil.logout();
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "本地用户不存在");
        }
        return toView(user);
    }

    public void logout() {
        StpUtil.logout();
    }

    private Map<?, ?> loadUserInfo(String accessToken) {
        try {
            Map<?, ?> profile = restClient.get()
                    .uri(properties.issuer() + "/oauth/userinfo")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(Map.class);
            if (profile == null) {
                throw new IllegalStateException("Empty userinfo");
            }
            return profile;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "无法读取 OpenAtom 用户资料");
        }
    }

    private User mapUser(Map<?, ?> profile) {
        String sub = text(profile, "sub");
        if (!StringUtils.hasText(sub)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "OpenAtom 用户资料缺少 sub");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getOauthSub, sub));
        String email = text(profile, "email");
        if (user == null && StringUtils.hasText(email)) {
            user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        }
        if (user == null) {
            user = new User();
            user.setCreatedAt(LocalDateTime.now());
        }

        user.setOauthSub(sub);
        user.setUsername(firstText(profile, "preferred_username", "username", "sub"));
        user.setDisplayName(firstText(profile, "name", "nickname", "preferred_username", "username"));
        user.setEmail(email);
        user.setAvatar(firstText(profile, "avatar", "picture"));
        user.setRoles(mergeProviderRoles(user.getRoles(), listText(profile.get("roles"))));
        user.setUpdatedAt(LocalDateTime.now());

        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user;
    }

    private AuthDtos.LoginResponse issueLocalLogin(User user) {
        StpUtil.login(user.getId(), new SaLoginModel().setTimeout(7 * 24 * 60 * 60));
        return new AuthDtos.LoginResponse(
                StpUtil.getTokenName(),
                StpUtil.getTokenValue(),
                7 * 24 * 60 * 60,
                toView(user)
        );
    }

    private AuthDtos.UserView toView(User user) {
        List<String> roles = StringUtils.hasText(user.getRoles())
                ? Arrays.stream(user.getRoles().split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : Collections.emptyList();
        return new AuthDtos.UserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatar(),
                roles
        );
    }

    private void validateRedirectUri(String redirectUri) {
        boolean configured = redirectUri.equals(properties.redirectUri());
        if (!configured && !isEquivalentLoopbackRedirect(properties.redirectUri(), redirectUri)) {
            throw new BusinessException("OAuth 回调地址不在允许列表中");
        }
    }

    private boolean isEquivalentLoopbackRedirect(String configuredRedirectUri, String requestedRedirectUri) {
        try {
            URI configured = URI.create(configuredRedirectUri);
            URI requested = URI.create(requestedRedirectUri);
            return isLoopbackHost(configured.getHost())
                    && isLoopbackHost(requested.getHost())
                    && "http".equalsIgnoreCase(configured.getScheme())
                    && "http".equalsIgnoreCase(requested.getScheme())
                    && effectivePort(configured) == effectivePort(requested)
                    && configured.getPath().equals(requested.getPath())
                    && configured.getQuery() == null
                    && requested.getQuery() == null
                    && configured.getFragment() == null
                    && requested.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }

    private int effectivePort(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : 80;
    }

    private static String text(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String firstText(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            String value = text(source, key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String listText(Object value) {
        if (value instanceof List<?> list) {
            return String.join(",", list.stream().map(String::valueOf).toList());
        }
        return value == null ? "" : String.valueOf(value);
    }

    static String mergeProviderRoles(String existingRoles, String providerRoles) {
        LinkedHashSet<String> merged = AdminAccessService.parseRoles(providerRoles);
        if (AdminAccessService.parseRoles(existingRoles).contains("site_admin")) {
            merged.add("site_admin");
        }
        return String.join(",", merged);
    }

    private record PendingOAuth(String redirectUri, String nonce, Instant expiresAt) {
    }
}
