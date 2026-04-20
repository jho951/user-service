package com.userservice.app.domain.user.service;

import java.util.Optional;
import java.util.UUID;

import com.userservice.app.common.base.constant.ErrorCode;
import com.userservice.app.common.base.exception.BusinessException;
import com.userservice.app.domain.audit.UserAuditLogService;
import com.userservice.app.domain.user.constant.UserRole;
import com.userservice.app.domain.user.constant.UserStatus;
import com.userservice.app.domain.user.entity.User;
import com.userservice.app.domain.user.observability.SocialLinkMetrics;
import com.userservice.app.domain.user.repository.UserRepository;
import com.userservice.app.domain.user.repository.UserSocialRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserSocialRepository userSocialRepository;

	@Mock
	private SocialLinkMetrics socialLinkMetrics;

	@Mock
	private UserAuditLogService userAuditLogService;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	@DisplayName("내 정보 조회는 DB의 ACTIVE 상태 사용자만 반환한다")
	void getMeReturnsActiveUserFromDatabase() {
		UUID userId = UUID.randomUUID();
		User user = user(UserStatus.ACTIVE);
		when(userRepository.findWithUserSocialListById(userId)).thenReturn(Optional.of(user));

		assertThat(userService.getMe(userId).getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("내 정보 조회는 DB 상태가 ACTIVE가 아니면 거부한다")
	void getMeRejectsInactiveUserFromDatabase() {
		UUID userId = UUID.randomUUID();
		User user = user(UserStatus.SUSPENDED);
		when(userRepository.findWithUserSocialListById(userId)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userService.getMe(userId))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	private User user(UserStatus status) {
		return User.builder()
			.email("user@example.com")
			.role(UserRole.USER)
			.status(status)
			.build();
	}
}
