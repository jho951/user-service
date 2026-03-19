package com.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.api.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmail(String email);

	@EntityGraph(attributePaths = "userSocialList")
	Optional<User> findWithUserSocialListById(Long id);

	@EntityGraph(attributePaths = "userSocialList")
	Optional<User> findWithUserSocialListByEmail(String email);
}
