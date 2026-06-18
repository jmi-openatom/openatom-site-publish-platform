package cn.jmi.openatom.sitepublish.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.jmi.openatom.sitepublish.entity.User;
import cn.jmi.openatom.sitepublish.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-controller;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.oauth.dev-login-enabled=true"
})
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void allowsAdministratorToViewDashboardAndGrantAdminRole() throws Exception {
        String adminToken = devLoginToken();
        User member = createMember();

        mockMvc.perform(get("/api/admin/dashboard").header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.users").isNumber())
                .andExpect(jsonPath("$.data.users").isArray())
                .andExpect(jsonPath("$.data.sites").isArray());

        mockMvc.perform(put("/api/admin/users/{id}/admin", member.getId())
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admin").value(true));

        assertThat(userMapper.selectById(member.getId()).getRoles()).contains("site_admin");
    }

    @Test
    void rejectsNormalMemberFromAdminApi() throws Exception {
        User member = createMember();
        String memberToken = StpUtil.getStpLogic().createLoginSession(member.getId());

        mockMvc.perform(get("/api/admin/dashboard").header("satoken", memberToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("需要管理员权限"));
    }

    private String devLoginToken() throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/dev-login"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        return response.path("data").path("tokenValue").asText();
    }

    private User createMember() {
        LocalDateTime now = LocalDateTime.now();
        User member = new User();
        member.setUsername("member-" + UUID.randomUUID());
        member.setDisplayName("普通成员");
        member.setEmail(member.getUsername() + "@example.com");
        member.setOauthSub(member.getUsername());
        member.setRoles("member");
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        userMapper.insert(member);
        return member;
    }
}
