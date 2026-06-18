// screens-extra.jsx — 빈 화면(empty state) · 모달 · 반응형 비교
const E = () => window.JR_DATA;

// ---------- 따뜻한 스폿 일러스트 (단순 기하 도형) ----------
function Spot({ kind = 'sprout', size = 132 }) {
  if (kind === 'sun') {
    return (
      <svg width={size} height={size} viewBox="0 0 120 120" fill="none">
        <circle cx="60" cy="60" r="56" fill="#FFF6DC" />
        <g stroke="#FFD45E" strokeWidth="5" strokeLinecap="round">
          {[...Array(10)].map((_, i) => <line key={i} x1="60" y1="22" x2="60" y2="32" transform={`rotate(${i * 36} 60 60)`} />)}
        </g>
        <circle cx="60" cy="60" r="22" fill="#FFD45E" />
        <circle cx="52" cy="57" r="3" fill="#43392E" />
        <circle cx="68" cy="57" r="3" fill="#43392E" />
        <path d="M53 66 q7 6 14 0" stroke="#43392E" strokeWidth="3" strokeLinecap="round" fill="none" />
      </svg>
    );
  }
  // sprout in pot (단순 도형)
  return (
    <svg width={size} height={size} viewBox="0 0 120 120" fill="none">
      <circle cx="60" cy="60" r="56" fill="#FFF6DC" />
      <path d="M40 78 h40 l-4 22 a5 5 0 0 1-5 4 h-22 a5 5 0 0 1-5-4 z" fill="#F0D9B8" />
      <rect x="36" y="73" width="48" height="9" rx="4.5" fill="#E7C79B" />
      <path d="M60 78 V52" stroke="#7FBF8E" strokeWidth="5" strokeLinecap="round" />
      <path d="M60 60 C49 58 43 49 45 40 C56 40 64 49 60 60 Z" fill="#8FD9A8" />
      <path d="M60 54 C71 52 77 43 75 34 C64 34 56 43 60 54 Z" fill="#A8E0BB" />
      <circle cx="84" cy="34" r="8" fill="#FFD45E" />
      <g stroke="#FFD45E" strokeWidth="2.6" strokeLinecap="round">
        {[...Array(8)].map((_, i) => <line key={i} x1="84" y1="22" x2="84" y2="25" transform={`rotate(${i * 45} 84 34)`} />)}
      </g>
    </svg>
  );
}

function EmptyState({ spot, title, desc, cta, compact }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: compact ? 32 : 48 }}>
      <Spot kind={spot} size={compact ? 124 : 140} />
      <div className="jr-h1" style={{ marginTop: 22, fontSize: compact ? 21 : 24 }}>{title}</div>
      <div className="jr-body" style={{ color: 'var(--text-sub)', marginTop: 10, maxWidth: 300, lineHeight: 1.6 }}>{desc}</div>
      {cta && <button className="jr-btn jr-btn--primary jr-btn--lg" style={{ marginTop: 26 }}><Icon name="plus" size={22} stroke={2.6} /> {cta}</button>}
    </div>
  );
}

// 빈 타임라인 (모바일)
function EmptyTimelineMobile() {
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 16px 12px' }}>
          <button style={{ border: 'none', background: 'transparent', color: 'var(--text)', cursor: 'pointer', padding: 4 }}><Icon name="back" size={24} /></button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Avatar name="서지호" size="lg" />
            <div>
              <div className="jr-h1" style={{ fontSize: 22, whiteSpace: 'nowrap' }}>서지호</div>
              <div style={{ fontSize: 13.5, color: 'var(--text-sub)', whiteSpace: 'nowrap', marginTop: 2 }}>햇살반 · 관찰기록 0개</div>
            </div>
          </div>
        </div>
        <EmptyState compact spot="sprout"
          title="아직 기록이 없어요"
          desc={<>지호의 첫 메모를 남겨볼까요?<br/>한 줄이면 충분해요.</>}
          cta="첫 메모 남기기" />
        <BottomTab active="children" />
      </div>
    </PhoneFrame>
  );
}

