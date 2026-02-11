package tigers.meowmail.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class EmitterRepository {

	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	public SseEmitter save(String email, SseEmitter emitter) {
		emitters.put(email, emitter);
		return emitter;
	}

	public void delete(String email) {
		emitters.remove(email);
	}

	public Optional<SseEmitter> findByEmail(String email) {
		return Optional.ofNullable(emitters.get(email));
	}

}
