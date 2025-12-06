package com.admin.controller;

import com.admin.domain.User;
import com.admin.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

	private final UserService userService;

	public UserAdminController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<List<User>> getUsers() {
		return ResponseEntity.ok(userService.findAll());
	}

	@PostMapping
	public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
		User created = userService.create(
			request.username(),
			request.password(),
			request.email()
		);
		return ResponseEntity.ok(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<User> updateUser(
		@PathVariable Long id,
		@RequestBody UpdateUserRequest request
	) {
		User updated = userService.update(id, request.email());
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

	// DTO
	public record CreateUserRequest(String username, String password, String email) {}
	public record UpdateUserRequest(String email) {}
}
