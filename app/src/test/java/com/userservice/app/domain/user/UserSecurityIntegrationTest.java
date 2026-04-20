package com.userservice.app.domain.user;

import com.userservice.app.TestInfrastructureConfig;
import com.userservice.app.common.base.constant.ErrorCode;
import com.userservice.app.common.base.exception.BusinessException;
import com.userservice.app.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestInfrastructureConfig.class)
class UserSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("공개_signup은 인증 없이 허용된다")
    void signupIsPublic() throws Exception {
        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpStatus").value(201))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    @DisplayName("내 정보 조회는 인증된 사용자 식별자로 DB 상태 검사를 위임한다")
    void meDelegatesAuthenticatedUserIdToService() throws Exception {
        when(userService.getMe(any())).thenReturn(null);

        mockMvc.perform(get("/users/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("123e4567-e89b-12d3-a456-426614174000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(3000))
                .andExpect(jsonPath("$.message").value("내 사용자 정보 조회 성공"));

        verify(userService).getMe(any());
    }

    @Test
    @DisplayName("내 정보 조회는 JWT status claim을 권한 판단에 사용하지 않는다")
    void meDoesNotUseJwtStatusClaimForAccessDecision() throws Exception {
        when(userService.getMe(any())).thenReturn(null);

        mockMvc.perform(get("/users/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("123e4567-e89b-12d3-a456-426614174000")
                                .claim("status", "S"))))
                .andExpect(status().isOk());

        verify(userService).getMe(any());
    }

    @Test
    @DisplayName("내 정보 조회는 DB 상태 검사 실패를 거부 응답으로 반환한다")
    void meRejectsInactiveDatabaseStatus() throws Exception {
        when(userService.getMe(any())).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/users/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("123e4567-e89b-12d3-a456-426614174000"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내부 사용자 생성은 internal scope가 있어야 한다")
    void internalCreateRequiresScope() throws Exception {
        when(userService.create(any())).thenReturn(null);

        mockMvc.perform(post("/internal/users")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth-service"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_internal")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"internal-user@example.com\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpStatus").value(201))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(3002))
                .andExpect(jsonPath("$.message").value("사용자 생성 성공"));
        verify(userService).create(any());
    }

    @Test
    @DisplayName("내부 사용자 상태 요청은 DB code를 거부한다")
    void internalStatusRequestRejectsDatabaseCode() throws Exception {
        mockMvc.perform(put("/internal/users/123e4567-e89b-12d3-a456-426614174000/status")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth-service"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_internal")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"A\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("내부 사용자 생성은 internal scope가 없으면 거부된다")
    void internalCreateRejectsWithoutScope() throws Exception {
        mockMvc.perform(post("/internal/users")
                        .with(jwt().jwt(jwt -> jwt.subject("auth-service")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"internal-user@example.com\"}"))
                .andExpect(status().isForbidden());
    }
}