// 빈 홈 (데스크톱)
function EmptyHomeDesktop() {
  return (
    <DesktopWindow url="app.jaram.kr/home">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="home" teacher={E().teacher} cls={E().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 48px', display: 'flex', flexDirection: 'column' }}>
          <div>
            <div className="jr-display">민서 선생님, 좋은 하루예요 ☀️</div>
            <div style={{ fontSize: 15, color: 'var(--text-sub)', marginTop: 6 }}>{E().date} · {E().cls}</div>
          </div>
          <EmptyState spot="sun"
            title="오늘은 아직 메모가 없어요"
            desc="수업 틈틈이 떠오르는 순간을 한 줄로 남겨주세요. 밤에 AI가 일지로 정리해드릴게요."
            cta="오늘 첫 메모 남기기" />
        </div>
      </div>
    </DesktopWindow>
  );
}

// ---------- 모달: 재분석 확인 ----------
function ReanalyzeModal({ compact }) {
  const inner = (
    <>
      {/* 딤 */}
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(40,30,20,.32)', zIndex: 20 }} />
      <div style={{ position: 'absolute', inset: 0, zIndex: 21, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: compact ? 24 : 40 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 24, padding: compact ? '26px 22px 22px' : '30px 28px 24px', width: '100%', maxWidth: compact ? 320 : 380, boxShadow: 'var(--shadow-lg)', textAlign: 'center' }}>
          <div style={{ width: 60, height: 60, borderRadius: '50%', background: 'var(--brand-100)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
            <Icon name="sparkle" size={30} style={{ color: 'var(--brand-700)' }} />
          </div>
          <div className="jr-h2" style={{ fontSize: 20 }}>오늘 분석 내용이 이미 있어요</div>
          <div className="jr-body" style={{ color: 'var(--text-sub)', marginTop: 8, lineHeight: 1.6 }}>다시 만들면 이전 일지 초안은 새 내용으로 바뀌어요. 다시 분석할까요?</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7, justifyContent: 'center', fontSize: 12.5, color: 'var(--text-faint)', background: 'var(--surface-soft)', borderRadius: 999, padding: '8px 14px', marginTop: 16, whiteSpace: 'nowrap' }}>
            <Icon name="check" size={14} stroke={2.6} style={{ color: 'var(--success)' }} /> 메모는 그대로 보관돼요
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
            <button className="jr-btn jr-btn--ghost" style={{ flex: 1 }}>취소</button>
            <button className="jr-btn jr-btn--primary" style={{ flex: 1 }}><Icon name="refresh" size={19} /> 다시 분석</button>
          </div>
        </div>
      </div>
    </>
  );
  if (compact) {
    return (
      <PhoneFrame>
        {/* 배경: 흐릿한 재분석 화면 */}
        <div style={{ height: '100%', padding: '0 18px', filter: 'blur(1px)', opacity: .7 }}>
          <div style={{ padding: '12px 0' }}><div className="jr-h1" style={{ fontSize: 21 }}>오늘 분석</div></div>
          <div className="jr-card jr-card--soft" style={{ height: 70, marginBottom: 10 }} />
          <div className="jr-card jr-card--soft" style={{ height: 70, marginBottom: 10 }} />
        </div>
        {inner}
      </PhoneFrame>
    );
  }
  return (
    <DesktopWindow url="app.jaram.kr/journal">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="journal" teacher={E().teacher} cls={E().cls} />
        <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
          <div style={{ padding: '34px 40px', filter: 'blur(1.5px)', opacity: .6 }}>
            <div className="jr-h1">하루 일지 초안</div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginTop: 24 }}>
              {[0, 1, 2, 3].map(i => <div key={i} className="jr-card jr-card--soft" style={{ height: 96 }} />)}
            </div>
          </div>
          {inner}
        </div>
      </div>
    </DesktopWindow>
  );
}

