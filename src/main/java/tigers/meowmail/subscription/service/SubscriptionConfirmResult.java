package tigers.meowmail.subscription.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubscriptionConfirmResult {

	CONFIRMED(
		"Subscription Confirmed",
		"Meow-!",
		"이메일 인증에 성공해서 구독이 완료되었어요.\n매일 귀여운 고양이를 보내드릴게요!",
		"Get ready for some meow-velous memes delivered to your inbox!",
		"info"
	),

	ALREADY_ACTIVE(
		"Already Subscribed",
		"You're already on the list!",
		"이미 구독이 활성화되어 있어요.\n곧 메일함에서 고양이를 만나실 수 있어요!",
		"You're already subscribed — keep an eye on your inbox!",
		"info"
	),

	TOKEN_INVALID(
		"Invalid Link",
		"Oops! Something went wrong",
		"유효하지 않은 인증 링크네요!\n다시 시도해 주세요.",
		"This confirmation link is invalid.\nPlease try again.",
		"error"
	),

	TOKEN_EXPIRED(
		"Link Expired",
		"This link has expired",
		"인증 링크가 만료되었어요.\n새로운 인증 메일을 요청해 주세요.",
		"This confirmation link has expired!\nPlease request a new one.",
		"error"
	),

	TOKEN_USED(
		"Link Already Used",
		"Already confirmed",
		"이미 사용된 인증 링크예요.",
		"This confirmation link has already been used.",
		"error"
	),

	SUBSCRIBER_NOT_FOUND(
		"Subscriber Not Found",
		"We couldn't find you",
		"구독 정보를 찾을 수 없어요.\n이메일 주소를 다시 확인해 주세요.",
		"We couldn't find a subscription associated with this email.",
		"error"
	),

	SUBSCRIBER_INACTIVE(
		"Subscription Inactive",
		"Subscription is inactive",
		"현재 구독이 비활성화된 상태예요.\n다시 구독을 진행해 주세요.",
		"Your subscription is currently inactive.\nPlease subscribe again.",
		"warning"
	);

	private final String result;
	private final String title_en;
	private final String message_ko;
	private final String message_en;
	private final String type;

}
