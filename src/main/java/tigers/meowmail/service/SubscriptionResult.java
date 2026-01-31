package tigers.meowmail.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubscriptionResult {

	CONFIRMED(
		"Subscription Confirmed",
		"You're officially a cat person!",
		"집사가 되신 걸 환영해요!\n매일 배달되는 귀여운 고양이 소식과 함께\n기분 좋은 하루를 시작해 보세요.",
		"Welcome to the club!\nGet ready for your daily dose of fluff.\nPurr-fect stories are headed to your inbox.",
		"info"
	),

	ALREADY_ACTIVE(
		"Already Subscribed",
		"You're already on the list!",
		"이미 구독 중이시네요!\n고양이들은 이미 당신의 메일함으로\n달려갈 준비를 마쳤답니다.",
		"You're already part of the family!\nKeep an eye on your inbox—\nyour daily cat is on the way.",
		"info"
	),

	TOKEN_INVALID(
		"Invalid Link",
		"Oops! Something went wrong",
		"이런, 꼬인 실타래처럼 링크가 잘못되었어요.\n올바른 인증 링크인지 다시 확인해 주세요.",
		"Oops! This link is a bit tangled.\nPlease double-check your confirmation email.",
		"error"
	),

	TOKEN_EXPIRED(
		"Link Expired",
		"This link has expired",
		"인증 링크가 낮잠을 자러 갔나 봐요.\n보안을 위해 다시 한번 인증 메일을 요청해 주세요.",
		"This link is past its nap time.\nFor your security, please request\na new confirmation email.",
		"error"
	),

	TOKEN_USED(
		"Link Already Used",
		"Already confirmed",
		"이미 사용된 링크예요.\n발바닥 도장이 이미 꾹 찍혔답니다!",
		"This link has already been used.\nYour paw-print is already on the list!",
		"error"
	),

	SUBSCRIBER_NOT_FOUND(
		"Subscriber Not Found",
		"We couldn't find you",
		"구독 정보를 찾을 수 없어요.\n혹시 다른 이메일로 신청하셨나요?",
		"We couldn't find your subscription.\nAre you sure this is the right email address?",
		"error"
	);

	private final String result;
	private final String title_en;
	private final String message_ko;
	private final String message_en;
	private final String type;

}
