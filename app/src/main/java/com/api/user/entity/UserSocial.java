package com.api.user.entity;

import com.api.user.constant.UserSocialType;
import com.api.user.converter.UserSocialTypeConverter;
import com.core.entity.BaseEntity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 소셜 계정 연동 정보를 관리하는 엔티티입니다.
 * <p>동일한 소셜 제공자 내에서 중복된 식별값이 존재하지 않도록 {@code (social_type, provider_id)} 복합 유니크 제약 조건을 가집니다.</p>
 */
@Entity
@Table(
	name = "user_social_accounts",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_user_social_provider_key",
			columnNames = {"social_type", "provider_id"}
		)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "user_social_id", nullable = false, updatable = false, columnDefinition = "char(36)", length = 36))
public class UserSocial extends BaseEntity {

	@Convert(converter = UserSocialTypeConverter.class)
	@Column(name = "social_type", nullable = false, length = 3)
	private UserSocialType socialType;

	@Column(name = "provider_id", nullable = false, length = 150)
	private String providerId;

	@Column(name = "email", length = 191)
	private String email;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, columnDefinition = "char(36)")
	private User user;

	/**
	 * 소셜 계정 엔티티를 생성하는 빌더입니다.
	 * @param socialType 소셜 로그인 제공자 타입 (예: GGL, KKO)
	 * @param providerId 소셜 제공자(IdP)로부터 받은 사용자의 고유 식별자
	 * @param email      소셜 계정과 연결 당시 이메일 링크
	 * @param user       소셜 계정과 연결된 시스템 사용자 정보
	 */
	@Builder
	private UserSocial(UserSocialType socialType, String providerId, String email, User user) {
		this.socialType = socialType;
		this.providerId = providerId;
		this.email = email;
		this.user = user;
	}

	public void changeEmail(String email) {
		this.email = email;
	}
}
