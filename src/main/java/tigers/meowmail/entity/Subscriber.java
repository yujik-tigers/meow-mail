package tigers.meowmail.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Subscriber {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 320)
	private String email; // lower-case normalized

	@Column(nullable = false, length = 5)
	private String time; // HH:mm format

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SubscriptionStatus status;

	private Instant confirmedAt;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public void markActive(Instant activeAt) {
		this.status = SubscriptionStatus.ACTIVE;
		this.confirmedAt = activeAt;
		this.updatedAt = activeAt;
	}

	public void markInactive(Instant inactiveAt) {
		this.status = SubscriptionStatus.INACTIVE;
		this.updatedAt = inactiveAt;
	}

	public void updateTime(String time, Instant updatedAt) {
		this.time = time;
		this.updatedAt = updatedAt;
	}

}
