// screens-account.jsx — 일지·분석 허브 / 마이 (반 전환 포함)
const AC = () => window.JR_DATA;
const { useState: useAcState } = React;

// ===== 일지·분석 =====

// A / B 분석 진입 카드 (큰 메뉴)
function AnalysisBig({ id, title, sub, desc, icon, accent }) {
  return (
    <div className="jr-card" style={{ padding: 20, cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: 12, border: '1.5px solid var(--hair)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <span style={{ width: 48, height: 48, borderRadius: 14, flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', background: accent }}><Icon name={icon} size={24} style={{ color: 'var(--text)' }} /></span>
        <div style={{ lineHeight: 1.25 }}>
          <div style={{ fontSize: 16.5, fontWeight: 800, whiteSpace: 'nowrap' }}><span style={{ color: 'var(--text-faint)', marginRight: 5 }}>{id}</span>{title}</div>
          <div style={{ fontSize: 12.5, color: 'var(--text-sub)', fontWeight: 600, whiteSpace: 'nowrap' }}>{sub}</div>
        </div>
      </div>
      <div style={{ fontSize: 13.5, color: 'var(--text-sub)', lineHeight: 1.55 }}>{desc}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 13.5, fontWeight: 800, color: 'var(--brand-700)', whiteSpace: 'nowrap' }}>분석 시작 <Icon name="chevR" size={16} stroke={2.6} /></div>
    </div>);

}

// 영역 색 점 묶음
function AreaDots({ areas }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
      <div style={{ display: 'flex', gap: 3 }}>
        {areas.map((a) => <span key={a} style={{ width: 9, height: 9, borderRadius: '50%', background: AREAS[a].color }} />)}
      </div>
      <span style={{ fontSize: 12, color: 'var(--text-faint)', fontWeight: 600, whiteSpace: 'nowrap' }}>{areas.length}개 영역</span>
    </div>);

}

function JournalRow({ j }) {
  return (
    <div className="jr-card" style={{ padding: '14px 16px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 12 }}>
      {j.child ?
      <Avatar name={j.child} size="sm" /> :
      <span style={{ width: 36, height: 36, borderRadius: 11, flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--brand-100)', color: 'var(--brand-700)' }}><Icon name="journal" size={19} /></span>}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 14.5, fontWeight: 700, whiteSpace: 'nowrap' }}>{j.type}</span>
          <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-faint)', whiteSpace: 'nowrap' }}>{j.time}</span>
        </div>
        <div style={{ marginTop: 5 }}><AreaDots areas={j.areas} /></div>
      </div>
      <Icon name="chevR" size={18} style={{ color: 'var(--text-faint)', flex: '0 0 auto' }} />
    </div>);

}

function TodayCta({ wide }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: wide ? '20px 24px' : '16px 18px', borderRadius: 20, background: 'linear-gradient(135deg, #FFD45E, #F5B940)', boxShadow: '0 8px 22px rgba(245,185,64,.4)', cursor: 'pointer' }}>
      <span style={{ width: 46, height: 46, borderRadius: 14, flex: '0 0 auto', background: 'rgba(255,255,255,.45)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text)' }}><Icon name="sparkle" size={26} /></span>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--text)', whiteSpace: 'nowrap' }}>오늘 메모 4개로 일지 쓰기</div>
        <div style={{ fontSize: 13, color: '#7a5e22', fontWeight: 600, marginTop: 2 }}>하루 일지를 바로 만들어요</div>
      </div>
      <Icon name="chevR" size={22} style={{ color: 'var(--text)' }} />
    </div>);

}

function JournalHubMobile() {
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <MobileHeader cls={AC().cls} date={AC().date} right={<Avatar name={AC().teacher} size="sm" />} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 20px' }}>
          <div className="jr-h1" style={{ marginBottom: 14 }}>일지·분석</div>
          <div style={{ marginBottom: 18 }}><TodayCta /></div>
          <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', marginBottom: 10 }}>어떤 걸 분석할까요?</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 22 }}>
            <AnalysisBig id="A" title="한 아이 분석" sub="개인 관찰 평가" icon="me" accent="rgba(201,168,232,.3)" desc="한 아이의 기록을 모아 상담·발달평가용 관찰 평가를 만들어요." />
            <AnalysisBig id="B" title="오늘 전체 분석" sub="하루 일지" icon="journal" accent="var(--brand-300)" desc="오늘 반 전체 메모로 매일 제출하는 일지를 만들어요." />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 12 }}>
            <span style={{ fontSize: 14, fontWeight: 800, whiteSpace: 'nowrap' }}>최근 만든 일지</span>
            <span className="jr-badge-count">{AC().journalHistory.length}</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {AC().journalHistory.slice(0, 3).map((j, i) => <JournalRow key={i} j={j} />)}
          </div>
        </div>
        <BottomTab active="journal" />
      </div>
    </PhoneFrame>);

}

