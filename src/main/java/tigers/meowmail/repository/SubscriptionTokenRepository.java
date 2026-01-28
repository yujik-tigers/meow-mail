package tigers.meowmail.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tigers.meowmail.entity.SubscriptionToken;

public interface SubscriptionTokenRepository extends JpaRepository<SubscriptionToken, Long> {

	Optional<SubscriptionToken> findByTokenHashHex(String tokenHashHex);

}
