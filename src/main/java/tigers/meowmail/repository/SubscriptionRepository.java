package tigers.meowmail.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tigers.meowmail.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findByEmail(String email);

}
