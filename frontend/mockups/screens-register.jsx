// screens-register.jsx — 아이 등록 (처음 시작 온보딩 + 학기 중 단일 추가)
const R = () => window.JR_DATA;

// ---------- 사진 업로드 슬롯 (원형) ----------
function PhotoSlot({ size = 88 }) {
  return (
    <div style={{ width: size, height: size, borderRadius: '50%', flex: '0 0 auto', position: 'relative',
      background: 'var(--surface-soft)', border: '2px dashed var(--brand-300)', display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', gap: 3, cursor: 'pointer' }}>
      <Icon name="camera" size={size > 96 ? 30 : 26} style={{ color: 'var(--brand-700)' }} />
      <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-sub)', whiteSpace: 'nowrap' }}>사진</span>
      <span style={{ position: 'absolute', right: -2, bottom: -2, width: 26, height: 26, borderRadius: '50%', background: 'var(--brand-500)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text)', border: '2.5px solid var(--bg)' }}><Icon name="plus" size={14} stroke={3} /></span>
    </div>
  );
}

// ---------- 성별 토글 ----------
function SexToggle({ value = '남' }) {
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      {['남', '여'].map(s => (
        <span key={s} className={'jr-toggle' + (s === value ? ' is-on' : '')} style={{ flex: 1, justifyContent: 'center' }}>
          {s === value && <Icon name="check" size={14} stroke={2.6} />} {s === '남' ? '남자' : '여자'}
        </span>
      ))}
    </div>
  );
}

// ---------- 날짜 입력 (생년월일) ----------
function DateField({ value }) {
  return (
    <div style={{ position: 'relative' }}>
      <input className="jr-input" defaultValue={value} placeholder="2021.03.14" style={{ paddingRight: 44 }} />
      <Icon name="cal" size={20} style={{ position: 'absolute', right: 15, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-faint)', pointerEvents: 'none' }} />
    </div>
  );
}

// ---------- 등록 폼 본체 ----------
function RegisterFields({ name = '', birth = '', sex = '남', centerPhoto }) {
  return (
    <>
      <div style={{ display: 'flex', justifyContent: centerPhoto ? 'center' : 'flex-start', marginBottom: 18 }}>
        <PhotoSlot />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div>
          <label className="jr-field-label">이름</label>
          <input className="jr-input" defaultValue={name} placeholder="아이 이름을 입력해주세요" />
        </div>
        <div>
          <label className="jr-field-label">생년월일</label>
          <DateField value={birth} />
        </div>
        <div>
          <label className="jr-field-label">성별</label>
          <SexToggle value={sex} />
        </div>
      </div>
    </>
  );
}

// ---------- 추가한 아이 행 ----------
function AddedRow({ name, birth }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 11, padding: '10px 12px', borderRadius: 14, background: 'var(--surface-soft)' }}>
      <Avatar name={name} size="sm" />
      <div style={{ lineHeight: 1.3, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 700, whiteSpace: 'nowrap' }}>{name}</div>
        <div style={{ fontSize: 12, color: 'var(--text-sub)', whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' }}>{birth}</div>
      </div>
      <button style={{ marginLeft: 'auto', border: 'none', background: 'transparent', color: 'var(--text-faint)', cursor: 'pointer', padding: 4, flex: '0 0 auto' }}><Icon name="x" size={18} /></button>
    </div>
  );
}

// ============ 온보딩 (처음 시작 · 여러 명 등록) ============
function OnboardMobile() {
  const added = R().children.slice(0, 4);
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '6px 22px 12px' }}>
          <LogoLockup height={30} />
          <div className="jr-h1" style={{ fontSize: 22, marginTop: 14 }}>우리 반 아이들을<br/>등록해 주세요</div>
          <div style={{ fontSize: 13.5, color: 'var(--text-sub)', marginTop: 6, lineHeight: 1.5 }}>햇살반 친구들을 한 명씩 추가해요. 나중에 언제든 더할 수 있어요.</div>
        </div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 22px' }}>
          <div className="jr-card" style={{ padding: 18 }}>
            <RegisterFields name="" birth="" sex="남" centerPhoto />
            <button className="jr-btn jr-btn--secondary jr-btn--block" style={{ marginTop: 16 }}><Icon name="plus" size={20} stroke={2.4} /> 이 아이 추가</button>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7, margin: '20px 0 12px', whiteSpace: 'nowrap' }}>
            <span style={{ fontSize: 14, fontWeight: 800 }}>추가한 아이</span>
            <span className="jr-badge-count">{added.length}</span>
          </div>
          <div style={{ display: 'flex', gap: 14, overflow: 'hidden' }}>
            {added.map(c => (
              <div key={c.name} style={{ flex: '0 0 auto', width: 58, textAlign: 'center' }}>
                <Avatar name={c.name} size="lg" />
                <div style={{ fontSize: 12, fontWeight: 700, marginTop: 5, whiteSpace: 'nowrap' }}>{c.name}</div>
              </div>
            ))}
          </div>
        </div>
        <div style={{ padding: '12px 22px 26px', background: 'linear-gradient(transparent, var(--bg) 30%)' }}>
          <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg">{added.length}명 등록 완료 · 시작하기 <Icon name="chevR" size={20} /></button>
        </div>
      </div>
    </PhoneFrame>
  );
}

