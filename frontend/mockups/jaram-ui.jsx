// jaram-ui.jsx — 자람 공유 UI 컴포넌트 (window 전역으로 내보냄)
// Pretendard 기반. 디자인 토큰(tokens.css)에 의존.

// ---------- 누리과정 5영역 ----------
const AREAS = {
  body:   { ko: '신체운동·건강', color: '#FF9E80', chip: 'jr-area-body',   strip: 'jr-strip-body' },
  comm:   { ko: '의사소통',       color: '#7EC4E8', chip: 'jr-area-comm',   strip: 'jr-strip-comm' },
  social: { ko: '사회관계',       color: '#FFC94D', chip: 'jr-area-social', strip: 'jr-strip-social' },
  art:    { ko: '예술경험',       color: '#C9A8E8', chip: 'jr-area-art',    strip: 'jr-strip-art' },
  nature: { ko: '자연탐구',       color: '#8FD9A8', chip: 'jr-area-nature', strip: 'jr-strip-nature' },
  uncat:  { ko: '미분류',         color: '#B7AFA2', chip: 'jr-area-uncat',  strip: 'jr-strip-uncat' },
};

// ---------- 아바타 색 (이름 해시 → 파스텔) ----------
const AVA_COLORS = ['#EF9D5E', '#62AdD0', '#E0AE3C', '#B07FD6', '#5FBA86', '#E8897C', '#8089D2', '#D483AC'];
function avatarColor(name) {
  let h = 0; for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
  return AVA_COLORS[h % AVA_COLORS.length];
}

