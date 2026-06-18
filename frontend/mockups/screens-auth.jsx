// screens-auth.jsx — 로그인 · 회원가입 (한 페이지, 상단 탭 전환 + 소셜 로그인)
const A = () => window.JR_DATA;
const { useState } = React;

// ---------- 브랜드 아이콘 ----------
function KakaoIcon({ size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="#191600" style={{ flex: '0 0 auto' }}>
      <path d="M12 3.5C6.75 3.5 2.5 6.84 2.5 10.96c0 2.66 1.78 4.99 4.46 6.31-.2.71-.71 2.57-.81 2.97-.13.49.18.49.38.36.16-.11 2.5-1.7 3.52-2.39.47.07.96.1 1.45.1 5.25 0 9.5-3.34 9.5-7.46S17.25 3.5 12 3.5z" />
    </svg>);

}
function GoogleIcon({ size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" style={{ flex: '0 0 auto' }}>
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </svg>);

}

// ---------- 상단 탭 메뉴 ----------
function AuthTabs({ tab, setTab, big }) {
  const items = [['login', '로그인'], ['signup', '회원가입']];
  return (
    <div style={{ display: 'flex', gap: big ? 28 : 22, borderBottom: '2px solid var(--hair)', marginBottom: big ? 30 : 18 }}>
      {items.map(([id, label]) => {
        const on = tab === id;
        return (
          <button key={id} onClick={() => setTab(id)} style={{
            position: 'relative', border: 'none', background: 'transparent', cursor: 'pointer', fontFamily: 'inherit', whiteSpace: 'nowrap',
            fontSize: big ? 19 : 17, fontWeight: on ? 800 : 600, color: on ? 'var(--text)' : 'var(--text-faint)',
            padding: big ? '0 2px 14px' : '0 2px 12px', transition: 'color .15s' }}>
            {label}
            <span style={{ position: 'absolute', left: 0, right: 0, bottom: -2, height: 3, borderRadius: 3,
              background: on ? 'var(--brand-500)' : 'transparent', transition: 'background .15s' }} />
          </button>);

      })}
    </div>);

}

// ---------- 입력 필드 ----------
function Field({ label, type = 'text', placeholder, value }) {
  return (
    <div>
      <label className="jr-field-label">{label}</label>
      <input className="jr-input" type={type} placeholder={placeholder} defaultValue={value} />
    </div>);

}

// ---------- 소셜 로그인 버튼 묶음 ----------
function SocialButtons({ verb = '시작하기' }) {
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '2px 0 12px' }}>
        <hr className="jr-divider" style={{ flex: 1 }} />
        <span style={{ fontSize: 12.5, color: 'var(--text-faint)', fontWeight: 600, whiteSpace: 'nowrap' }}>또는 간편하게</span>
        <hr className="jr-divider" style={{ flex: 1 }} />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <button className="jr-btn jr-btn--block" style={{ background: '#FEE500', color: '#191600', boxShadow: 'none' }}>
          <KakaoIcon size={20} /> 카카오로 {verb}
        </button>
        <button className="jr-btn jr-btn--block" style={{ background: 'var(--surface)', color: '#3c4043', border: '1.5px solid var(--hair-strong)', boxShadow: 'none' }}>
          <GoogleIcon size={19} /> Google로 {verb}
        </button>
      </div>
    </>);

}

// ---------- 로그인 폼 ----------
function LoginForm({ big }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Field label="이메일" type="email" placeholder="teacher@jaram.kr" />
      <div>
        <Field label="비밀번호" type="password" placeholder="비밀번호를 입력해주세요" />
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginTop: 12 }}>
          <span style={{ width: 18, height: 18, borderRadius: 6, background: 'var(--brand-100)', border: '1.5px solid var(--brand-500)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--brand-700)', flex: '0 0 auto' }}><Icon name="check" size={12} stroke={3} /></span>
          <span style={{ fontSize: 13.5, color: 'var(--text-sub)', fontWeight: 600, whiteSpace: 'nowrap' }}>로그인 상태 유지</span>
          <span style={{ marginLeft: 'auto', fontSize: 13.5, color: 'var(--text-sub)', fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' }}>비밀번호 찾기</span>
        </div>
      </div>
      <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" style={{ marginTop: 4 }}>로그인 <Icon name="chevR" size={20} /></button>
    </div>);

}

