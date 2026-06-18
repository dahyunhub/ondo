// screens-memo.jsx — 화면2 빠른 메모 입력 (모바일 + 데스크톱) + 입력칸 확대 모달
const D = () => window.JR_DATA;
const { useState: useMemoState } = React;

// 선택된 아이(민준이)의 입력 항목
const MEMO_FIELDS = [
{ key: '놀이', val: '블록놀이 중 친구와 자리를 두고 다퉜지만 스스로 “같이 쓰자”며 해결함. 블록을 높이 쌓는 구성놀이에 30분 넘게 몰입함.' },
{ key: '의사소통·상호작용', val: '“같이 쓰자”고 먼저 제안하고, 친구의 의견도 끝까지 들어준 뒤 자기 생각을 분명한 말로 전달함.' },
{ key: '수업태도', val: '' }];


// 아이 선택 칩
function PickChip({ name, on, lg }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, flex: '0 0 auto', width: lg ? 72 : 60 }}>
      <div style={{ position: 'relative', borderRadius: '50%', padding: 3, background: on ? 'var(--brand-500)' : 'transparent', boxShadow: on ? '0 4px 12px rgba(245,185,64,.4)' : 'none' }}>
        <Avatar name={name} size={lg ? 'lg' : ''} />
        {on && <span style={{ position: 'absolute', right: -2, bottom: -2, width: 20, height: 20, borderRadius: '50%', background: 'var(--success)', border: '2px solid var(--surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}><Icon name="check" size={11} stroke={3.4} /></span>}
      </div>
      <span style={{ fontSize: 12, fontWeight: on ? 800 : 600, color: on ? 'var(--text)' : 'var(--text-sub)', whiteSpace: 'nowrap' }}>{name}</span>
    </div>);

}

function StepLabel({ n, children, hint }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 9, marginBottom: 13 }}>
      <span style={{ width: 22, height: 22, borderRadius: '50%', background: 'var(--brand-500)', color: 'var(--text)', fontSize: 12, fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center', flex: '0 0 auto' }}>{n}</span>
      <span className="jr-h2" style={{ fontSize: 18, whiteSpace: 'nowrap' }}>{children}</span>
      {hint && <span style={{ fontSize: 12, color: 'var(--text-faint)', marginLeft: 'auto', whiteSpace: 'nowrap', flex: '0 0 auto' }}>{hint}</span>}
    </div>);

}

// 저장 가능 조건 안내 (비활성)
function SaveHint() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, fontSize: 13, color: 'var(--text-faint)', fontWeight: 600 }}>
      <Icon name="help" size={15} /> 아이를 고르고 내용을 한 줄만 적어주세요
    </div>);

}

// 입력 가이드 행 (클릭하면 확대 모달)
function GuideRows({ onOpen, fields = MEMO_FIELDS }) {
  return (
    <div className="jr-card" style={{ padding: '4px 18px' }}>
      {fields.map((f, i) =>
      <div className="jr-guide-row" key={f.key} onClick={() => onOpen(i)} style={{ cursor: 'pointer' }}>
          <span className="lab">{f.key}</span>
          <span className="inp" style={{ flex: 1, minWidth: 0, color: f.val ? 'var(--text)' : 'var(--text-faint)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', padding: '4px 0' }}>
            {f.val || '톡톡 적어요'}
          </span>
          <Icon name="expand" size={17} style={{ color: 'var(--text-faint)', flex: '0 0 auto', marginLeft: 8 }} />
        </div>
      )}
    </div>);

}
const EMPTY_FIELDS = MEMO_FIELDS.map((f) => ({ key: f.key, val: '' }));

