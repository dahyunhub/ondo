// screens-misc.jsx — 화면0 로그인·반선택 / 화면1 홈 / 화면5 아이 목록
const M = () => window.JR_DATA;

// ---------- 반 선택 카드 ----------
function ClassCard({ name, count, year, tag, on, wide }) {
  const isNow = tag === '올해';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: wide ? '18px 22px' : '16px 18px', borderRadius: 18, cursor: 'pointer',
      background: on ? 'var(--brand-100)' : 'var(--surface)', border: '2px solid ' + (on ? 'var(--brand-500)' : 'var(--hair-strong)'), boxShadow: on ? '0 6px 18px rgba(245,185,64,.25)' : 'var(--shadow-sm)' }}>
      <span style={{ width: 52, height: 52, borderRadius: 16, flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', background: on ? 'var(--brand-300)' : 'var(--surface-soft)' }}><Icon name={isNow ? 'sun' : 'leaf'} size={26} style={{ color: on ? '#9A6B12' : 'var(--text-sub)' }} /></span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, whiteSpace: 'nowrap' }}>
          <span style={{ fontSize: 18, fontWeight: 800 }}>{name}</span>
          <span className="jr-chip" style={{ background: isNow ? 'rgba(127,209,174,.22)' : 'var(--surface-soft)', color: isNow ? '#3C8F62' : 'var(--text-faint)', fontSize: 11 }}>{tag}</span>
        </div>
        <div style={{ fontSize: 13, color: 'var(--text-sub)', marginTop: 3, fontWeight: 600, whiteSpace: 'nowrap' }}>{year} · 원아 {count}명</div>
      </div>
      {on && <Icon name="check" size={22} stroke={2.6} style={{ color: 'var(--brand-700)', flex: '0 0 auto' }} />}
    </div>);

}

function LoginMobile() {
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column', padding: '20px 24px 28px' }}>
        <div style={{ textAlign: 'center', paddingTop: 30, paddingBottom: 26, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <LogoVertical height={150} />
          <div className="jr-body" style={{ color: 'var(--text-sub)', marginTop: 16 }}>오늘도 우리 함께해요 ☀️</div>
        </div>
        <div style={{ fontSize: 14, fontWeight: 800, color: 'var(--text-sub)', marginBottom: 14 }}>담당 반을 선택해 주세요</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, flex: 1 }}>
          {M().classes.map((c, i) => <ClassCard key={c.name + c.year} {...c} on={i === 0} />)}
        </div>
        <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" style={{ marginTop: 18 }}>햇살반으로 시작하기 <Icon name="chevR" size={20} /></button>
      </div>
    </PhoneFrame>);

}

function LoginDesktop() {
  return (
    <DesktopWindow url="app.jaram.kr/login" height={620}>
      <div style={{ display: 'flex', height: '100%' }}>
        {/* 좌: 브랜드 */}
        <div style={{ flex: '0 0 46%', background: 'linear-gradient(165deg, #FFF6DC, #FFFBF2)', borderRight: '1px solid var(--hair)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: 40 }}>
          <LogoVertical height={190} />
          <div style={{ fontSize: 18, fontWeight: 700, color: '#7a5e22', marginTop: 18 }}>오늘도 우리 함께해요 ☀️</div>
          <div style={{ fontSize: 14, color: '#8a6c2c', marginTop: 24, lineHeight: 1.6, maxWidth: 300 }}>짧은 메모만 남기면 AI가 일지를 만들고,<br />기록은 아이별로 차곡차곡 쌓여요.</div>
        </div>
        {/* 우: 반 선택 */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: '0 56px' }}>
          <div className="jr-h1" style={{ marginBottom: 4 }}>김민서 선생님, 환영해요</div>
          <div className="jr-body" style={{ color: 'var(--text-sub)', marginBottom: 26 }}>담당 반을 선택해 주세요</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14, maxWidth: 420 }}>
            {M().classes.map((c, i) => <ClassCard key={c.name + c.year} {...c} on={i === 0} wide />)}
          </div>
          <button className="jr-btn jr-btn--primary jr-btn--lg" style={{ marginTop: 26, alignSelf: 'flex-start', paddingLeft: 32, paddingRight: 32 }}>햇살반으로 시작하기 <Icon name="chevR" size={20} /></button>
        </div>
      </div>
    </DesktopWindow>);

}