function OnboardDesktop() {
  const added = R().children.slice(0, 6);
  return (
    <DesktopWindow url="app.jaram.kr/onboarding" height={660}>
      <div style={{ display: 'flex', height: '100%' }}>
        {/* 좌: 입력 폼 */}
        <div style={{ flex: '0 0 auto', width: 460, padding: '36px 44px', borderRight: '1px solid var(--hair)', overflow: 'hidden' }}>
          <LogoLockup height={34} />
          <div className="jr-h1" style={{ marginTop: 16 }}>우리 반 아이들을 등록해요</div>
          <div style={{ fontSize: 14, color: 'var(--text-sub)', marginTop: 6, marginBottom: 26, lineHeight: 1.5 }}>햇살반 친구들을 한 명씩 추가해 주세요.<br/>나중에 언제든 더하거나 수정할 수 있어요.</div>
          <RegisterFields name="" birth="" sex="남" />
          <button className="jr-btn jr-btn--secondary jr-btn--block" style={{ marginTop: 20 }}><Icon name="plus" size={20} stroke={2.4} /> 이 아이 추가</button>
        </div>
        {/* 우: 추가한 아이 목록 */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', padding: '36px 40px', overflow: 'hidden', background: 'var(--surface-soft)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16, whiteSpace: 'nowrap' }}>
            <span className="jr-h2">추가한 아이</span>
            <span className="jr-badge-count">{added.length}</span>
            <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--text-faint)' }}>가나다순</span>
          </div>
          <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', gap: 8 }}>
            {added.map(c => <AddedRow key={c.name} name={c.name} birth={c.birth} />)}
          </div>
          <button className="jr-btn jr-btn--primary jr-btn--lg" style={{ marginTop: 18 }}>{added.length}명으로 자람 시작하기 <Icon name="chevR" size={20} /></button>
        </div>
      </div>
    </DesktopWindow>
  );
}

// ============ 보존형 삭제(soft delete) 확인 모달 ============
function DeleteChildModal({ onClose, compact, name = '윤서연' }) {
  return (
    <>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(40,30,20,.36)', zIndex: 20 }} />
      <div style={{ position: 'absolute', inset: 0, zIndex: 21, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: compact ? 24 : 40 }}>
        <div style={{ background: 'var(--surface)', borderRadius: 24, padding: compact ? '24px 22px' : '28px 30px', width: '100%', maxWidth: 420, boxShadow: 'var(--shadow-lg)', textAlign: 'center' }}>
          <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'rgba(240,140,125,.14)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}><Icon name="heart" size={28} style={{ color: 'var(--warn)' }} /></div>
          <div className="jr-h2">{name}이를 명단에서 숨길까요?</div>
          <div className="jr-banner" style={{ marginTop: 16, textAlign: 'left' }}>
            <Icon name="check" size={22} stroke={2.4} style={{ color: 'var(--brand-700)', flex: '0 0 auto' }} />
            <span style={{ fontSize: 13.5, fontWeight: 700 }}>기록은 그대로 보존되며, 명단에서만 숨겨져요. 완전히 삭제되지 않아요.</span>
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
            <button className="jr-btn jr-btn--secondary" onClick={onClose} style={{ flex: 1 }}>취소</button>
            <button className="jr-btn jr-btn--warn" onClick={onClose} style={{ flex: 1 }}>명단에서 숨기기</button>
          </div>
        </div>
      </div>
    </>
  );
}

// ============ 아이 정보 수정 (실명·생년월일 수정 + 보존형 삭제) ============
function EditChildMobile({ delModal = false }) {
  const [open, setOpen] = React.useState(delModal);
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 16px 12px' }}>
          <button style={{ border: 'none', background: 'transparent', color: 'var(--text)', cursor: 'pointer', padding: 4 }}><Icon name="back" size={24} /></button>
          <span className="jr-h1" style={{ fontSize: 21 }}>아이 정보 수정</span>
        </div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 22px' }}>
          <div className="jr-card" style={{ padding: 18 }}>
            <RegisterFields name="김민준" birth="2020.11.03" sex="남" centerPhoto />
          </div>
          <button onClick={() => setOpen(true)} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7, width: '100%', marginTop: 18, padding: '13px', border: 'none', background: 'transparent', color: 'var(--warn)', cursor: 'pointer', fontFamily: 'inherit', fontSize: 14.5, fontWeight: 700 }}><Icon name="x" size={18} /> 명단에서 숨기기 (기록 보존)</button>
        </div>
        <div style={{ padding: '12px 22px 26px', background: 'linear-gradient(transparent, var(--bg) 30%)' }}>
          <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg"><Icon name="check" size={22} stroke={2.6} /> 저장하기</button>
        </div>
      </div>
      {open && <DeleteChildModal name="민준" onClose={() => setOpen(false)} compact />}
    </PhoneFrame>
  );
}

