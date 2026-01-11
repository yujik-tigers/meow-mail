package tigers.meowmail.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tigers.meowmail.subscription.entity.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

}
