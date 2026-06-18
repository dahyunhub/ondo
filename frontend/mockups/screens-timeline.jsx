// screens-timeline.jsx — 화면4 아이별 타임라인 (+ 개인평가 이력)
const T = () => window.JR_DATA;
const { useState: useTlState } = React;

// ---------- 영역 필터 칩 (미분류 포함) ----------
function FilterChips({ compact, active = '전체' }) {
  const order = ['body', 'comm', 'social', 'art', 'nature', 'uncat'];
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: compact ? 'nowrap' : 'wrap', overflow: 'hidden' }}>
      <span className={'jr-toggle' + (active === '전체' ? ' is-on' : '')} style={{ padding: compact ? '7px 13px' : '8px 15px' }}>전체</span>
      {order.map(a => (
        <span key={a} className={'jr-toggle' + (active === a ? ' is-on' : '')} style={{ padding: compact ? '7px 12px' : '8px 14px', gap: 7 }}>
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: AREAS[a].color, border: a === 'uncat' ? '1px dashed #9b9384' : 'none', flex: '0 0 auto' }} /> {AREAS[a].ko}
        </span>
      ))}
    </div>
  );
}

// ---------- 영역 인라인 수정 드롭다운 ----------
function AreaEditMenu() {
  const order = ['body', 'comm', 'social', 'art', 'nature'];
  return (
    <div style={{ position: 'absolute', top: '100%', right: 0, marginTop: 6, zIndex: 12, background: 'var(--surface)', borderRadius: 14, boxShadow: 'var(--shadow-lg)', border: '1px solid var(--hair)', padding: 8, width: 200 }}>
      <div style={{ fontSize: 11.5, fontWeight: 800, color: 'var(--text-faint)', padding: '4px 8px 8px' }}>누리과정 영역 지정</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {order.map(a => (
          <div key={a} style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '8px 8px', borderRadius: 9, cursor: 'pointer', background: a === 'comm' ? 'var(--brand-100)' : 'transparent' }}>
            <span style={{ width: 9, height: 9, borderRadius: '50%', background: AREAS[a].color, flex: '0 0 auto' }} />
            <span style={{ fontSize: 13.5, fontWeight: a === 'comm' ? 800 : 600, color: 'var(--text)' }}>{AREAS[a].ko}</span>
            {a === 'comm' && <Icon name="check" size={15} stroke={3} style={{ marginLeft: 'auto', color: 'var(--brand-700)' }} />}
          </div>
        ))}
      </div>
    </div>
  );
}

