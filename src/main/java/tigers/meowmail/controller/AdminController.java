package tigers.meowmail.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.service.EmailService;
import tigers.meowmail.service.ImageService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

	// quotes 3종 + memes 3종 모두 상태를 반환 (존재 여부는 true/false)
	private static final List<String> TYPES = List.of(
		"quotes-eng", "quotes-kor", "quotes-none",
		"memes-eng", "memes-kor", "memes-none"
	);

	private final ImageService imageService;
	private final EmailService emailService;
	private final AppProperties appProperties;

	@GetMapping
	public String showAdminPage(Model model) {
		ZoneId zoneId = ZoneId.of(appProperties.timezone());
		LocalDate today = LocalDate.now(zoneId);
		List<String> dates = IntStream.rangeClosed(0, 7)
			.mapToObj(i -> today.plusDays(i).toString())
			.toList();
		model.addAttribute("dates", dates);
		model.addAttribute("today", today.toString());
		return "view-admin";
	}

	@ResponseBody
	@GetMapping("/api/images")
	public ResponseEntity<Map<String, Object>> getImageStatus(@RequestParam String date) {
		List<Path> paths = imageService.findImagePaths(date);
		Map<String, Boolean> imageStatus = new HashMap<>();
		for (String key : TYPES) {
			boolean exists = paths.stream()
				.anyMatch(p -> p.getFileName().toString().startsWith(date + "-" + key + "."));
			imageStatus.put(key, exists);
		}
		String type = imageService.resolveType(date).name();
		return ResponseEntity.ok(Map.of("date", date, "type", type, "images", imageStatus));
	}

	@ResponseBody
	@PostMapping("/api/images/fetch")
	public ResponseEntity<Void> fetchImages(@RequestParam String date,
		@RequestParam(required = false) String key,
		@RequestParam(required = false) String type) {
		if (key != null) {
			// key로 개별 요청 시: 이미 반대 타입의 이미지가 존재하면 409
			List<Path> existingPaths = imageService.findImagePaths(date);
			boolean isQuoteKey = key.startsWith("quotes-");
			boolean hasConflict = existingPaths.stream().anyMatch(p -> {
				String name = p.getFileName().toString();
				return isQuoteKey ? name.contains("-memes-") : name.contains("-quotes-");
			});
			if (hasConflict) {
				return ResponseEntity.status(HttpStatus.CONFLICT).build();
			}
			imageService.fetchAndSaveImageByKey(date, key);
		} else if (type != null) {
			imageService.fetchAndSaveImages(date, ImageService.ImageType.valueOf(type.toUpperCase()));
		} else {
			imageService.fetchAndSaveImages(date);
		}
		return ResponseEntity.ok().build();
	}

	@ResponseBody
	@DeleteMapping("/api/images")
	public ResponseEntity<Void> deleteImages(@RequestParam String date, @RequestParam(required = false) String lang) {
		if (lang != null) {
			imageService.deleteImageByKey(date, lang);
		} else {
			imageService.deleteImages(date);
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api/images/preview")
	public void previewImage(@RequestParam String date, @RequestParam String lang, HttpServletResponse response) throws IOException {
		List<Path> paths = imageService.findImagePaths(date);
		Optional<Path> imagePath = paths.stream()
			.filter(p -> p.getFileName().toString().startsWith(date + "-" + lang + "."))
			.findFirst();

		if (imagePath.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Path path = imagePath.get();
		String filename = path.getFileName().toString();
		String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
		String contentType = switch (ext) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "gif" -> "image/gif";
			case "webp" -> "image/webp";
			default -> "image/png";
		};

		response.setContentType(contentType);
		response.setContentLengthLong(Files.size(path));
		Files.copy(path, response.getOutputStream());
	}

	@ResponseBody
	@PostMapping("/api/email/test")
	public ResponseEntity<Void> sendTestEmail() {
		emailService.sendImageEmail();
		return ResponseEntity.ok().build();
	}

}
