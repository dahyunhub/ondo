// screens-journal.jsx — 화면3 AI 일지 생성·검토 (로딩/초안/에러 + 재분석)
const JD = () => window.JR_DATA;

// A · 한 아이 분석  /  B · 오늘 전체 분석 — UI 구분
function AnalysisMenu({ active = 'B', compact }) {
  const items = [
    { id: 'A', t: '한 아이 분석', s: '개인 관찰 평가', ic: 'me' },
    { id: 'B', t: '오늘 전체 분석', s: '하루 일지', ic: 'journal' },
  ];
  return (
    <div style={{ display: 'flex', gap: 10 }}>
      {items.map(it => {
        const on = it.id === active;
        return (
          <div key={it.id} style={{ flex: 1, minWidth: 0, display: 'flex', alignItems: 'center', gap: 10, padding: compact ? '10px 12px' : '12px 16px', borderRadius: 14, overflow: 'hidden',
            background: on ? 'var(--brand-100)' : 'var(--surface)', border: '2px solid ' + (on ? 'var(--brand-500)' : 'var(--hair-strong)') }}>
            <span style={{ width: 34, height: 34, borderRadius: 10, flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', background: on ? 'var(--brand-300)' : 'var(--surface-soft)', color: 'var(--text)' }}><Icon name={it.ic} size={19} /></span>
            <div style={{ lineHeight: 1.25, minWidth: 0 }}>
              <div style={{ fontSize: compact ? 13 : 14, fontWeight: 800, whiteSpace: 'nowrap' }}><span style={{ color: 'var(--text-faint)', marginRight: 4 }}>{it.id}</span>{it.t}</div>
              <div style={{ fontSize: 11.5, color: 'var(--text-sub)', whiteSpace: 'nowrap' }}>{it.s}</div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function AreaBlock({ area, txt }) {
  const a = AREAS[area];
  return (
    <div className="jr-area-block" style={{ '--strip': a.color }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 7 }}>
        <NuriChip area={area} />
        <Icon name="pencil" size={15} style={{ color: 'var(--text-faint)', marginLeft: 'auto' }} />
      </div>
      <div style={{ fontSize: 14.5, lineHeight: 1.6, color: 'var(--text)' }}>{txt}</div>
    </div>
  );
}

function TodayMemoList({ withNew, compact }) {
  const memos = withNew ? [...JD().newMemos, ...JD().todayMemos] : JD().todayMemos;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {memos.map((m, i) => (
        <div key={i} className="jr-card jr-card--soft" style={{ padding: '13px 15px', border: m.isNew ? '1.5px solid var(--accent-new)' : 'none' }}>
          <div className="jr-memo">
            <Avatar name={m.name} size="sm" />
            <div className="body">
              <div className="top">
                <span className="name" style={{ fontSize: 14 }}>{m.name}</span>
                <NuriChip area={m.area} />
                {m.isNew && <span className="jr-badge-new">NEW</span>}
                <span className="time">{m.time}</span>
              </div>
              <div className="txt" style={{ fontSize: 14 }}>{m.txt}</div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function LoadingView({ compact }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: 30 }}>
      <LogoLoader size={compact ? 116 : 132} />
      <div className="jr-h1" style={{ marginTop: 22, fontSize: compact ? 21 : 24 }}>오늘 메모를 모아<br/>일지를 쓰고 있어요</div>
      <div className="jr-body" style={{ color: 'var(--text-sub)', marginTop: 10 }}>잠시만요 — 약 15초 걸려요 ☀️</div>
      <div style={{ display: 'flex', gap: 8, marginTop: 22 }}>
        {[0, 1, 2].map(i => <span key={i} className="jr-dot-pulse" style={{ width: 9, height: 9, borderRadius: '50%', background: 'var(--brand-500)', animationDelay: i * 0.22 + 's' }} />)}
      </div>
      <div style={{ marginTop: 30, display: 'flex', alignItems: 'center', gap: 7, fontSize: 13, color: 'var(--text-faint)', background: 'var(--surface-soft)', padding: '9px 16px', borderRadius: 999, whiteSpace: 'nowrap' }}>
        <Icon name="check" size={15} stroke={2.6} style={{ color: 'var(--success)' }} /> 메모는 안전하게 저장돼 있어요
      </div>
    </div>
  );
}

function ErrorView({ compact }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: compact ? 28 : 40 }}>
      <div style={{ width: 88, height: 88, borderRadius: '50%', background: 'rgba(240,140,125,.14)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
        <Icon name="heart" size={42} style={{ color: 'var(--warn)' }} stroke={2} />
      </div>
      <div className="jr-h1" style={{ fontSize: compact ? 21 : 24 }}>앗, 분석이 잠시 멈췄어요</div>
      <div className="jr-body" style={{ color: 'var(--text-sub)', marginTop: 8 }}>네트워크가 잠깐 불안정했나 봐요.</div>

      <div className="jr-banner jr-banner--warn" style={{ marginTop: 24, padding: '18px 20px', borderRadius: 20, maxWidth: 340 }}>
        <Icon name="check" size={26} stroke={2.4} style={{ color: 'var(--warn)', flex: '0 0 auto' }} />
        <div style={{ textAlign: 'left' }}>
          <div style={{ fontSize: 16.5, fontWeight: 800, color: 'var(--text)' }}>작성하신 메모는 그대로 저장돼 있어요.</div>
          <div style={{ fontSize: 13, color: 'var(--text-sub)', marginTop: 3 }}>하나도 사라지지 않았으니 안심하세요.</div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 10, marginTop: 26, width: '100%', maxWidth: 340 }}>
        <button className="jr-btn jr-btn--primary" style={{ flex: 1 }}><Icon name="refresh" size={20} /> 다시 시도</button>
        <button className="jr-btn jr-btn--secondary" style={{ flex: 1 }}>메모만 보기</button>
      </div>
    </div>
  );
}

// 덮어쓰기 확인 모달 — 기존 일지를 덮어씀
function OverwriteModal({ onClose, compact }) {
  return (
    <>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(40,30,20,.36)', zIndex: 20 }} />
      <div style={{ position: 'absolute', inset: 0, zIndex: 21, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: compact ? 24 : 40 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 24, padding: compact ? '24px 22px' : '28px 30px', width: '100%', maxWidth: 420, boxShadow: 'var(--shadow-lg)', textAlign: 'center' }}>
          <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--brand-100)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}><Icon name="refresh" size={28} style={{ color: 'var(--brand-700)' }} /></div>
          <div className="jr-h2">기존 일지를 덮어쓸까요?</div>
          <div style={{ fontSize: 14, color: 'var(--text-sub)', lineHeight: 1.55, marginTop: 8 }}>다시 분석하면 <b style={{ color: 'var(--text)' }}>오늘 14:30에 만든 일지</b>가<br/>새 결과로 바뀌어요. 원본 메모는 그대로예요.</div>
          <div style={{ display: 'flex', gap: 10, marginTop: 24 }}>
            <button className="jr-btn jr-btn--secondary" onClick={onClose} style={{ flex: 1 }}>취소</button>
            <button className="jr-btn jr-btn--primary" onClick={onClose} style={{ flex: 1 }}><Icon name="refresh" size={19} /> 덮어쓰고 분석</button>
          </div>
        </div>
      </div>
    </>
  );
}

// 동시 분석 제한 — 이미 진행 중
function BusyView({ compact }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: compact ? 28 : 40 }}>
      <div style={{ width: 88, height: 88, borderRadius: '50%', background: 'var(--brand-100)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
        <Icon name="clock" size={42} style={{ color: 'var(--brand-700)' }} />
      </div>
      <div className="jr-h1" style={{ fontSize: compact ? 21 : 24 }}>분석이 이미 진행 중이에요</div>
      <div className="jr-body" style={{ color: 'var(--text-sub)', marginTop: 8, lineHeight: 1.5 }}>지금 만들고 있는 일지가 끝나면<br/>이어서 진행할 수 있어요.</div>
      <div style={{ marginTop: 22, display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--text-faint)', background: 'var(--surface-soft)', padding: '10px 18px', borderRadius: 999 }}>
        <span className="jr-dot-pulse" style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--brand-500)' }} /> 약 15초 후 완료 예정
      </div>
      <button className="jr-btn jr-btn--secondary" style={{ marginTop: 24 }}>진행 상황 보기</button>
    </div>
  );
}

// ---------- 모바일 ----------
function JournalHeader({ title, sub, onMobile }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: onMobile ? '6px 16px 12px' : 0 }}>
      {onMobile && <button style={{ border: 'none', background: 'transparent', color: 'var(--text)', cursor: 'pointer', padding: 4 }}><Icon name="back" size={24} /></button>}
      <div style={{ lineHeight: 1.2 }}>
        <div className="jr-h1" style={{ fontSize: onMobile ? 21 : 26 }}>{title}</div>
        {sub && <div style={{ fontSize: 13, color: 'var(--text-sub)', marginTop: 2, whiteSpace: 'nowrap' }}>{sub}</div>}
      </div>
    </div>
  );
}