// ---------- 타임라인 ----------
function TimelineList({ compact, editFlatIndex = -1 }) {
  const groups = [];
  T().timeline.forEach(e => {
    const g = groups.find(x => x.date === e.date);
    if (g) g.items.push(e); else groups.push({ date: e.date, items: [e] });
  });
  let flat = -1;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      {groups.map(g => (
        <div key={g.date}>
          <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Icon name="cal" size={15} /> {g.date}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {g.items.map((e, i) => {
              flat += 1;
              const isUncat = e.area === 'uncat';
              const editing = flat === editFlatIndex;
              return (
                <div key={i} className="jr-area-block" style={{ '--strip': AREAS[e.area].color, position: editing ? 'relative' : 'relative', overflow: editing ? 'visible' : 'hidden', zIndex: editing ? 5 : 'auto' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                    <NuriChip area={e.area} />
                    {/* 영역 수정 진입점 */}
                    <button style={{ display: 'flex', alignItems: 'center', gap: 3, border: 'none', background: isUncat ? 'var(--brand-100)' : 'transparent', borderRadius: 999, padding: isUncat ? '4px 9px' : '4px 6px', cursor: 'pointer', fontFamily: 'inherit', fontSize: 11.5, fontWeight: 700, color: isUncat ? 'var(--brand-700)' : 'var(--text-faint)' }}>
                      {isUncat ? '영역 지정' : <Icon name="pencil" size={13} />}{isUncat && <Icon name="chevD" size={13} stroke={2.4} />}
                    </button>
                    <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-faint)', fontVariantNumeric: 'tabular-nums' }}>{e.time}</span>
                  </div>
                  <div style={{ fontSize: 14.5, lineHeight: 1.55 }}>{e.txt}</div>
                  {editing && <AreaEditMenu />}
                </div>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}

// ---------- 개인평가 이력 (시간순 누적 · 수동/자동 배지) ----------
function EvalKindBadge({ kind }) {
  const auto = kind === '자동';
  return (
    <span className="jr-chip" style={{ background: auto ? 'rgba(127,209,174,.2)' : 'var(--brand-300)', color: auto ? '#3C8F62' : '#8a6a1f', fontSize: 10.5, padding: '4px 9px' }}>
      {auto ? '월말 자동' : '수동 생성'}
    </span>
  );
}

function EvalHistory({ compact, onGenerate }) {
  return (
    <div className="jr-card" style={{ padding: compact ? 16 : 18 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
        <span style={{ width: 28, height: 28, borderRadius: 9, background: 'var(--brand-300)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text)', flex: '0 0 auto' }}><Icon name="me" size={17} /></span>
        <span style={{ fontSize: 14.5, fontWeight: 800, whiteSpace: 'nowrap' }}>개인 관찰평가</span>
        <span style={{ fontSize: 11.5, color: 'var(--text-faint)', fontWeight: 600 }}>시간순 누적</span>
        <button onClick={onGenerate} className="jr-btn jr-btn--primary jr-btn--sm" style={{ marginLeft: 'auto', flex: '0 0 auto' }}><Icon name="plus" size={17} stroke={2.6} /> 생성</button>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {T().childEvals.map((ev, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 11, padding: '11px 12px', borderRadius: 13, background: 'var(--surface-soft)', cursor: 'pointer' }}>
            <span style={{ width: 34, height: 34, borderRadius: 10, flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--surface)', color: 'var(--text-sub)' }}><Icon name="journal" size={18} /></span>
            <div style={{ minWidth: 0, flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 7, whiteSpace: 'nowrap' }}>
                <span style={{ fontSize: 14, fontWeight: 800 }}>{ev.title}</span>
                <EvalKindBadge kind={ev.kind} />
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-sub)', marginTop: 3, whiteSpace: 'nowrap' }}>{ev.period} · {ev.time}</div>
            </div>
            <Icon name="chevR" size={18} style={{ color: 'var(--text-faint)', flex: '0 0 auto' }} />
          </div>
        ))}
      </div>
    </div>
  );
}

// ---------- 개인평가 생성 모달 (분석 기간 표시) ----------
function EvalGenModal({ onClose, compact }) {
  return (
    <>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(40,30,20,.34)', zIndex: 20 }} />
      <div style={{ position: 'absolute', inset: 0, zIndex: 21, display: 'flex', alignItems: compact ? 'flex-end' : 'center', justifyContent: 'center', padding: compact ? 0 : 40 }}>
        <div style={{ background: 'var(--surface)', borderRadius: compact ? '26px 26px 0 0' : 24, padding: compact ? '24px 22px 28px' : '28px 30px', width: '100%', maxWidth: compact ? '100%' : 440, boxShadow: 'var(--shadow-lg)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 11, marginBottom: 8 }}>
            <Avatar name="김민준" size="sm" />
            <div style={{ fontSize: 17, fontWeight: 800 }}>김민준 개인평가 만들기</div>
            <button onClick={onClose} style={{ marginLeft: 'auto', border: 'none', background: 'var(--surface-soft)', borderRadius: '50%', width: 34, height: 34, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-sub)', cursor: 'pointer', flex: '0 0 auto' }}><Icon name="x" size={18} /></button>
          </div>
          <div style={{ fontSize: 13.5, color: 'var(--text-sub)', lineHeight: 1.5, marginBottom: 18 }}>직전 평가 이후부터 오늘까지의 관찰 기록을 모아 분석해요.</div>
          <div className="jr-banner" style={{ marginBottom: 18 }}>
            <Icon name="cal" size={20} style={{ color: 'var(--brand-700)', flex: '0 0 auto' }} />
            <div>
              <div style={{ fontSize: 12, color: 'var(--text-sub)', fontWeight: 600 }}>분석 대상 기간</div>
              <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--text)' }}>6월 1일 ~ 오늘 (6월 15일)</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: 12.5, color: 'var(--text-faint)', fontWeight: 600, marginBottom: 22 }}>
            <Icon name="check" size={15} stroke={2.6} style={{ color: 'var(--success)' }} /> 새 평가는 기존 평가를 덮어쓰지 않고 따로 쌓여요.
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button className="jr-btn jr-btn--ghost" onClick={onClose} style={{ flex: '0 0 auto' }}>취소</button>
            <button className="jr-btn jr-btn--primary" onClick={onClose} style={{ flex: 1 }}><Icon name="sparkle" size={20} /> 평가 생성</button>
          </div>
        </div>
      </div>
    </>
  );
}

function ChildHead({ compact }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
      <Avatar name={T().childAnalysis.name} size="lg" />
      <div>
        <div className="jr-h1" style={{ fontSize: compact ? 22 : 26, whiteSpace: 'nowrap' }}>{T().childAnalysis.name}</div>
        <div style={{ fontSize: 13.5, color: 'var(--text-sub)', display: 'flex', alignItems: 'center', gap: 6, marginTop: 2, whiteSpace: 'nowrap' }}>
          {T().cls} · 관찰기록 <b style={{ color: 'var(--text)' }}>{T().timeline.length}</b>개
        </div>
      </div>
    </div>
  );
}

// ---------- 빈 상태 ----------
function EmptyTimeline({ compact }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: '40px 24px', minHeight: compact ? 320 : 380 }}>
      <SproutLoader size={92} />
      <div className="jr-h2" style={{ marginTop: 18 }}>아직 기록이 없어요</div>
      <div className="jr-body" style={{ color: 'var(--text-sub)', marginTop: 8, lineHeight: 1.5 }}>서윤이의 오늘 첫 메모를<br/>남겨볼까요?</div>
      <button className="jr-btn jr-btn--primary" style={{ marginTop: 22 }}><Icon name="plus" size={20} stroke={2.6} /> 첫 메모 남기기</button>
    </div>
  );
}

function TimelineMobile({ empty = false, editFlatIndex = -1, evalModal = false }) {
  const [showEval, setShowEval] = useTlState(evalModal);
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 16px 12px' }}>
          <button style={{ border: 'none', background: 'transparent', color: 'var(--text)', cursor: 'pointer', padding: 4 }}><Icon name="back" size={24} /></button>
          <ChildHead compact />
          <button style={{ marginLeft: 'auto', border: 'none', background: 'transparent', color: 'var(--text-sub)', cursor: 'pointer', padding: 4 }}><Icon name="dots" size={22} /></button>
        </div>
        {empty ? <EmptyTimeline compact /> : (
          <div style={{ flex: 1, overflow: 'hidden', padding: '0 18px' }}>
            <div style={{ marginBottom: 16 }}><EvalHistory compact onGenerate={() => setShowEval(true)} /></div>
            <div style={{ position: 'sticky', top: 0, background: 'var(--bg)', paddingBottom: 12, marginBottom: 4, zIndex: 2 }}>
              <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', marginBottom: 10 }}>관찰 기록 타임라인</div>
              <FilterChips compact />
            </div>
            <TimelineList compact editFlatIndex={editFlatIndex} />
          </div>
        )}
        <BottomTab active="children" />
      </div>
      {showEval && <EvalGenModal onClose={() => setShowEval(false)} compact />}
    </PhoneFrame>
  );
}

function TimelineDesktop({ empty = false, editFlatIndex = -1, evalModal = false }) {
  const [showEval, setShowEval] = useTlState(evalModal);
  return (
    <DesktopWindow url="app.jaram.kr/child/minjun">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="children" teacher={T().teacher} cls={T().cls} />
        <div style={{ flex: 1, display: 'flex', overflow: 'hidden', position: 'relative' }}>
          {/* 좌: 프로필 + 개인평가 */}
          <div style={{ width: 400, flex: '0 0 auto', padding: '34px 28px', borderRight: '1px solid var(--hair)', overflow: 'hidden' }}>
            <button style={{ display: 'flex', alignItems: 'center', gap: 6, border: 'none', background: 'transparent', color: 'var(--text-sub)', cursor: 'pointer', fontFamily: 'inherit', fontSize: 14, fontWeight: 600, marginBottom: 22, padding: 0, whiteSpace: 'nowrap' }}><Icon name="back" size={18} /> 아이 목록</button>
            <ChildHead />
            <div style={{ marginTop: 22 }}><EvalHistory onGenerate={() => setShowEval(true)} /></div>
          </div>
          {/* 우: 타임라인 */}
          <div style={{ flex: 1, overflow: 'hidden', padding: '34px 40px' }}>
            {empty ? <EmptyTimeline /> : (
              <>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 18 }}>
                  <span className="jr-h2">관찰 기록 타임라인</span>
                  <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--text-faint)' }}>최신순 · 영역 칩을 눌러 수정</span>
                </div>
                <div style={{ marginBottom: 22 }}><FilterChips /></div>
                <div style={{ maxWidth: 560 }}><TimelineList editFlatIndex={editFlatIndex} /></div>
              </>
            )}
          </div>
          {showEval && <EvalGenModal onClose={() => setShowEval(false)} />}
        </div>
      </div>
    </DesktopWindow>
  );
}

Object.assign(window, { TimelineMobile, TimelineDesktop });
