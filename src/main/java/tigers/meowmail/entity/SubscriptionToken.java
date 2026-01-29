package tigers.meowmail.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
	name = "subscription_token",
	indexes = {
		@Index(name = "idx_subscription_token_hash_hex", columnList = "tokenHashHex", unique = true)
	}
)
public class SubscriptionToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false, unique = true)
	private Subscriber subscriber;

	@Column(nullable = false, unique = true, length = 64)
	private String tokenHashHex;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant usedAt;

	public boolean isExpired(Instant now) {
		return now.isAfter(expiresAt);
	}

	public boolean isUsed() {
		return usedAt != null;
	}

	public void markUsed(Instant usedAt) {
		this.usedAt = usedAt;
	}

	public void updateToken(String newTokenHashHex, Instant newExpiresAt) {
		this.tokenHashHex = newTokenHashHex;
		this.expiresAt = newExpiresAt;
		this.usedAt = null; // 재발급 시 사용 기록 초기화
	}

}
