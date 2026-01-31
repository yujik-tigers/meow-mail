package tigers.meowmail.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UnsubscriptionResult {

	SUCCESS(
		"구독 해지 완료",
		"구독이 정상적으로 해지되었어요.\n더 이상 메일이 발송되지 않습니다.\n다시 만나고 싶을 땐 언제든 돌아와 주세요!",
		"success"
	),

	NOT_FOUND(
		"구독 정보 없음",
		"해당 이메일로 등록된 구독 정보를 찾을 수 없어요.\n혹시 다른 이메일로 구독하셨나요?",
		"error"
	),

	ALREADY_INACTIVE(
		"이미 해지됨",
		"이미 구독이 해지된 상태예요.\n다시 구독하고 싶으시다면 구독 신청 메일을 보내주세요!",
		"info"
	);

	private final String title;
	private final String message;
	private final String type;

}