function JournalMobile({ state = 'draft' }) {
  if (state === 'loading') return <PhoneFrame><LoadingView compact /></PhoneFrame>;
  if (state === 'error') return <PhoneFrame><ErrorView compact /></PhoneFrame>;
  if (state === 'busy') return <PhoneFrame><BusyView compact /></PhoneFrame>;

  if (state === 'reanalyze' || state === 'overwrite') {
    return (
      <PhoneFrame>
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          <JournalHeader title="오늘 분석" sub="햇살반 · 6월 14일" onMobile />
          <div style={{ flex: 1, overflow: 'hidden', padding: '0 18px 8px' }}>
            <div className="jr-banner" style={{ marginBottom: 18 }}>
              <Icon name="sparkle" size={22} style={{ color: 'var(--brand-700)', flex: '0 0 auto' }} />
              <span style={{ fontSize: 13.5, fontWeight: 700 }}>새 메모 1건 — 재분석이 필요해요<br/><span style={{ fontWeight: 600, color: 'var(--text-sub)' }}>추가된 메모를 반영해 다시 만들까요?</span></span>
            </div>
            <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 6, whiteSpace: 'nowrap' }}>오늘 메모 <span className="jr-badge-count">5</span></div>
            <TodayMemoList withNew compact />
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, margin: '22px 0 12px' }}>
              <hr className="jr-divider" style={{ flex: 1 }} />
              <span style={{ fontSize: 12, color: 'var(--text-faint)', display: 'flex', alignItems: 'center', gap: 5 }}><Icon name="clock" size={13} /> 이전 분석 · 오늘 14:30 생성</span>
              <hr className="jr-divider" style={{ flex: 1 }} />
            </div>
            <div style={{ opacity: .55, filter: 'grayscale(.4)' }}>
              <div className="jr-area-block" style={{ '--strip': '#c9c2b4', background: 'var(--surface-soft)' }}>
                <div style={{ fontSize: 14, lineHeight: 1.6, color: 'var(--text-sub)' }}>{JD().journalDraft.blocks[0].txt}</div>
              </div>
            </div>
          </div>
          <div style={{ padding: '12px 18px 26px', background: 'linear-gradient(transparent, var(--bg) 30%)' }}>
            <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg"><Icon name="refresh" size={21} /> 다시 분석하기</button>
          </div>
        </div>
        {state === 'overwrite' && <OverwriteModal onClose={() => {}} compact />}
      </PhoneFrame>
    );
  }

  // draft
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <JournalHeader title="하루 일지" sub="햇살반 · 6월 14일 · AI 초안" onMobile />
        <div style={{ padding: '0 18px 14px' }}><AnalysisMenu active="B" compact /></div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '4px 18px 8px', display: 'flex', flexDirection: 'column', gap: 11 }}>
          <div style={{ fontSize: 12.5, color: 'var(--text-faint)', display: 'flex', alignItems: 'center', gap: 5 }}><Icon name="pencil" size={13} /> 카드를 눌러 바로 수정할 수 있어요</div>
          {JD().journalDraft.blocks.slice(0, 3).map((b, i) => <AreaBlock key={i} {...b} />)}
        </div>
        <div style={{ display: 'flex', gap: 9, padding: '12px 18px 26px', background: 'linear-gradient(transparent, var(--bg) 30%)' }}>
          <button className="jr-btn jr-btn--ghost" style={{ flex: '0 0 auto' }}><Icon name="pencil" size={19} /> 수정</button>
          <button className="jr-btn jr-btn--secondary" style={{ flex: '0 0 auto' }}><Icon name="refresh" size={19} /> 다시</button>
          <button className="jr-btn jr-btn--primary" style={{ flex: 1 }}><Icon name="check" size={21} stroke={2.6} /> 확정</button>
        </div>
      </div>
    </PhoneFrame>
  );
}