// ---------- 홈 ----------
function JournalCta({ wide }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: wide ? '22px 26px' : '18px 20px', borderRadius: 20,
      background: 'linear-gradient(135deg, #FFD45E, #F5B940)', boxShadow: '0 8px 22px rgba(245,185,64,.4)', cursor: 'pointer' }}>
      <span style={{ width: wide ? 56 : 48, height: wide ? 56 : 48, borderRadius: 16, flex: '0 0 auto', background: 'rgba(255,255,255,.45)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text)' }}><Icon name="sparkle" size={wide ? 30 : 26} /></span>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: wide ? 19 : 17, fontWeight: 800, color: 'var(--text)' }}>AI 일지 쓰기</div>
        <div style={{ fontSize: 13.5, color: '#7a5e22', fontWeight: 600, marginTop: 2 }}>오늘 메모 4개로
하루 일지를 만들어요</div>
      </div>
      <Icon name="chevR" size={24} style={{ color: 'var(--text)' }} />
    </div>);
}

function MemoFeed({ compact }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {M().todayMemos.map((m, i) =>
      <div key={i} className="jr-card" style={{ padding: '14px 16px' }}>
          <div className="jr-memo">
            <Avatar name={m.name} />
            <div className="body">
              <div className="top"><span className="name">{m.name}</span><NuriChip area={m.area} /><span className="time">{m.time}</span></div>
              <div className="txt">{m.txt}</div>
            </div>
          </div>
        </div>
      )}
    </div>);

}

function HomeMobile() {
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <MobileHeader cls={M().cls} date={M().date} right={<Avatar name={M().teacher} size="sm" />} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 20px' }}>
          <div className="jr-h1" style={{ marginBottom: 4 }}>민서 선생님,<br />좋은 하루예요 ☀️</div>
          <div style={{ fontSize: 14, color: 'var(--text-sub)', marginBottom: 20 }}>오늘 <b style={{ color: 'var(--text)' }}>4개</b>의 메모를 남겼어요.</div>
          <div style={{ marginBottom: 24 }}><JournalCta /></div>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 12 }}>
            <span className="jr-h2" style={{ fontSize: 18, whiteSpace: 'nowrap' }}>오늘의 메모</span>
            <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--text-faint)' }}>4개</span>
          </div>
          <MemoFeed />
        </div>
        <Fab />
        <BottomTab active="home" />
      </div>
    </PhoneFrame>);

}

