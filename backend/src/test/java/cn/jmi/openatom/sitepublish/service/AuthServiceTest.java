package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.common.BusinessException;
import cn.jmi.openatom.sitepublish.config.OidcProperties;
import cn.jmi.openatom.sitepublish.dto.AuthDtos;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    @Test
    void acceptsEquivalentLocalhostAndIpv4LoopbackRedirects() {
        AuthService service = serviceWithRedirect("http://localhost:5173/auth/callback");

        AuthDtos.OAuthStartResponse response = service.start(new AuthDtos.OAuthStartRequest(
                "challenge",
                "http://127.0.0.1:5173/auth/callback",
                "nonce"
        ));

        assertThat(response.authorizeUrl())
                .contains("redirect_uri=http://127.0.0.1:5173/auth/callback");
    }

    @Test
    void rejectsLoopbackRedirectWithDifferentPort() {
        AuthService service = serviceWithRedirect("http://localhost:5173/auth/callback");

        assertThatThrownBy(() -> service.start(new AuthDtos.OAuthStartRequest(
                "challenge",
                "http://127.0.0.1:5174/auth/callback",
                "nonce"
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("OAuth 回调地址不在允许列表中");
    }

    @Test
    void doesNotAllowLoopbackWhenProductionRedirectIsConfigured() {
        AuthService service = serviceWithRedirect("https://publish.example.com/auth/callback");

        assertThatThrownBy(() -> service.start(new AuthDtos.OAuthStartRequest(
                "challenge",
                "http://127.0.0.1:5173/auth/callback",
                "nonce"
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("OAuth 回调地址不在允许列表中");
    }

    @Test
    void preservesLocallyGrantedSiteAdminRoleAcrossOauthLogin() {
        assertThat(AuthService.mergeProviderRoles("member,site_admin", "member"))
                .isEqualTo("member,site_admin");
        assertThat(AuthService.mergeProviderRoles("member", "member"))
                .isEqualTo("member");
    }

    private AuthService serviceWithRedirect(String redirectUri) {
        OidcProperties properties = new OidcProperties(
                "https://oauth.jmi-openatom.cn/api/v1",
                "site-platform",
                "",
                "openid profile email roles permissions",
                redirectUri,
                false
        );
        return new AuthService(properties, null);
    }
}