// ---------- 데스크톱 (2단: 좌 메모 / 우 AI 결과) ----------
function JournalDesktop({ state = 'draft' }) {
  const inner = () => {
    if (state === 'loading') return <LoadingView />;
    if (state === 'error') return <ErrorView />;
    if (state === 'busy') return <BusyView />;
    // draft: 2단
    return (
      <div style={{ display: 'flex', height: '100%' }}>
        {/* 좌: 오늘 메모 */}
        <div style={{ width: 380, flex: '0 0 auto', borderRight: '1px solid var(--hair)', padding: '28px 24px', overflow: 'hidden', background: 'var(--surface-soft)' }}>
          <div style={{ fontSize: 14, fontWeight: 800, color: 'var(--text-sub)', marginBottom: 14, display: 'flex', alignItems: 'center', gap: 7, whiteSpace: 'nowrap' }}>오늘 메모 <span className="jr-badge-count">4</span></div>
          <TodayMemoList />
        </div>
        {/* 우: AI 결과 */}
        <div style={{ flex: 1, overflow: 'hidden', padding: '28px 36px', display: 'flex', flexDirection: 'column' }}>
          <JournalHeader title="하루 일지 초안" sub="6월 14일 · AI가 누리과정 영역별로 정리했어요" />
          <div style={{ margin: '18px 0', maxWidth: 440 }}><AnalysisMenu active="B" /></div>
          <div style={{ flex: 1, overflow: 'hidden', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, alignContent: 'start' }}>
            {JD().journalDraft.blocks.map((b, i) => <AreaBlock key={i} {...b} />)}
          </div>
          <div style={{ display: 'flex', gap: 12, marginTop: 20, justifyContent: 'flex-end' }}>
            <button className="jr-btn jr-btn--ghost"><Icon name="pencil" size={19} /> 수정</button>
            <button className="jr-btn jr-btn--secondary"><Icon name="refresh" size={19} /> 다시 생성</button>
            <button className="jr-btn jr-btn--primary" style={{ paddingLeft: 30, paddingRight: 30 }}><Icon name="check" size={21} stroke={2.6} /> 확정</button>
          </div>
        </div>
      </div>
    );
  };
  return (
    <DesktopWindow url="app.jaram.kr/journal">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="journal" teacher={JD().teacher} cls={JD().cls} />
        <div style={{ flex: 1, overflow: 'hidden', position: 'relative' }}>{inner()}
          {state === 'overwrite' && <OverwriteModal onClose={() => {}} />}
        </div>
      </div>
    </DesktopWindow>
  );
}

Object.assign(window, { JournalMobile, JournalDesktop });
