package com.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.user.constant.UserSocialType;
import com.api.user.entity.UserSocial;

public interface UserSocialRepository extends JpaRepository<UserSocial, Long> {

	boolean existsBySocialTypeAndProviderId(UserSocialType socialType, String providerId);

	Optional<UserSocial> findBySocialTypeAndProviderId(UserSocialType socialType, String providerId);
}
