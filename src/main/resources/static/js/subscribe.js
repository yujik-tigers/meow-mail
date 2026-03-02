// ── 유틸 ──
function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function hideAllEmailStatus() {
    ['email-error', 'verify-pending', 'verify-expired', 'verify-success', 'verify-fail']
        .forEach(id => document.getElementById(id).classList.add('hidden'));
}

function show(id) {
    document.getElementById(id).classList.remove('hidden');
}

function hide(id) {
    document.getElementById(id).classList.add('hidden');
}

// ── 상태 변수 ──
let verified = false;
let timerInterval = null;
let activeEvtSource = null;
let pollInterval = null;
let currentEmail = null;
let verifiedEmail = null;

// ── 버튼 활성화 ──
function updateSendBtnState() {
    const email = document.getElementById('email').value.trim();
    document.getElementById('send-verify-btn').disabled = !isValidEmail(email);
}

function updateSubscribeBtnState() {
    document.getElementById('subscribe-btn').disabled = !verified;
}

// ── 타이머 ──
function startTimer() {
    clearInterval(timerInterval);
    let remaining = 30 * 60; // 30분 (초)
    const timerEl = document.getElementById('timer');

    timerInterval = setInterval(() => {
        remaining--;
        const m = String(Math.floor(remaining / 60)).padStart(2, '0');
        const s = String(remaining % 60).padStart(2, '0');
        timerEl.textContent = `${m}:${s}`;

        if (remaining <= 0) {
            clearInterval(timerInterval);
            if (!verified) {
                hideAllEmailStatus();
                show('verify-expired');
                document.getElementById('email').classList.remove('input-verified');
                document.getElementById('email').readOnly = false;
                updateSendBtnState();
                if (activeEvtSource) {
                    activeEvtSource.close();
                    activeEvtSource = null;
                }
                stopPolling();
            }
        }
    }, 1000);
}

// ── 인증 성공 처리 (SSE/폴링 공통) ──
function onVerificationSuccess() {
    if (verified) return;
    verified = true;
    verifiedEmail = currentEmail;
    clearInterval(timerInterval);
    stopPolling();
    if (activeEvtSource) {
        activeEvtSource.close();
        activeEvtSource = null;
    }
    hideAllEmailStatus();
    // 인증된 이메일로 입력 필드 값 고정
    document.getElementById('email').value = verifiedEmail;
    document.getElementById('email').classList.add('input-verified');
    document.getElementById('email').readOnly = true;
    show('verify-success');
    hide('email-hint');
    document.getElementById('verify-btn-label').textContent = '인증완료';
    document.getElementById('send-verify-btn').disabled = true;
    updateSubscribeBtnState();
}

// ── 폴링 폴백 ──
function startPolling(email) {
    stopPolling();
    pollInterval = setInterval(async () => {
        if (verified) {
            stopPolling();
            return;
        }
        try {
            const res = await fetch(`/api/subscriptions/verify/status?email=${encodeURIComponent(email)}`);
            if (res.ok) {
                const data = await res.json();
                if (data.verified) onVerificationSuccess();
            }
        } catch { /* 네트워크 오류 무시 */
        }
    }, 2000);
}

function stopPolling() {
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
}

// ── SSE 연결 ──
function openSse(email) {
    if (activeEvtSource) {
        activeEvtSource.close();
    }

    const evtSource = new EventSource(`/api/subscriptions/verify/events?email=${encodeURIComponent(email)}`);
    activeEvtSource = evtSource;

    evtSource.addEventListener('verified', function (e) {
        evtSource.close();
        activeEvtSource = null;

        const data = JSON.parse(e.data);
        if (data.code === 'success') {
            onVerificationSuccess();
        } else {
            stopPolling();
            clearInterval(timerInterval);
            hideAllEmailStatus();
            show('verify-fail');
            document.getElementById('verify-fail-msg').textContent =
                data.message || '이메일 인증에 실패했어요 다시 시도해 주세요';
            document.getElementById('email').readOnly = false;
            updateSendBtnState();
        }
    });

    evtSource.onerror = function () {
        // SSE 연결 오류 시 폴링으로 전환
        evtSource.close();
        activeEvtSource = null;
        if (!verified) startPolling(email);
    };
}

// ── 인증 버튼 로딩 상태 ──
function setVerifyBtnLoading(loading) {
    document.getElementById('verify-btn-spinner').classList.toggle('hidden', !loading);
    document.getElementById('verify-btn-label').textContent = loading ? '전송 중' : '인증하기';
}

