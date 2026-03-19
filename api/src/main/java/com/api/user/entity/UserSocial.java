package com.api.user.entity;

import com.api.user.constant.UserSocialType;
import com.core.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "user_social_accounts",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_social_provider_key", columnNames = {"social_type", "provider_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocial extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "social_type", nullable = false, length = 30)
	private UserSocialType socialType;

	@Column(name = "provider_id", nullable = false, length = 150)
	private String providerId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Builder
	private UserSocial(UserSocialType socialType, String providerId, User user) {
		this.socialType = socialType;
		this.providerId = providerId;
		this.user = user;
	}
}
