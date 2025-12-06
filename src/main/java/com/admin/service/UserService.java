package com.admin.service;

import com.admin.domain.User;
import com.admin.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> findAll() {
		return userRepository.findAll();
	}

	public User create(String username, String password, String email) {
		User user = new User(username, password, email);
		return userRepository.save(user);
	}

	public User update(Long id, String email) {
		User user = userRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
		user.setEmail(email);
		return user;
	}

	public void delete(Long id) {
		userRepository.deleteById(id);
	}
}