// ── 인증 취소 ──
function cancelVerification() {
    verified = false;
    verifiedEmail = null;
    currentEmail = null;
    clearInterval(timerInterval);
    if (activeEvtSource) {
        activeEvtSource.close();
        activeEvtSource = null;
    }
    stopPolling();
    hideAllEmailStatus();
    document.getElementById('email').classList.remove('input-error', 'input-verified');
    document.getElementById('email').readOnly = false;
    document.getElementById('send-verify-btn').disabled = false;
    document.getElementById('verify-btn-label').textContent = '인증하기';
    updateSendBtnState();
}

// ── 인증 메일 발송 ──
document.getElementById('send-verify-btn').addEventListener('click', async function () {
    const email = document.getElementById('email').value.trim();
    const btn = this;

    btn.disabled = true;
    setVerifyBtnLoading(true);
    hideAllEmailStatus();
    verified = false;
    currentEmail = email;
    if (activeEvtSource) {
        activeEvtSource.close();
        activeEvtSource = null;
    }
    stopPolling();
    document.getElementById('email').classList.remove('input-verified');
    document.getElementById('email').readOnly = false;
    updateSubscribeBtnState();

    try {
        const res = await fetch('/api/subscriptions/verify', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({email}),
        });

        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            show('email-error');
            document.getElementById('email-error-msg').textContent =
                body.message || '요청 중 오류가 발생했어요 잠시 후 다시 시도해 주세요';
            document.getElementById('email').classList.add('input-error');
            document.getElementById('email').readOnly = false;
            setVerifyBtnLoading(false);
            btn.disabled = false;
            return;
        }

        document.getElementById('email').classList.remove('input-error');
        document.getElementById('email').readOnly = true;  // 인증 대기 중 이메일 수정 불가
        setVerifyBtnLoading(false);
        show('verify-pending');
        startTimer();
        openSse(email);
        // 인증 메일 발송 후 재발송 방지 (타이머가 끝나면 다시 활성화)
        btn.disabled = true;

    } catch {
        show('email-error');
        document.getElementById('email-error-msg').textContent =
            '네트워크 오류가 발생했어요 잠시 후 다시 시도해 주세요';
        document.getElementById('email').classList.add('input-error');
        document.getElementById('email').readOnly = false;
        setVerifyBtnLoading(false);
        btn.disabled = false;
    }
});

// ── 구독하기 ──
document.getElementById('subscribe-btn').addEventListener('click', async function () {
    // 인증된 이메일로만 구독 요청
    if (!verifiedEmail) {
        showDone(false, '구독 실패', '이메일 인증을 먼저 완료해 주세요');
        return;
    }

    const btn = this;
    btn.disabled = true;

    try {
        const res = await fetch('/api/subscriptions', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({email: verifiedEmail}),
        });

        if (res.ok) {
            showDone(true, '구독 완료',
                `${verifiedEmail}으로\n매일 귀여운 고양이 편지를 보내드릴게요`);
        } else {
            const body = await res.json().catch(() => ({}));
            showDone(false, '구독 실패', body.message || '구독 중 오류가 발생했어요');
        }
    } catch {
        showDone(false, '구독 실패', '네트워크 오류가 발생했어요 잠시 후 다시 시도해 주세요');
    }
});

// ── 완료 화면 ──
function showDone(success, title, message) {
    document.getElementById('done-icon').textContent = success ? '🐈' : '😿';
    document.getElementById('done-title').textContent = title;
    document.getElementById('done-message').textContent = message;
    if (!success) {
        document.getElementById('done-card').style.boxShadow = '0 18px 35px rgba(248,113,113,0.22)';
    }
    document.querySelectorAll('.step').forEach(el => el.classList.remove('active'));
    document.getElementById('step-done').classList.add('active');
}

// ── 이벤트 리스너 ──
document.getElementById('email').addEventListener('input', function () {
    if (verified) return;
    // 인증 대기 중 이메일 수정 시 진행 중인 인증 프로세스 중단
    if (currentEmail !== null && this.value.trim() !== currentEmail) {
        clearInterval(timerInterval);
        if (activeEvtSource) {
            activeEvtSource.close();
            activeEvtSource = null;
        }
        stopPolling();
        currentEmail = null;
        document.getElementById('send-verify-btn').disabled = false;
    }
    hideAllEmailStatus();
    this.classList.remove('input-error', 'input-verified');
    updateSendBtnState();
});

document.getElementById('email').addEventListener('blur', function () {
    if (verified) return;
    const v = this.value.trim();
    if (v && !isValidEmail(v)) {
        show('email-error');
        document.getElementById('email-error-msg').textContent = '이메일 형식을 다시 확인해 주세요';
        this.classList.add('input-error');
    }
});

document.getElementById('cancel-verify-btn').addEventListener('click', function () {
    cancelVerification();
});