// ---------- 반응형 비교 1장 ----------
function Anno({ children, color = 'var(--brand-700)' }) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, fontSize: 13.5, lineHeight: 1.5, color: 'var(--text)' }}>
      <span style={{ width: 7, height: 7, borderRadius: '50%', background: color, marginTop: 6, flex: '0 0 auto' }} />
      <span>{children}</span>
    </div>
  );
}

function ResponsiveCompare() {
  return (
    <div className="jr" style={{ background: 'var(--bg)', padding: 44, width: 1320 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 4 }}>
        <span className="jr-display">반응형 한눈에 비교</span>
        <span style={{ fontSize: 15, color: 'var(--text-sub)' }}>같은 'AI 일지' 화면, 두 사용 맥락</span>
      </div>
      <div style={{ fontSize: 14, color: 'var(--text-faint)', marginBottom: 30 }}>브레이크포인트 768px · 모바일=입력 속도 / 데스크톱=검토·편집 편의</div>

      <div style={{ display: 'flex', gap: 36, alignItems: 'flex-start' }}>
        {/* 모바일 */}
        <div style={{ flex: '0 0 auto', textAlign: 'center', width: 298 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: '#FFF6DC', borderRadius: 999, padding: '7px 16px', marginBottom: 16, fontWeight: 800, fontSize: 13.5, whiteSpace: 'nowrap' }}>
            🌞 낮 · 모바일 <span style={{ color: 'var(--text-sub)', fontWeight: 600 }}>&lt;768px</span>
          </div>
          <div style={{ width: 298, height: 602, overflow: 'hidden' }}>
            <div style={{ transform: 'scale(0.72)', transformOrigin: 'top left' }}>
              <JournalMobile state="draft" />
            </div>
          </div>
        </div>

        {/* 가운데 화살표 + 주석 */}
        <div style={{ flex: '1 1 0', minWidth: 200, paddingTop: 54 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, marginBottom: 22, color: 'var(--brand-700)' }}>
            <Icon name="swap" size={30} />
            <span style={{ fontWeight: 800, fontSize: 15, color: 'var(--text)' }}>768px</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 15, background: 'var(--surface)', borderRadius: 18, padding: 20, boxShadow: 'var(--shadow-sm)' }}>
            <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', textTransform: 'uppercase', letterSpacing: '.05em' }}>무엇이 달라지나</div>
            <Anno>내비게이션: <b>하단 탭바 + 플로팅 메모</b> → <b>좌측 고정 사이드바</b></Anno>
            <Anno color="#7EC4E8">레이아웃: <b>단일 컬럼(위→아래)</b> → <b>2단(좌 메모 / 우 AI 결과)</b></Anno>
            <Anno color="#8FD9A8">우선순위: <b>입력 10초</b> 중심 → <b>나란히 검토·편집</b> 중심</Anno>
            <Anno color="#C9A8E8">액션: 하단 고정 버튼 → 우측 정렬 버튼 그룹</Anno>
            <Anno color="#FF9E80">동일 유지: 컬러·타이포·라운딩·누리 영역 색 코딩 — <b>하나의 디자인 시스템</b></Anno>
          </div>
        </div>

        {/* 데스크톱 */}
        <div style={{ flex: '0 0 auto', textAlign: 'center', width: 543 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: '#EAF4FB', borderRadius: 999, padding: '7px 16px', marginBottom: 16, fontWeight: 800, fontSize: 13.5, whiteSpace: 'nowrap' }}>
            🌙 밤 · 데스크톱 <span style={{ color: 'var(--text-sub)', fontWeight: 600 }}>≥768px</span>
          </div>
          <div style={{ width: 543, height: 370, overflow: 'hidden' }}>
            <div style={{ transform: 'scale(0.46)', transformOrigin: 'top left' }}>
              <JournalDesktop state="draft" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { Spot, EmptyState, EmptyTimelineMobile, EmptyHomeDesktop, ReanalyzeModal, ResponsiveCompare });
