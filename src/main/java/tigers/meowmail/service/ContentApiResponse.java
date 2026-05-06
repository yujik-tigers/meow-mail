package tigers.meowmail.service;

import com.fasterxml.jackson.annotation.JsonProperty;

record ContentApiResponse(
	@JsonProperty("status_code")
	int statusCode,
	@JsonProperty("status_message")
	String statusMessage,
	DailyMemeContent content) {

}