function JournalHubDesktop() {
  return (
    <DesktopWindow url="app.jaram.kr/journal">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="journal" teacher={AC().teacher} cls={AC().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 48px' }}>
          <div className="jr-display" style={{ marginBottom: 6 }}>일지·분석</div>
          <div style={{ fontSize: 15, color: 'var(--text-sub)', marginBottom: 26 }}>{AC().date} · {AC().cls} · 오늘 메모 4개</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1.1fr', gap: 16, marginBottom: 30 }}>
            <AnalysisBig id="A" title="한 아이 분석" sub="개인 관찰 평가" icon="me" accent="rgba(201,168,232,.3)" desc="한 아이의 기록을 모아 상담·발달평가용 관찰 평가를 만들어요." />
            <AnalysisBig id="B" title="오늘 전체 분석" sub="하루 일지" icon="journal" accent="var(--brand-300)" desc="오늘 반 전체 메모로 매일 제출하는 일지를 만들어요." />
            <div style={{ display: 'flex', alignItems: 'stretch' }}><TodayCta wide /></div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
            <span className="jr-h2">최근 만든 일지</span>
            <span className="jr-badge-count">{AC().journalHistory.length}</span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            {AC().journalHistory.map((j, i) => <JournalRow key={i} j={j} />)}
          </div>
        </div>
      </div>
    </DesktopWindow>);

}

// ===== 마이 (반 전환 포함) =====

// 반 전환 카드
function SwitchCard({ c, on, onClick }) {
  const isNow = c.tag === '올해';
  return (
    <div onClick={onClick} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px', borderRadius: 16, cursor: 'pointer',
      background: on ? 'var(--brand-100)' : 'var(--surface)', border: '2px solid ' + (on ? 'var(--brand-500)' : 'var(--hair-strong)') }}>
      <span style={{ width: 44, height: 44, borderRadius: 13, flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', background: on ? 'var(--brand-300)' : 'var(--surface-soft)' }}><Icon name={isNow ? 'sun' : 'leaf'} size={22} style={{ color: on ? '#9A6B12' : 'var(--text-sub)' }} /></span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, whiteSpace: 'nowrap' }}>
          <span style={{ fontSize: 16, fontWeight: 800 }}>{c.name}</span>
          <span className="jr-chip" style={{ background: isNow ? 'rgba(127,209,174,.22)' : 'var(--surface-soft)', color: isNow ? '#3C8F62' : 'var(--text-faint)', fontSize: 10.5 }}>{c.tag}</span>
        </div>
        <div style={{ fontSize: 12.5, color: 'var(--text-sub)', fontWeight: 600, marginTop: 2, whiteSpace: 'nowrap' }}>{c.year} · 원아 {c.count}명</div>
      </div>
      {on ?
      <Icon name="check" size={20} stroke={2.6} style={{ color: 'var(--brand-700)', flex: '0 0 auto' }} /> :
      <span style={{ fontSize: 12.5, fontWeight: 800, color: 'var(--brand-700)', whiteSpace: 'nowrap' }}>보기</span>}
    </div>);

}

// 반 전환하기 모달 — 지금까지 맡았던 반 중에서 선택
function ClassSwitchModal({ idx, onSelect, onClose, compact }) {
  return (
    <>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(40,30,20,.34)', zIndex: 20 }} />
      <div style={{ position: 'absolute', inset: 0, zIndex: 21, display: 'flex', alignItems: compact ? 'flex-end' : 'center', justifyContent: 'center', padding: compact ? 0 : 40 }}>
        <div style={{ background: 'var(--surface)', borderRadius: compact ? '26px 26px 0 0' : 24, padding: compact ? '22px 22px 28px' : '26px 28px', width: '100%', maxWidth: compact ? '100%' : 440, boxShadow: 'var(--shadow-lg)' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10, marginBottom: 6 }}>
            <div style={{ lineHeight: 1.3 }}>
              <div style={{ fontSize: 18, fontWeight: 800 }}>반 전환하기</div>
              <div style={{ fontSize: 13, color: 'var(--text-sub)', marginTop: 2, whiteSpace: 'nowrap' }}>지금까지 맡았던 반 중에서 선택하세요</div>
            </div>
            <button onClick={onClose} style={{ marginLeft: 'auto', border: 'none', background: 'var(--surface-soft)', borderRadius: '50%', width: 34, height: 34, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-sub)', cursor: 'pointer', flex: '0 0 auto' }}><Icon name="x" size={18} /></button>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 16 }}>
            {AC().classes.map((c, i) => <SwitchCard key={c.name + c.year} c={c} on={i === idx} onClick={() => onSelect(i)} />)}
          </div>
        </div>
      </div>
    </>);

}

function SettingRow({ icon, label, sub, danger, onClick }) {
  return (
    <div onClick={onClick} style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '14px 4px', cursor: 'pointer', borderBottom: '1px solid var(--hair)' }}>
      <Icon name={icon} size={21} style={{ color: danger ? 'var(--warn)' : 'var(--text-sub)', flex: '0 0 auto' }} />
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: danger ? 'var(--warn)' : 'var(--text)', whiteSpace: 'nowrap' }}>{label}</div>
        {sub && <div style={{ fontSize: 12.5, color: 'var(--text-sub)', fontWeight: 600, whiteSpace: 'nowrap', marginTop: 2 }}>{sub}</div>}
      </div>
      {!danger && <Icon name="chevR" size={18} style={{ marginLeft: 'auto', color: 'var(--text-faint)', flex: '0 0 auto' }} />}
    </div>);

}

