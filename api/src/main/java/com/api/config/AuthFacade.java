package com.api.config;

import org.springframework.stereotype.Service;

import auth.TokenService;
import lombok.RequiredArgsConstructor;

/**
 * auth 모듈의 토큰 기능을 앱 내부에서 사용할 때 감싸는 파사드입니다.
 */
@RequiredArgsConstructor
@Service
public class AuthFacade {
	private final TokenService tokenService;

	/**
	 * 토큰 서비스를 반환합니다.
	 *
	 * @return auth 모듈 토큰 서비스
	 */
	public TokenService tokenService() {
		return tokenService;
	}
}
