package com.api.user.controller;

import com.core.dto.BaseResponse;

import com.api.user.domain.User;
import com.api.user.dto.UserRequestDto;
import com.api.user.service.UserServiceInterface;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 사용자 관리 API 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

	private final UserServiceInterface userService;

	/**
	 * 생성자 주입입니다.
	 *
	 * @param userService 사용자 서비스
	 */
	public UserAdminController(UserServiceInterface userService) {
		this.userService = userService;
	}

	/**
	 * 관리자 사용자 목록을 조회합니다.
	 *
	 * @return 사용자 목록 응답
	 */
	@GetMapping
	public ResponseEntity<BaseResponse<List<User>>> getUsers() {
		List<User> users = userService.findAll();
		return ResponseEntity.ok(BaseResponse.ok(users));
	}

	/**
	 * 관리자 사용자를 생성합니다.
	 *
	 * @param request 생성 요청
	 * @return 생성된 사용자 응답
	 */
	@PostMapping
	public ResponseEntity<BaseResponse<User>> createUser(@RequestBody UserRequestDto request) {
		User created = userService.create(request);
		return ResponseEntity.ok(BaseResponse.ok(created, "관리자 계정이 생성되었습니다."));
	}

	/**
	 * 관리자 사용자 정보를 수정합니다.
	 *
	 * @param id 사용자 ID
	 * @param request 수정 요청
	 * @return 수정된 사용자 응답
	 */
	@PutMapping("/{id}")
	public ResponseEntity<BaseResponse<User>> updateUser(
		@PathVariable Long id,
		@RequestBody UserRequestDto request
	) {
		User updated = userService.update(id, request);
		return ResponseEntity.ok(BaseResponse.ok(updated, "관리자 계정이 수정되었습니다."));
	}

	/**
	 * 관리자 사용자를 삭제합니다.
	 *
	 * @param id 사용자 ID
	 * @return 삭제 결과 응답
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable Long id) {
		userService.delete(id);
		return ResponseEntity.ok(BaseResponse.ok(null, "관리자 계정이 삭제되었습니다."));
	}
}