function EditChildDesktop({ delModal = false }) {
  const [open, setOpen] = React.useState(delModal);
  return (
    <DesktopWindow url="app.jaram.kr/children/minjun/edit">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="children" teacher={R().teacher} cls={R().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 48px', display: 'flex', flexDirection: 'column', alignItems: 'center', position: 'relative' }}>
          <div style={{ width: '100%', maxWidth: 480 }}>
            <button style={{ display: 'flex', alignItems: 'center', gap: 6, border: 'none', background: 'transparent', color: 'var(--text-sub)', cursor: 'pointer', fontFamily: 'inherit', fontSize: 14, fontWeight: 600, marginBottom: 18, padding: 0, whiteSpace: 'nowrap' }}><Icon name="back" size={18} /> 민준이 타임라인</button>
            <div className="jr-display" style={{ fontSize: 26 }}>아이 정보 수정</div>
            <div style={{ fontSize: 14, color: 'var(--text-sub)', marginTop: 6, marginBottom: 22 }}>실명·생년월일을 수정할 수 있어요 · 햇살반</div>
            <div className="jr-card" style={{ padding: 28 }}>
              <RegisterFields name="김민준" birth="2020.11.03" sex="남" centerPhoto />
              <div style={{ display: 'flex', alignItems: 'center', marginTop: 24 }}>
                <button onClick={() => setOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: 6, border: 'none', background: 'transparent', color: 'var(--warn)', cursor: 'pointer', fontFamily: 'inherit', fontSize: 14, fontWeight: 700, padding: 0 }}><Icon name="x" size={17} /> 명단에서 숨기기</button>
                <div style={{ marginLeft: 'auto', display: 'flex', gap: 12 }}>
                  <button className="jr-btn jr-btn--ghost">취소</button>
                  <button className="jr-btn jr-btn--primary" style={{ paddingLeft: 26, paddingRight: 26 }}><Icon name="check" size={21} stroke={2.6} /> 저장하기</button>
                </div>
              </div>
            </div>
          </div>
          {open && <DeleteChildModal name="민준" onClose={() => setOpen(false)} />}
        </div>
      </div>
    </DesktopWindow>
  );
}

// ============ 학기 중 단일 추가 (아이들 → 아이 등록하기) ============
function AddChildMobile() {
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 16px 12px' }}>
          <button style={{ border: 'none', background: 'transparent', color: 'var(--text)', cursor: 'pointer', padding: 4 }}><Icon name="back" size={24} /></button>
          <span className="jr-h1" style={{ fontSize: 21 }}>새 친구 등록</span>
        </div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 22px' }}>
          <div className="jr-banner" style={{ marginBottom: 18 }}>
            <Icon name="heart" size={22} style={{ color: 'var(--brand-700)', flex: '0 0 auto' }} />
            <span style={{ fontSize: 13.5, fontWeight: 600 }}>학기 중에 온 친구도 언제든 등록할 수 있어요.</span>
          </div>
          <div className="jr-card" style={{ padding: 18 }}>
            <RegisterFields name="" birth="" sex="여" centerPhoto />
          </div>
        </div>
        <div style={{ padding: '12px 22px 26px', background: 'linear-gradient(transparent, var(--bg) 30%)' }}>
          <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg"><Icon name="check" size={22} stroke={2.6} /> 햇살반에 등록하기</button>
        </div>
      </div>
    </PhoneFrame>
  );
}

function AddChildDesktop() {
  return (
    <DesktopWindow url="app.jaram.kr/children/new">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="children" teacher={R().teacher} cls={R().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 48px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ width: '100%', maxWidth: 480 }}>
            <button style={{ display: 'flex', alignItems: 'center', gap: 6, border: 'none', background: 'transparent', color: 'var(--text-sub)', cursor: 'pointer', fontFamily: 'inherit', fontSize: 14, fontWeight: 600, marginBottom: 18, padding: 0, whiteSpace: 'nowrap' }}><Icon name="back" size={18} /> 아이 목록</button>
            <div className="jr-display" style={{ fontSize: 26 }}>새 친구 등록</div>
            <div style={{ fontSize: 14, color: 'var(--text-sub)', marginTop: 6, marginBottom: 22 }}>학기 중에 온 친구도 언제든 등록할 수 있어요 · 햇살반</div>
            <div className="jr-card" style={{ padding: 28 }}>
              <RegisterFields name="" birth="" sex="여" centerPhoto />
              <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
                <button className="jr-btn jr-btn--ghost" style={{ flex: '0 0 auto' }}>취소</button>
                <button className="jr-btn jr-btn--primary" style={{ flex: 1 }}><Icon name="check" size={21} stroke={2.6} /> 햇살반에 등록하기</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DesktopWindow>
  );
}

Object.assign(window, { PhotoSlot, OnboardMobile, OnboardDesktop, AddChildMobile, AddChildDesktop, EditChildMobile, EditChildDesktop });