// 작년 반 보는 중 안내 배너
function ViewingBanner({ sel }) {
  return (
    <div className="jr-banner" style={{ marginBottom: 18 }}>
      <Icon name="clock" size={20} style={{ color: 'var(--brand-700)', flex: '0 0 auto' }} />
      <span style={{ fontSize: 13, fontWeight: 700 }}>{sel.name} {sel.year} 기록을 보는 중이에요. 지난 반 아이들 기록을 확인할 수 있어요.</span>
    </div>);

}

function ProfileBlock({ big }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
      <Avatar name={AC().teacher} size="lg" />
      <div style={{ minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, whiteSpace: 'nowrap' }}>
          <span className="jr-h2" style={{ fontSize: big ? 22 : 19 }}>{AC().teacher} 선생님</span>
        </div>
        <div style={{ fontSize: 13.5, color: 'var(--text-sub)', whiteSpace: 'nowrap', marginTop: 2 }}>{AC().teacherEmail}</div>
      </div>
      <button className="jr-btn jr-btn--secondary jr-btn--sm" style={{ marginLeft: 'auto', flex: '0 0 auto' }}>프로필 수정</button>
    </div>);

}

function MyPageMobile({ initialIdx = 0, initialOpen = false }) {
  const [idx, setIdx] = useAcState(initialIdx);
  const [open, setOpen] = useAcState(initialOpen);
  const sel = AC().classes[idx];
  const isPast = sel.tag !== '올해';
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '6px 20px 12px' }}><span className="jr-h1">마이</span></div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 20px' }}>
          <div className="jr-card" style={{ marginBottom: 20 }}><ProfileBlock /></div>
          {isPast && <ViewingBanner sel={sel} />}
          <div className="jr-card" style={{ padding: '4px 16px' }}>
            <SettingRow icon="swap" label="반 전환하기" sub={`현재 ${sel.name} · ${sel.year}`} onClick={() => setOpen(true)} />
            <SettingRow icon="bell" label="알림 설정" />
            <SettingRow icon="download" label="기록 내보내기" />
            <SettingRow icon="help" label="도움말" />
            <SettingRow icon="logout" label="로그아웃" danger />
          </div>
        </div>
        <BottomTab active="me" />
      </div>
      {open && <ClassSwitchModal idx={idx} onSelect={(i) => {setIdx(i);setOpen(false);}} onClose={() => setOpen(false)} compact />}
    </PhoneFrame>);

}

function MyPageDesktop({ initialIdx = 0, initialOpen = false }) {
  const [idx, setIdx] = useAcState(initialIdx);
  const [open, setOpen] = useAcState(initialOpen);
  const sel = AC().classes[idx];
  const isPast = sel.tag !== '올해';
  return (
    <DesktopWindow url="app.jaram.kr/me">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="me" teacher={AC().teacher} cls={AC().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 48px', position: 'relative' }}>
          <div className="jr-display" style={{ marginBottom: 24 }}>마이</div>
          <div style={{ maxWidth: 560, display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div className="jr-card"><ProfileBlock big /></div>
            {isPast && <ViewingBanner sel={sel} />}
            <div>
              <div style={{ fontSize: 14, fontWeight: 800, color: 'var(--text-sub)', marginBottom: 12 }}>설정</div>
              <div className="jr-card" style={{ padding: '4px 18px' }}>
                <SettingRow icon="swap" label="반 전환하기" sub={`현재 ${sel.name} · ${sel.year}`} onClick={() => setOpen(true)} />
                <SettingRow icon="bell" label="알림 설정" />
                <SettingRow icon="download" label="기록 내보내기" />
                <SettingRow icon="help" label="도움말" />
                <SettingRow icon="logout" label="로그아웃" danger />
              </div>
            </div>
          </div>
          {open && <ClassSwitchModal idx={idx} onSelect={(i) => {setIdx(i);setOpen(false);}} onClose={() => setOpen(false)} />}
        </div>
      </div>
    </DesktopWindow>);

}

Object.assign(window, { JournalHubMobile, JournalHubDesktop, MyPageMobile, MyPageDesktop });