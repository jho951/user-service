package com.core.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GenerationType;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 공통 매핑 정보 추상 클래스
 * 이 클래스는 직접 테이블과 매핑되지 않으며, 이를 상속받는 자식 엔티티들에게 식별자(ID)와 생성 및 수정 시간 관리 기능을 제공합니다.
 * JPA Auditing 기능을 활용하여 데이터의 생성/수정 시점을 자동으로 기록합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	/**
	 * 엔티티의 고유 식별자입니다.
	 * UUID 전략으로 생성되며 CHAR(36) 형태로 저장됩니다.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false, length = 36, columnDefinition = "char(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	/**
	 * 엔티티가 생성되어 저장된 시간입니다.
	 * 한 번 저장된 후에는 수정되지 않도록 설정되어 있습니다.
	 */
	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 엔티티의 내용이 마지막으로 수정된 시간입니다.
	 * 데이터가 업데이트될 때마다 자동으로 갱신됩니다.
	 */
	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
