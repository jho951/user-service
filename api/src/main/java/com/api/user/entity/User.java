package com.api.user.entity;

import java.util.ArrayList;
import java.util.List;

import com.api.user.constant.UserStatus;
import com.core.constant.UserRole;
import com.core.domain.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<UserSocial> userSocialList = new ArrayList<>();

	@Builder
	private User(String email, String name, UserRole role, UserStatus status) {
		this.email = email;
		this.name = name;
		this.role = role;
		this.status = status;
	}

	public void changeStatus(UserStatus status) {
		this.status = status;
	}

	public void addUserSocial(UserSocial userSocial) {
		userSocialList.add(userSocial);
	}
}
