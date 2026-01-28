package tigers.meowmail.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tigers.meowmail.entity.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

	Optional<Subscriber> findByEmail(String email);

}
