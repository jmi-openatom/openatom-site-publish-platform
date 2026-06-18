package cn.jmi.openatom.sitepublish.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record AuthConfig(
            String issuer,
            String clientId,
            String scope,
            String redirectUri,
            boolean devLoginEnabled
    ) {
    }

    public record OAuthStartRequest(
            @NotBlank(message = "缺少 PKCE code challenge") String codeChallenge,
            @NotBlank(message = "缺少 OAuth 回调地址") String redirectUri,
            @NotBlank(message = "缺少 OIDC nonce") String nonce
    ) {
    }

    public record OAuthStartResponse(String authorizeUrl, String state) {
    }

    public record OAuthCallbackRequest(
            @NotBlank(message = "缺少授权码") String code,
            @NotBlank(message = "缺少 OAuth state") String state,
            @NotBlank(message = "缺少 PKCE code verifier") String codeVerifier,
            @NotBlank(message = "缺少 OAuth 回调地址") String redirectUri
    ) {
    }

    public record UserView(
            Long id,
            String username,
            String displayName,
            String email,
            String avatar,
            List<String> roles
    ) {
    }

    public record LoginResponse(String tokenName, String tokenValue, long expiresIn, UserView user) {
    }
}

