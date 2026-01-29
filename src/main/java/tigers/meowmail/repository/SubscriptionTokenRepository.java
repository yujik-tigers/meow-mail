package tigers.meowmail.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tigers.meowmail.entity.Subscriber;
import tigers.meowmail.entity.SubscriptionToken;

public interface SubscriptionTokenRepository extends JpaRepository<SubscriptionToken, Long> {

	Optional<SubscriptionToken> findBySubscriber(Subscriber subscriber);

	Optional<SubscriptionToken> findByTokenHashHex(String tokenHashHex);

}