// ---------- 회원가입 폼 ----------
function SignupForm({ big }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: big ? 14 : 11 }}>
      <Field label="이름" placeholder="선생님 성함을 입력해주세요" />
      <Field label="이메일" type="email" placeholder="teacher@jaram.kr" />
      <Field label="비밀번호" type="password" placeholder="8자 이상 입력해주세요" />
      <Field label="비밀번호 확인" type="password" placeholder="비밀번호를 한 번 더 입력해주세요" />
      <button className="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" style={{ marginTop: 4, marginBottom: 2 }}>회원가입</button>
      <SocialButtons verb="시작하기" />
      <p style={{ fontSize: 12, color: 'var(--text-faint)', textAlign: 'center', lineHeight: 1.6, marginTop: 4 }}>
        가입 시 <span style={{ color: 'var(--text-sub)', textDecoration: 'underline' }}>이용약관</span>과 <span style={{ color: 'var(--text-sub)', textDecoration: 'underline' }}>개인정보 처리방침</span>에 동의하게 돼요.
      </p>
    </div>);

}

// ---------- 브랜드 패널 (데스크톱 좌측) ----------
function BrandPanel() {
  return (
    <div style={{ flex: '0 0 44%', background: 'linear-gradient(165deg, #FFF6DC, #FFFBF2)', borderRight: '1px solid var(--hair)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: 40 }}>
      <LogoVertical height={190} />
      <div style={{ fontSize: 18, fontWeight: 700, color: '#7a5e22', marginTop: 18, whiteSpace: 'nowrap' }}>오늘도 우리 함께해요 ☀️</div>
      <div style={{ fontSize: 14, color: '#8a6c2c', marginTop: 24, lineHeight: 1.6, maxWidth: 300 }}>짧은 메모만 남기면 AI가 일지를 만들고,<br />기록은 아이별로 차곡차곡 쌓여요.</div>
    </div>);

}

// ---------- 모바일 ----------
function AuthMobile({ initial = 'login' }) {
  const [tab, setTab] = useState(initial);
  return (
    <PhoneFrame>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <div style={{ textAlign: 'center', padding: '12px 0 8px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <LogoVertical height={96} />
        </div>
        <div style={{ flex: 1, overflow: 'hidden', padding: '0 26px' }}>
          <AuthTabs tab={tab} setTab={setTab} />
          {tab === 'login' ? <LoginForm /> : <SignupForm />}
        </div>
      </div>
    </PhoneFrame>);

}

// ---------- 데스크톱 ----------
function AuthDesktop({ initial = 'login' }) {
  const [tab, setTab] = useState(initial);
  return (
    <DesktopWindow url="app.jaram.kr/login" height={620}>
      <div style={{ display: 'flex', height: '100%' }}>
        <BrandPanel />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: '0 56px', overflow: 'hidden' }}>
          <div style={{ maxWidth: 380, width: '100%', margin: '0 auto' }}>
            <div className="jr-h1" style={{ marginBottom: 4 }}>{tab === 'login' ? '다시 오셨네요, 반가워요' : '자람과 함께 시작해요'}</div>
            <div className="jr-body" style={{ color: 'var(--text-sub)', marginBottom: 26 }}>{tab === 'login' ? '메모와 기록이 그대로 기다리고 있어요.' : '몇 가지만 입력하면 바로 시작할 수 있어요.'}</div>
            <AuthTabs tab={tab} setTab={setTab} big />
            {tab === 'login' ? <LoginForm big /> : <SignupForm big />}
          </div>
        </div>
      </div>
    </DesktopWindow>);

}

Object.assign(window, { KakaoIcon, GoogleIcon, AuthMobile, AuthDesktop });