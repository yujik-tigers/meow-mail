package tigers.meowmail.subscription.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class SubscriptionToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long subscriberId;

	@Column(nullable = false, length = 64)
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

}