function HomeDesktop() {
  return (
    <DesktopWindow url="app.jaram.kr/home">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="home" teacher={M().teacher} cls={M().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 48px' }}>
          <div style={{ display: 'flex', alignItems: 'flex-end', marginBottom: 28 }}>
            <div>
              <div className="jr-display">민서 선생님, 좋은 하루예요 ☀️</div>
              <div style={{ fontSize: 15, color: 'var(--text-sub)', marginTop: 6 }}>{M().date} · {M().cls} · 오늘 메모 4개</div>
            </div>
            <button className="jr-btn jr-btn--primary" style={{ marginLeft: 'auto' }}><Icon name="plus" size={22} stroke={2.6} /> 빠른 메모</button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: 28, alignItems: 'start' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', marginBottom: 14 }}>
                <span className="jr-h2">오늘의 메모</span>
                <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--text-faint)' }}>4개</span>
              </div>
              <MemoFeed />
            </div>
            <div style={{ position: 'sticky', top: 0 }}>
              <JournalCta wide />
              <div className="jr-card" style={{ marginTop: 18 }}>
                <div style={{ fontSize: 14, fontWeight: 800, marginBottom: 12 }}>이번 주 기록</div>
                <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end', height: 70 }}>
                  {[5, 8, 4, 9, 4, 0, 0].map((v, i) =>
                  <div key={i} style={{ flex: 1, textAlign: 'center' }}>
                      <div style={{ height: v * 6 + 2, background: i === 4 ? 'var(--brand-500)' : 'var(--brand-300)', borderRadius: 6 }} />
                      <div style={{ fontSize: 10, color: 'var(--text-faint)', marginTop: 5 }}>{'월화수목금토일'[i]}</div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DesktopWindow>);

}

// ---------- 아이 목록 ----------
function KidCard({ name, memos }) {
  return (
    <div className="jr-card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 9, padding: '20px 12px', cursor: 'pointer', textAlign: 'center' }}>
      <Avatar name={name} size="lg" />
      <div style={{ fontSize: 15, fontWeight: 800, whiteSpace: 'nowrap' }}>{name}</div>
      <span className="jr-chip" style={{ background: 'var(--surface-soft)', color: 'var(--text-sub)', fontSize: 11.5 }}><Icon name="journal" size={12} /> 기록 {memos}</span>
    </div>);

}

// 아이 등록하기 카드 (점선 + 플러스)
function AddKidCard() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 9, padding: '20px 12px', cursor: 'pointer', textAlign: 'center',
      borderRadius: 'var(--r-card)', border: '2px dashed var(--brand-300)', background: 'var(--brand-100)' }}>
      <span style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--brand-700)', boxShadow: 'var(--shadow-sm)' }}><Icon name="plus" size={26} stroke={2.6} /></span>
      <div style={{ fontSize: 15, fontWeight: 800, whiteSpace: 'nowrap', color: 'var(--text)' }}>아이 등록하기</div>
      <span style={{ fontSize: 11.5, color: 'var(--text-sub)', fontWeight: 600, whiteSpace: 'nowrap' }}>새 친구를 맞이해요</span>
    </div>);

}

function ChildListMobile() {
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '6px 20px 10px' }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, whiteSpace: 'nowrap' }}>
            <span className="jr-h1">아이들</span>
            <span style={{ fontSize: 14, color: 'var(--text-sub)' }}>{M().cls} · {M().children.length}명</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 14 }}>
            <span style={{ fontSize: 13, color: 'var(--text-faint)', fontWeight: 600 }}>가나다순</span>
            <button className="jr-btn jr-btn--primary" style={{ marginLeft: 'auto', flex: '0 0 auto', minHeight: 44, padding: '0 18px' }}><Icon name="plus" size={21} stroke={2.6} /> 아이 등록하기</button>
          </div>
        </div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '8px 20px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            {M().children.map((c) => <KidCard key={c.name} name={c.name} memos={c.memos} />)}
          </div>
        </div>
        <BottomTab active="children" />
      </div>
    </PhoneFrame>);

}

function ChildListDesktop() {
  return (
    <DesktopWindow url="app.jaram.kr/children">
      <div style={{ display: 'flex', height: '100%' }}>
        <Sidebar active="children" teacher={M().teacher} cls={M().cls} />
        <div style={{ flex: 1, overflow: 'hidden', padding: '40px 48px' }}>
          <div style={{ display: 'flex', alignItems: 'flex-end', marginBottom: 24 }}>
            <div>
              <div className="jr-display">아이들</div>
              <div style={{ fontSize: 15, color: 'var(--text-sub)', marginTop: 6, whiteSpace: 'nowrap' }}>{M().cls} · 가나다순 · {M().children.length}명</div>
            </div>
            <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 12 }}>
              <button className="jr-btn jr-btn--primary"><Icon name="plus" size={21} stroke={2.6} /> 아이 등록하기</button>
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 16 }}>
            {M().children.map((c) => <KidCard key={c.name} name={c.name} memos={c.memos} />)}
          </div>
        </div>
      </div>
    </DesktopWindow>);

}

Object.assign(window, { LoginMobile, LoginDesktop, HomeMobile, HomeDesktop, ChildListMobile, ChildListDesktop });