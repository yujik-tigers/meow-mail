package tigers.meowmail.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tigers.meowmail.entity.Subscription;
import tigers.meowmail.entity.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findByEmail(String email);

	List<Subscription> findByStatusAndTime(SubscriptionStatus status, String time);

}