// 입력칸 확대 모달 — 한 항목을 크게 입력 + 오늘 메모 한눈에
function FieldModal({ index, onClose, compact }) {
  const f = MEMO_FIELDS[index];
  return (
    <>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(40,30,20,.34)', zIndex: 20 }} />
      <div style={{ position: 'absolute', inset: 0, zIndex: 21, display: 'flex', alignItems: compact ? 'flex-end' : 'center', justifyContent: 'center', padding: compact ? 0 : 40 }}>
        <div style={{ background: 'var(--surface)', borderRadius: compact ? '26px 26px 0 0' : 24, padding: compact ? '22px 22px 28px' : '26px 28px', width: '100%', maxWidth: compact ? '100%' : 520, boxShadow: 'var(--shadow-lg)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
            <Avatar name="김민준" size="sm" />
            <div style={{ lineHeight: 1.2 }}>
              <div style={{ fontSize: 16, fontWeight: 800 }}>김민준 · {f.key}</div>
              <div style={{ fontSize: 12, color: 'var(--text-sub)', whiteSpace: 'nowrap' }}>크게 보고 편하게 적어요</div>
            </div>
            <button onClick={onClose} style={{ marginLeft: 'auto', border: 'none', background: 'var(--surface-soft)', borderRadius: '50%', width: 34, height: 34, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-sub)', cursor: 'pointer', flex: '0 0 auto' }}><Icon name="x" size={18} /></button>
          </div>
          <textarea className="jr-textarea" rows={compact ? 4 : 5} defaultValue={f.val} placeholder={`${f.key}에서 관찰한 모습을 적어요`} style={{ fontSize: 16, lineHeight: 1.6 }} autoFocus />

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, margin: '20px 0 10px' }}>
            <span style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', whiteSpace: 'nowrap' }}>오늘 메모 한눈에</span>
            <NuriChip area="social" />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {MEMO_FIELDS.map((m, i) =>
            <div key={m.key} style={{ display: 'flex', gap: 10, padding: '10px 12px', borderRadius: 12, background: i === index ? 'var(--brand-100)' : 'var(--surface-soft)', border: i === index ? '1.5px solid var(--brand-300)' : '1.5px solid transparent' }}>
                <span style={{ flex: '0 0 auto', width: 92, fontSize: 12.5, fontWeight: 700, color: 'var(--text-sub)' }}>{m.key}</span>
                <span style={{ flex: 1, fontSize: 13.5, lineHeight: 1.5, color: m.val ? 'var(--text)' : 'var(--text-faint)' }}>{m.val || '아직 비어 있어요'}</span>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
            <button className="jr-btn jr-btn--ghost" onClick={onClose} style={{ flex: '0 0 auto' }}>닫기</button>
            <button className="jr-btn jr-btn--primary" onClick={onClose} style={{ flex: 1 }}><Icon name="check" size={20} stroke={2.6} /> 완료</button>
          </div>
        </div>
      </div>
    </>);

}

function MemoMobile({ saved = false, initialField = null, empty = false }) {
  const [openField, setOpenField] = useMemoState(initialField);
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 16px 14px' }}>
          <button style={{ border: 'none', background: 'transparent', color: 'var(--text)', cursor: 'pointer', padding: 4 }}><Icon name="back" size={24} /></button>
          <span className="jr-h1" style={{ fontSize: 21 }}>빠른 메모</span>
          <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--text-faint)', display: 'flex', alignItems: 'center', gap: 4, whiteSpace: 'nowrap' }}><Icon name="clock" size={15} /> 10초면 충분해요</span>
        </div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 20px' }}>
          <StepLabel n={1} children="아이 선택" hint="옆으로 밀어 더 보기" />
          <div style={{ position: 'relative', marginBottom: 26 }}>
            <div style={{ display: 'flex', gap: 14, overflow: 'hidden', paddingBottom: 2 }}>
              {D().children.map((c, i) => <PickChip key={c.name} name={c.name} on={!empty && i === 1} />)}
            </div>
            <div style={{ position: 'absolute', top: 0, bottom: 0, right: -20, width: 44, background: 'linear-gradient(90deg, transparent, var(--bg))', pointerEvents: 'none' }} />
          </div>

          <StepLabel n={2} children="내용 입력" hint="모두 선택 · 칸을 누르면 크게" />
          <GuideRows onOpen={setOpenField} fields={empty ? EMPTY_FIELDS : MEMO_FIELDS} />
        </div>
        <div style={{ padding: '14px 20px 26px', background: 'linear-gradient(transparent, var(--bg) 28%)' }}>
          {empty && <div style={{ marginBottom: 10 }}><SaveHint /></div>}
          <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" disabled={empty}><Icon name="check" size={22} stroke={2.6} /> {empty ? '메모 저장' : '김민준 메모 저장'}</button>
        </div>
      </div>
      {openField !== null && <FieldModal index={openField} onClose={() => setOpenField(null)} compact />}
      {saved &&
      <>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(40,30,20,.18)', zIndex: 10 }} />
          <div style={{ position: 'absolute', left: 20, right: 20, bottom: 120, zIndex: 11, display: 'flex', justifyContent: 'center' }}>
            <div className="jr-toast" style={{ width: '100%', justifyContent: 'center' }}><span className="tick"><Icon name="check" size={14} stroke={3} /></span>김민준 페이지에 저장됐어요</div>
          </div>
        </>
      }
    </PhoneFrame>);

}

function MemoDesktop({ initialField = null, empty = false }) {
  const [openField, setOpenField] = useMemoState(initialField);
  return (
    <DesktopWindow url="app.jaram.kr/memo">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="home" teacher={D().teacher} cls={D().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 56px', position: 'relative' }}>
          <div style={{ maxWidth: 760, margin: '0 auto' }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 4 }}>
              <span className="jr-display">빠른 메모</span>
              <span style={{ fontSize: 14, color: 'var(--text-sub)' }}>아이 선택 → 내용 입력 → 저장, 세 동작이면 끝</span>
            </div>
            <div style={{ fontSize: 14, color: 'var(--text-faint)', marginBottom: 28 }}>{D().date} · {D().cls}</div>

            <StepLabel n={1} children="아이 선택" hint="반 전체가 한눈에" />
            <div style={{ display: 'flex', gap: 18, flexWrap: 'wrap', marginBottom: 30 }}>
              {D().children.map((c, i) => <PickChip key={c.name} name={c.name} on={!empty && i === 1} lg />)}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 22, alignItems: 'start' }}>
              <div style={{ minWidth: 0 }}>
                <StepLabel n={2} children="내용 입력" hint="모두 선택 · 칸을 누르면 크게" />
                <GuideRows onOpen={setOpenField} fields={empty ? EMPTY_FIELDS : MEMO_FIELDS} />
              </div>
              <div>
                <div style={{ height: 31 }} />
                <div className="jr-card jr-card--soft" style={{ padding: 18, textAlign: 'center' }}>
                  <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" disabled={empty}><Icon name="check" size={22} stroke={2.6} /> 저장하기</button>
                  {empty ?
                  <div style={{ marginTop: 12 }}><SaveHint /></div> :
                  <p style={{ fontSize: 13, color: 'var(--text-faint)', textAlign: 'center', marginTop: 12, lineHeight: 1.5 }}>저장하면 <b style={{ color: 'var(--text-sub)' }}>김민준 페이지</b>에<br />바로 기록돼요 · 영역은 밤에 자동 분류</p>}
                </div>
              </div>
            </div>
          </div>
          {openField !== null && <FieldModal index={openField} onClose={() => setOpenField(null)} />}
        </div>
      </div>
    </DesktopWindow>);

}

Object.assign(window, { MemoMobile, MemoDesktop });