// ---------- 아이콘 (둥근 라인, stroke 2) ----------
function Icon({ name, size = 22, stroke = 2, style }) {
  const p = { fill: 'none', stroke: 'currentColor', strokeWidth: stroke, strokeLinecap: 'round', strokeLinejoin: 'round' };
  const paths = {
    home: <><path d="M3 10.5L12 3l9 7.5" {...p} /><path d="M5 9.5V20h14V9.5" {...p} /><path d="M9.5 20v-5h5v5" {...p} /></>,
    children: <><circle cx="8" cy="8" r="3" {...p} /><circle cx="16.5" cy="9.5" r="2.4" {...p} /><path d="M2.5 19c0-3 2.5-5 5.5-5s5.5 2 5.5 5" {...p} /><path d="M14.5 19c.2-2.4 1.8-4 4-4 1.6 0 3 .9 3.5 2.3" {...p} /></>,
    journal: <><rect x="4" y="3" width="16" height="18" rx="2.5" {...p} /><path d="M8 8h8M8 12h8M8 16h5" {...p} /></>,
    me: <><circle cx="12" cy="8" r="3.4" {...p} /><path d="M4.5 20c.5-3.8 3.6-6 7.5-6s7 2.2 7.5 6" {...p} /></>,
    plus: <><path d="M12 5v14M5 12h14" {...p} /></>,
    mic: <><rect x="9" y="3" width="6" height="11" rx="3" {...p} /><path d="M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3" {...p} /></>,
    check: <><path d="M4.5 12.5l4.5 4.5L19.5 6.5" {...p} /></>,
    pencil: <><path d="M4 20l4-1 11-11-3-3L5 16l-1 4z" {...p} /><path d="M14.5 5.5l3 3" {...p} /></>,
    refresh: <><path d="M3.5 12a8.5 8.5 0 0 1 14.5-6M20.5 12a8.5 8.5 0 0 1-14.5 6" {...p} /><path d="M18 2.5V6h-3.5M6 21.5V18h3.5" {...p} /></>,
    chevR: <><path d="M9 5l7 7-7 7" {...p} /></>,
    chevL: <><path d="M15 5l-7 7 7 7" {...p} /></>,
    chevD: <><path d="M6 9l6 6 6-6" {...p} /></>,
    cal: <><rect x="3.5" y="5" width="17" height="16" rx="3" {...p} /><path d="M3.5 9.5h17M8 3v4M16 3v4" {...p} /></>,
    sparkle: <><path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3z" {...p} /><path d="M19 15l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7.7-2z" {...p} /></>,
    filter: <><path d="M4 5h16l-6 7v6l-4 2v-8L4 5z" {...p} /></>,
    search: <><circle cx="11" cy="11" r="6.5" {...p} /><path d="M16 16l4 4" {...p} /></>,
    back: <><path d="M11 5l-7 7 7 7M4 12h16" {...p} /></>,
    clock: <><circle cx="12" cy="12" r="8.5" {...p} /><path d="M12 7v5l3.5 2" {...p} /></>,
    list: <><path d="M8 6h12M8 12h12M8 18h12" {...p} /><circle cx="4" cy="6" r="1.1" fill="currentColor" stroke="none" /><circle cx="4" cy="12" r="1.1" fill="currentColor" stroke="none" /><circle cx="4" cy="18" r="1.1" fill="currentColor" stroke="none" /></>,
    chart: <><path d="M4 20V4M4 20h16" {...p} /><path d="M8 16v-3M12 16V9M16 16v-6" {...p} /></>,
    heart: <><path d="M12 20s-7-4.5-7-9.5A3.8 3.8 0 0 1 12 7a3.8 3.8 0 0 1 7 3c0 5-7 9.5-7 9.5z" {...p} /></>,
    sun: <><circle cx="12" cy="12" r="4" {...p} /><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.2 5.2l1.8 1.8M17 17l1.8 1.8M18.8 5.2L17 7M7 17l-1.8 1.8" {...p} /></>,
    leaf: <><path d="M5 19c0-8 6-13 14-13 0 8-5 14-13 14-1 0-1-1-1-1z" {...p} /><path d="M5 19c3-4 6-6 10-7.5" {...p} /></>,
    x: <><path d="M6 6l12 12M18 6L6 18" {...p} /></>,
    camera: <><path d="M3 8.5a2 2 0 0 1 2-2h2l1.2-2h7.6L19 6.5h0a2 2 0 0 1 2 2V18a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8.5z" {...p} /><circle cx="12" cy="13" r="3.6" {...p} /></>,
    cake: <><path d="M4 20h16M5 20v-7a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v7" {...p} /><path d="M4 15c1.2 0 1.2 1.2 2.4 1.2S7.6 15 8.8 15s1.2 1.2 2.4 1.2S12.4 15 13.6 15s1.2 1.2 2.4 1.2S17.2 15 18.4 15" {...p} /><path d="M12 7.5V11M12 5.5v0" {...p} /></>,
    expand: <><path d="M9 4H4v5M15 4h5v5M9 20H4v-5M15 20h5v-5" {...p} /></>,
    logout: <><path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3" {...p} /><path d="M10 8l-4 4 4 4M6 12h10" {...p} /></>,
    bell: <><path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6z" {...p} /><path d="M10 19a2 2 0 0 0 4 0" {...p} /></>,
    download: <><path d="M12 4v11M8 11l4 4 4-4" {...p} /><path d="M5 20h14" {...p} /></>,
    help: <><circle cx="12" cy="12" r="8.5" {...p} /><path d="M9.5 9.5a2.5 2.5 0 0 1 4.5 1.5c0 1.5-2 2-2 3.5" {...p} /><path d="M12 17.5v0" {...p} /></>,
    dots: <><circle cx="5" cy="12" r="1.6" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1.6" fill="currentColor" stroke="none" /><circle cx="19" cy="12" r="1.6" fill="currentColor" stroke="none" /></>,
    swap: <><path d="M7 4L3 8l4 4" {...p} /><path d="M3 8h13a5 5 0 0 1 0 10h-2" {...p} /></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" style={{ display: 'block', ...style }}>{paths[name]}</svg>;
}

// ---------- 누리 영역 칩 ----------
function NuriChip({ area, dot = true }) {
  const a = AREAS[area]; if (!a) return null;
  return <span className={'jr-chip ' + a.chip}>{dot && <span className="dot" />}{a.ko}</span>;
}

// ---------- 아바타 ----------
function Avatar({ name, size = '', color }) {
  const c = color || avatarColor(name);
  const initial = name ? name.slice(name.length > 2 ? 1 : 0) : '?'; // 성 제외 이름 (가독)
  const cls = 'jr-avatar' + (size ? ' jr-avatar--' + size : '');
  return <span className={cls} style={{ background: c }}>{initial}</span>;
}

// ---------- 자라는 새싹 로딩 (단순 도형 + 부드러운 모션) ----------
function SproutLoader({ size = 96 }) {
  return (
    <div style={{ width: size, height: size, position: 'relative' }} className="jr-sprout">
      <svg width={size} height={size} viewBox="0 0 96 96" fill="none">
        {/* 햇살 (회전) — 새싹 위로 띄움 */}
        <g className="jr-sun" style={{ transformOrigin: '48px 18px' }}>
          {[...Array(8)].map((_, i) => (
            <line key={i} x1="48" y1="2" x2="48" y2="8" stroke="#FFD45E" strokeWidth="3" strokeLinecap="round"
              transform={`rotate(${i * 45} 48 18)`} />
          ))}
        </g>
        <circle cx="48" cy="18" r="8" fill="#FFD45E" />
        {/* 화분 흙 */}
        <path d="M30 72 h36 l-3 14 a4 4 0 0 1-4 3 h-22 a4 4 0 0 1-4-3 z" fill="#F0D9B8" />
        <rect x="27" y="68" width="42" height="7" rx="3.5" fill="#E7C79B" />
        {/* 줄기 (자람) */}
        <g className="jr-stem" style={{ transformOrigin: '48px 72px' }}>
          <path d="M48 72 V52" stroke="#7FBF8E" strokeWidth="4" strokeLinecap="round" />
          <path className="jr-leaf jr-leaf-l" d="M48 60 C40 58 35 52 36 46 C44 46 49 51 48 60 Z" fill="#8FD9A8" />
          <path className="jr-leaf jr-leaf-r" d="M48 56 C56 54 61 48 60 42 C52 42 47 47 48 56 Z" fill="#A8E0BB" />
        </g>
      </svg>
    </div>
  );
}

// ---------- 폰 프레임 ----------
function PhoneFrame({ children, time = '9:41', dark = false, height = 812, statusColor }) {
  const W = 390;
  return (
    <div style={{
      width: W + 24, height: height + 24, background: '#2A2520', borderRadius: 56,
      padding: 12, boxShadow: '0 1px 2px rgba(0,0,0,.4), inset 0 0 2px rgba(255,255,255,.15)',
      position: 'relative', flex: '0 0 auto',
    }}>
      <div className="jr" style={{ width: W, height, background: 'var(--bg)', borderRadius: 44, overflow: 'hidden', position: 'relative' }}>
        {/* 상태바 */}
        <div style={{ height: 50, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
          padding: '0 28px 8px', position: 'relative', zIndex: 5, color: statusColor || 'var(--text)' }}>
          <span style={{ fontSize: 15, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{time}</span>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <svg width="18" height="12" viewBox="0 0 18 12" fill="currentColor"><rect x="0" y="7" width="3" height="5" rx="1"/><rect x="5" y="4" width="3" height="8" rx="1"/><rect x="10" y="1.5" width="3" height="10.5" rx="1"/><rect x="15" y="0" width="3" height="12" rx="1" opacity=".35"/></svg>
            <svg width="22" height="12" viewBox="0 0 22 12" fill="none"><rect x="1" y="1" width="18" height="10" rx="3" stroke="currentColor" strokeWidth="1.2" opacity=".5"/><rect x="2.6" y="2.6" width="13" height="6.8" rx="1.6" fill="currentColor"/><rect x="20" y="4" width="1.6" height="4" rx=".8" fill="currentColor" opacity=".5"/></svg>
          </div>
        </div>
        {/* 다이내믹 아일랜드 */}
        <div style={{ position: 'absolute', top: 11, left: '50%', transform: 'translateX(-50%)', width: 116, height: 34, background: '#16120d', borderRadius: 18, zIndex: 6 }} />
        {/* 본문 */}
        <div className="jr" style={{ position: 'absolute', top: 50, left: 0, right: 0, bottom: 0, overflow: 'hidden' }}>
          {children}
        </div>
      </div>
    </div>
  );
}

// ---------- 데스크톱(브라우저) 윈도우 ----------
function DesktopWindow({ children, width = 1180, height = 760, url = 'app.jaram.kr' }) {
  return (
    <div style={{ width, background: '#EDE7DC', borderRadius: 16, boxShadow: '0 1px 3px rgba(0,0,0,.12)', overflow: 'hidden', flex: '0 0 auto' }}>
      {/* 타이틀바 */}
      <div style={{ height: 44, display: 'flex', alignItems: 'center', gap: 14, padding: '0 16px' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          {['#FF6058', '#FFBD2E', '#28C840'].map(c => <span key={c} style={{ width: 12, height: 12, borderRadius: '50%', background: c }} />)}
        </div>
        <div style={{ flex: 1, maxWidth: 420, margin: '0 auto', height: 28, background: '#FBF8F1', borderRadius: 8,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7, fontSize: 13, color: '#9b9384', fontFamily: "'Pretendard', sans-serif" }}>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#b3a98f" strokeWidth="2"><rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>
          {url}
        </div>
        <div style={{ width: 52 }} />
      </div>
      {/* 본문 */}
      <div className="jr" style={{ height, background: 'var(--bg)', overflow: 'hidden', position: 'relative' }}>
        {children}
      </div>
    </div>
  );
}

// ---------- 하단 탭바 (모바일) ----------
function BottomTab({ active = 'home' }) {
  const items = [
    { id: 'home', label: '홈', icon: 'home' },
    { id: 'children', label: '아이들', icon: 'children' },
    { id: 'journal', label: '일지·분석', icon: 'journal' },
    { id: 'me', label: '마이', icon: 'me' },
  ];
  return (
    <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, background: 'rgba(255,255,255,0.92)',
      backdropFilter: 'blur(12px)', borderTop: '1px solid var(--hair)', padding: '10px 8px 22px', zIndex: 8 }}>
      <div style={{ display: 'flex' }}>
        {items.map(it => {
          const on = it.id === active;
          return (
            <div key={it.id} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
              color: on ? 'var(--text)' : 'var(--text-faint)' }}>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center', width: 56, height: 30, borderRadius: 999, background: on ? 'var(--brand-100)' : 'transparent' }}>
                <Icon name={it.icon} size={23} stroke={on ? 2.3 : 2} />
              </div>
              <span style={{ fontSize: 11, fontWeight: on ? 800 : 600 }}>{it.label}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ---------- 사이드바 (데스크톱) ----------
function Sidebar({ active = 'home', teacher = '김민서', cls = '햇살반' }) {
  const items = [
    { id: 'home', label: '홈', icon: 'home' },
    { id: 'children', label: '아이들', icon: 'children' },
    { id: 'journal', label: '일지·분석', icon: 'journal' },
    { id: 'me', label: '마이', icon: 'me' },
  ];
  return (
    <div style={{ width: 248, height: '100%', background: 'var(--surface)', borderRight: '1px solid var(--hair)',
      display: 'flex', flexDirection: 'column', padding: '26px 18px', flex: '0 0 auto' }}>
      <div style={{ padding: '0 8px 24px' }}>
        <LogoLockup height={38} />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {items.map(it => {
          const on = it.id === active;
          return (
            <div key={it.id} style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '13px 14px', borderRadius: 14,
              background: on ? 'var(--brand-100)' : 'transparent', color: on ? 'var(--text)' : 'var(--text-sub)',
              fontWeight: on ? 800 : 600, fontSize: 16 }}>
              <Icon name={it.icon} size={22} stroke={on ? 2.3 : 2} /> {it.label}
            </div>
          );
        })}
      </div>
      <div style={{ marginTop: 'auto', display: 'flex', alignItems: 'center', gap: 11, padding: '12px', borderRadius: 16, background: 'var(--surface-soft)' }}>
        <Avatar name={teacher} size="sm" />
        <div style={{ lineHeight: 1.3, minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 700, whiteSpace: 'nowrap' }}>{teacher} 선생님</div>
          <div style={{ fontSize: 12, color: 'var(--text-sub)', whiteSpace: 'nowrap' }}>{cls}</div>
        </div>
      </div>
    </div>
  );
}

// ---------- 로고 마크 (이미지) ----------
function Logo({ size = 32 }) {
  // 마크 원본 비율 580x690 (세로가 약간 긺) — 정사각 박스 안에 맞춤
  return (
    <img src="assets/logo-mark.png" alt="자람" width={size} height={size}
      style={{ flex: '0 0 auto', objectFit: 'contain', display: 'block' }} />
  );
}

// ---------- 로고 로더 (자람 마크가 위아래로 통통) ----------
function LogoLoader({ size = 120 }) {
  return (
    <div className="jr-logoloader" style={{ width: size, height: size, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <img src="assets/logo-mark.png" alt="자람" width={size} height={size}
        style={{ objectFit: 'contain', display: 'block' }} />
    </div>
  );
}

// ---------- 가로 로크업 (마크 + 자람) ----------
function LogoLockup({ height = 44 }) {
  // 원본 858x439 → 비율 유지
  return (
    <img src="assets/logo-lockup.png" alt="자람" height={height}
      style={{ flex: '0 0 auto', width: 'auto', objectFit: 'contain', display: 'block' }} />
  );
}

// ---------- 세로 로크업 (마크 위 + 자람 아래) ----------
function LogoVertical({ height = 120 }) {
  // 원본 411x666 → 비율 유지
  return (
    <img src="assets/logo-vertical.png" alt="자람" height={height}
      style={{ flex: '0 0 auto', width: 'auto', objectFit: 'contain', display: 'block' }} />
  );
}

// ---------- 타이틀(자람 글자만) ----------
function LogoTitle({ height = 40 }) {
  // 원본 584x292
  return (
    <img src="assets/logo-title.png" alt="자람" height={height}
      style={{ flex: '0 0 auto', width: 'auto', objectFit: 'contain', display: 'block' }} />
  );
}

// ---------- 플로팅 + 메모 버튼 ----------
function Fab({ label = '메모' }) {
  return (
    <div style={{ position: 'absolute', right: 20, bottom: 96, zIndex: 9 }}>
      <button className="jr-btn jr-btn--primary" style={{ height: 58, paddingLeft: 20, paddingRight: 24, boxShadow: '0 8px 22px rgba(245,185,64,0.5)' }}>
        <Icon name="plus" size={24} stroke={2.6} /> {label}
      </button>
    </div>
  );
}

// ---------- 모바일 상단 헤더 (반 + 날짜) ----------
function MobileHeader({ cls = '햇살반', date = '6월 14일 토요일', right }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 20px 14px' }}>
      <button style={{ display: 'flex', alignItems: 'center', gap: 6, background: 'var(--surface)', border: '1.5px solid var(--hair-strong)',
        borderRadius: 999, padding: '7px 12px 7px 14px', fontFamily: 'inherit', fontWeight: 800, fontSize: 15, color: 'var(--text)', cursor: 'pointer' }}>
        {cls} <Icon name="chevD" size={16} stroke={2.4} style={{ color: 'var(--text-sub)' }} />
      </button>
      <span style={{ fontSize: 13, color: 'var(--text-sub)', fontWeight: 600, whiteSpace: 'nowrap' }}>{date}</span>
      <div style={{ marginLeft: 'auto' }}>{right}</div>
    </div>
  );
}

Object.assign(window, {
  AREAS, avatarColor, Icon, NuriChip, Avatar, SproutLoader,
  PhoneFrame, DesktopWindow, BottomTab, Sidebar, Logo, LogoLoader, LogoLockup, LogoVertical, LogoTitle, Fab, MobileHeader,
});
