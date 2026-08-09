<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import Logo from '../components/Logo.vue'
import DeviceMockup from '../components/DeviceMockup.vue'
import DesktopMockup from '../components/DesktopMockup.vue'

const router = useRouter()

// 모든 CTA는 로그인 화면으로. 회원가입 유도 버튼은 가입 탭이 열리도록 쿼리를 붙인다.
function goLogin(signup = false) {
  router.push({ name: 'login', query: signup ? { tab: 'signup' } : {} })
}

// 스크롤 상단 고정 내비 배경 토글 대상
const navEl = ref(null)

// 정리용 핸들러 모음 — SPA라 컴포넌트가 사라질 때 반드시 떼어낸다.
let cleanups = []
let dustRAF = 0

onMounted(() => {
  // 소개 페이지는 늘 맨 위부터 — 새로고침 시 브라우저의 스크롤 복원을 끄고 최상단으로 올린다.
  if ('scrollRestoration' in history) history.scrollRestoration = 'manual'
  window.scrollTo(0, 0)

  const RM = matchMedia('(prefers-reduced-motion: reduce)').matches
  const root = document.querySelector('.intro')
  if (!root) return

  /* 히어로 글자 쪼개기 — 한 글자씩 blur-in */
  root.querySelectorAll('[data-split]').forEach((el, li) => {
    const walk = (node, base) => {
      ;[...node.childNodes].forEach((n) => {
        if (n.nodeType === 3) {
          const frag = document.createDocumentFragment()
          ;[...n.textContent].forEach((c) => {
            const s = document.createElement('span')
            s.className = 'ch'
            s.textContent = c === ' ' ? ' ' : c
            s.style.animationDelay = 0.35 + base.i * 0.045 + li * 0.34 + 's'
            base.i++
            frag.appendChild(s)
          })
          n.replaceWith(frag)
        } else if (n.nodeType === 1) walk(n, base)
      })
    }
    walk(el, { i: 0 })
  })

  /* 하루 기록 tally 20칸 채우기 */
  const tally = root.querySelector('.tally')
  if (tally) {
    for (let i = 0; i < 20; i++) {
      const b = document.createElement('i')
      b.style.transitionDelay = 0.3 + i * 0.045 + 's'
      tally.appendChild(b)
    }
  }
  root.querySelectorAll('.areas .jr-chip').forEach((c, i) => (c.style.transitionDelay = 0.2 + i * 0.1 + 's'))

  /* 스크롤 등장 */
  const io = new IntersectionObserver(
    (es) => es.forEach((e) => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target) } }),
    { threshold: 0.18, rootMargin: '0px 0px -8% 0px' },
  )
  root.querySelectorAll('.rv, .prob, .trust').forEach((el) => io.observe(el))
  cleanups.push(() => io.disconnect())

  /* 스크롤 연동 — 내비 배경 · 메모 모핑 */
  const nav = navEl.value
  const morph = root.querySelector('.morph')
  const memos = [...root.querySelectorAll('[data-memo]')]
  const sheet = root.querySelector('#introSheet')
  const bar = root.querySelector('#introBar')
  // 메모의 처음 흩어진 위치를 기억해 둔다(이 값에 진행도를 곱해 가운데로 모은다).
  const homes = memos.map((m) => ({
    tx: m.style.getPropertyValue('--tx'),
    ty: m.style.getPropertyValue('--ty'),
    rt: m.style.getPropertyValue('--rt'),
  }))
  const clamp01 = (v) => Math.max(0, Math.min(1, v))
  const ease = (t) => (t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2)
  let raf = 0
  function tick() {
    raf = 0
    if (nav) nav.classList.toggle('on', scrollY > 40)
    if (morph && memos.length) {
      const r = morph.getBoundingClientRect()
      const span = r.height - innerHeight
      const p = span > 0 ? clamp01(-r.top / span) : 0
      if (bar) bar.style.setProperty('--p', p.toFixed(3))
      // 메모가 가운데로 모이는 진행도. 페이드아웃(g 0.72→1)이 아래 일지 페이드인과 겹치도록 잡는다.
      const g = ease(clamp01((p - 0.10) / 0.55))
      memos.forEach((m, i) => {
        const h = homes[i]
        m.style.setProperty('--tx', `calc(${h.tx} * ${(1 - g).toFixed(3)})`)
        m.style.setProperty('--ty', `calc(${h.ty} * ${(1 - g).toFixed(3)})`)
        m.style.setProperty('--rt', `calc(${h.rt} * ${(1 - g).toFixed(3)})`)
        m.style.setProperty('--sc', (1 - g * 0.22).toFixed(3))
        m.style.setProperty('--op', clamp01(1 - Math.max(0, (g - 0.72) / 0.28)).toFixed(3))
      })
      // 일지 초안이 떠오르는 구간 — 메모가 옅어질 때 자연스럽게 교차한다.
      const s = ease(clamp01((p - 0.42) / 0.30))
      if (sheet) {
        sheet.style.setProperty('--ssc', (0.86 + s * 0.14).toFixed(3))
        sheet.style.setProperty('--sop', s.toFixed(3))
      }
    }
  }
  const onScroll = () => { if (!raf) raf = requestAnimationFrame(tick) }
  addEventListener('scroll', onScroll, { passive: true })
  addEventListener('resize', onScroll)
  cleanups.push(() => { removeEventListener('scroll', onScroll); removeEventListener('resize', onScroll) })
  tick()

  /* 커서 빛무리 */
  const glow = root.querySelector('#introGlow')
  if (glow && !RM && matchMedia('(pointer:fine)').matches) {
    let tx = innerWidth / 2
    let ty = innerHeight * 0.35
    let cx = tx
    let cy = ty
    const onMove = (e) => { tx = e.clientX; ty = e.clientY; glow.style.opacity = 1 }
    addEventListener('pointermove', onMove, { passive: true })
    let glowRAF = 0
    const loop = () => {
      cx += (tx - cx) * 0.07
      cy += (ty - cy) * 0.07
      glow.style.transform = `translate(${cx}px,${cy}px)`
      glowRAF = requestAnimationFrame(loop)
    }
    loop()
    cleanups.push(() => { removeEventListener('pointermove', onMove); cancelAnimationFrame(glowRAF) })
  }

  /* 햇살 입자 */
  const cv = root.querySelector('#introDust')
  if (cv && !RM) {
    const cx2 = cv.getContext('2d')
    let ps = []
    const COL = ['255,212,94', '255,224,138', '143,217,168', '255,196,152']
    function fit() {
      const d = Math.min(devicePixelRatio || 1, 2)
      cv.width = innerWidth * d
      cv.height = innerHeight * d
      cx2.setTransform(d, 0, 0, d, 0, 0)
      const n = innerWidth < 700 ? 26 : 46
      ps = Array.from({ length: n }, () => ({
        x: Math.random() * innerWidth,
        y: Math.random() * innerHeight,
        r: 1 + Math.random() * 2.6,
        vy: -(0.09 + Math.random() * 0.22),
        vx: (Math.random() - 0.5) * 0.11,
        a: 0.18 + Math.random() * 0.4,
        ph: Math.random() * 6.28,
        c: COL[(Math.random() * COL.length) | 0],
      }))
    }
    fit()
    const onDustResize = () => fit()
    addEventListener('resize', onDustResize)
    const draw = (t) => {
      cx2.clearRect(0, 0, innerWidth, innerHeight)
      ps.forEach((p) => {
        p.y += p.vy
        p.x += p.vx + Math.sin(t / 2600 + p.ph) * 0.16
        if (p.y < -12) { p.y = innerHeight + 12; p.x = Math.random() * innerWidth }
        if (p.x < -12) p.x = innerWidth + 12
        if (p.x > innerWidth + 12) p.x = -12
        cx2.beginPath()
        cx2.arc(p.x, p.y, p.r, 0, 6.283)
        cx2.fillStyle = `rgba(${p.c},${(p.a * (0.62 + 0.38 * Math.sin(t / 1500 + p.ph))).toFixed(3)})`
        cx2.fill()
      })
      dustRAF = requestAnimationFrame(draw)
    }
    dustRAF = requestAnimationFrame(draw)
    cleanups.push(() => { removeEventListener('resize', onDustResize); cancelAnimationFrame(dustRAF) })
  }
})

onBeforeUnmount(() => {
  cleanups.forEach((fn) => fn())
  cleanups = []
  if ('scrollRestoration' in history) history.scrollRestoration = 'auto'
})
</script>

<template>
  <div class="intro">
    <div class="warm" aria-hidden="true"></div>
    <canvas id="introDust" aria-hidden="true"></canvas>
    <div id="introGlow" aria-hidden="true"></div>

    <!-- 내비 -->
    <nav ref="navEl" class="nav">
      <div class="brand">
        <Logo variant="lockup" :height="30" />
      </div>
      <div class="nav-r">
        <div class="nav-l">
          <a href="#how">어떻게 쓰나요</a>
          <a href="#feat">기능</a>
          <a href="#trust">안심</a>
        </div>
        <button class="jr-btn jr-btn--primary jr-btn--sm" @click="goLogin()">시작하기</button>
      </div>
    </nav>

    <!-- 1 · 히어로 -->
    <section class="hero">
      <div>
        <span class="eyebrow rv">선생님을 위한 관찰기록 도구</span>
        <h1>
          <span class="ln" data-split>선생님의 하루가</span>
          <span class="ln" data-split>아이의 <span class="mark">기록</span>이 됩니다</span>
        </h1>
        <p class="hsub">아이를 보며 남긴 메모 세 줄. 그 메모를 모아 온도가 오늘의 관찰일지를 씁니다.</p>
        <div class="hero-cta">
          <button class="jr-btn jr-btn--primary jr-btn--lg" @click="goLogin(true)">우리 반 시작하기</button>
          <a class="jr-btn jr-btn--secondary jr-btn--lg" href="#how">어떻게 쓰는지 보기</a>
        </div>
        <p class="hero-note">회원가입 30초 · 바로 사용 가능</p>
      </div>
      <div class="cue" aria-hidden="true">
        <svg viewBox="0 0 34 44" fill="none">
          <g class="sn">
            <circle cx="17" cy="8" r="4.6" fill="#FFD45E" />
            <g fill="#FBC63C">
              <rect x="15.9" y="0" width="2.2" height="4" rx="1.1" />
              <rect x="15.9" y="0" width="2.2" height="4" rx="1.1" transform="rotate(90 17 8)" />
              <rect x="15.9" y="0" width="2.2" height="4" rx="1.1" transform="rotate(180 17 8)" />
              <rect x="15.9" y="0" width="2.2" height="4" rx="1.1" transform="rotate(270 17 8)" />
            </g>
          </g>
          <g class="plant">
            <path class="st" pathLength="1" d="M17 42V34" stroke="#63B486" stroke-width="2.6" stroke-linecap="round" />
            <g class="lf">
              <path d="M17 34c5 0 8-3 7.4-7.6-4.8-.4-7 2.4-7.4 7.6Z" fill="#8ED49E" />
              <path d="M17 34c-5 0-8-3-7.4-7.6 4.8-.4 7 2.4 7.4 7.6Z" fill="#A2DDAC" />
            </g>
          </g>
        </svg>
        <span>SCROLL</span>
      </div>
    </section>

    <!-- 2 · 문제 -->
    <section class="prob" id="how">
      <div class="scraps" aria-hidden="true">
        <div class="scrap" style="left:4%;top:8%;--r:-4deg">민준이 오늘 블록놀이 오래 했는데… 뭐라고 썼더라</div>
        <div class="scrap" style="right:5%;top:14%;--r:5deg">서윤이 친구랑 다툰 거, 기록해야 하는데</div>
        <div class="scrap" style="left:8%;bottom:16%;--r:3deg">지호 낮잠 안 자고 그림 그렸음</div>
        <div class="scrap" style="right:7%;bottom:10%;--r:-5deg">누리과정 어느 영역이었지…</div>
        <div class="scrap" style="left:44%;top:2%;--r:2deg">하준이 오늘 처음 인사했다 🙂</div>
      </div>
      <div class="wrap">
        <div class="prob-mid">
          <span class="eyebrow rv">선생님의 저녁</span>
          <h2 class="rv d1">낮에는 아이를 보고<br />밤에는 기록을 씁니다</h2>
          <p class="lead rv d2">
            아이 한 명당 관찰기록 한 줄, 스무 명이면 스무 줄. 기억은 흐려지고 저녁은 짧아집니다. 하루를 두 번 사는 셈이에요.
          </p>
          <div class="tally rv d3" aria-hidden="true"></div>
          <p class="tally-cap rv d3">하루에 남겨야 하는 기록 · 아이 20명</p>
        </div>
      </div>
    </section>

    <!-- 3 · 전환 (스티키 모핑) -->
    <section class="morph">
      <div class="morph-stick">
        <div class="morph-in">
          <span class="eyebrow">온도가 하는 일</span>
          <h2 style="margin-top:20px">메모 세 줄이면 됩니다</h2>
          <p class="lead">
            놀이 · 의사소통 · 수업태도. 보이는 대로 적어두면,<br />하나의 일지로 모아 씁니다.
          </p>
          <div class="stack">
            <div class="memo" data-memo style="--tx:-160px;--ty:-70px;--rt:-6deg">
              <span class="k">놀이</span>
              <p class="t">블록놀이 중 친구와 자리를 나눠 앉았다</p>
            </div>
            <div class="memo" data-memo style="--tx:150px;--ty:-20px;--rt:5deg">
              <span class="k">의사소통·상호작용</span>
              <p class="t">“같이 쓰자”고 먼저 제안했다</p>
            </div>
            <div class="memo" data-memo style="--tx:-40px;--ty:90px;--rt:-3deg">
              <span class="k">수업태도</span>
              <p class="t">정리 시간에 끝까지 자리에 남았다</p>
            </div>
            <div class="sheet" id="introSheet">
              <div class="sheet-h">
                <b>김민준 · 오늘의 일지</b><span>초안</span>
              </div>
              <p>
                블록놀이 시간에 친구와 자리를 나눠 앉으며 “같이 쓰자”고 먼저 제안하는 모습을 보였습니다. 정리 시간까지
                자리에 남아 놀이를 마무리하였습니다.
              </p>
              <div class="chips">
                <span class="jr-chip jr-area-social"><span class="dot" />사회관계</span>
                <span class="jr-chip jr-area-comm"><span class="dot" />의사소통</span>
                <span class="jr-chip jr-area-body"><span class="dot" />신체운동·건강</span>
              </div>
            </div>
          </div>
          <div class="bar" id="introBar" aria-hidden="true"><i></i></div>
        </div>
      </div>
    </section>

    <!-- 4 · 기능 — 한 화면에 하나씩, 각 기능마다 디바이스 목업 -->
    <section class="feat" id="feat">
      <div class="wrap feat-head">
        <span class="eyebrow rv">쓰는 방법</span>
        <h2 class="big rv d1">세 화면으로 끝납니다</h2>
        <p class="lead rv d2">적고 · 모아보고 · 일지로. 그 사이는 온도가 알아서 합니다.</p>
      </div>

      <!-- 기능 1 · 빠른 메모 -->
      <div class="feature">
        <div class="wrap feature-inner">
          <div class="feature-media rv">
            <DeviceMockup>
              <div class="mk-head">
                <span class="mk-date">8월 7일 목요일</span>
                <span class="mk-h1">빠른 메모</span>
              </div>
              <div class="mk-kids">
                <div class="mk-kid">
                  <span class="jr-avatar jr-avatar--sm" style="background:#EF9D5E">민준</span>
                  <em>김민준</em>
                </div>
                <div class="mk-kid on">
                  <span class="jr-avatar jr-avatar--sm" style="background:#62AdD0">서윤</span>
                  <em>이서윤</em>
                </div>
                <div class="mk-kid">
                  <span class="jr-avatar jr-avatar--sm" style="background:#5FBA86">지호</span>
                  <em>박지호</em>
                </div>
                <div class="mk-kid">
                  <span class="jr-avatar jr-avatar--sm" style="background:#B07FD6">하준</span>
                  <em>정하준</em>
                </div>
              </div>
              <div class="mk-fields">
                <div class="mk-field">
                  <span class="mk-lab">놀이</span>
                  <p>블록으로 높은 탑을 쌓고 친구에게 자리를 내어 주었어요</p>
                </div>
                <div class="mk-field">
                  <span class="mk-lab">의사소통·상호작용</span>
                  <p>“같이 쓰자”고 먼저 제안했어요</p>
                </div>
                <div class="mk-field empty">
                  <span class="mk-lab">수업태도</span>
                  <p class="ph">본 대로 적어주세요</p>
                </div>
              </div>
              <button class="mk-save">이서윤 메모 저장</button>
            </DeviceMockup>
          </div>
          <div class="feature-text rv d1">
            <span class="step-n">1</span>
            <h3>아이를 보는 그 순간, 10초</h3>
            <p>아이를 고르고 세 칸을 채우면 끝. 놀이·의사소통·수업태도를 보이는 대로 적으면 됩니다.</p>
            <ul>
              <li><svg width="18" height="18" viewBox="0 0 17 17" fill="none"><path d="M4 9l3 3 6-7" stroke="#F5B940" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>한 손으로 쓰는 큰 입력칸</li>
              <li><svg width="18" height="18" viewBox="0 0 17 17" fill="none"><path d="M4 9l3 3 6-7" stroke="#F5B940" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>저장하면 아이별 타임라인에 바로 쌓여요</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 기능 2 · 아이별 타임라인 (목업 오른쪽) -->
      <div class="feature alt">
        <div class="wrap feature-inner">
          <div class="feature-media rv">
            <DesktopMockup active="children">
              <div class="dw-tl-head">
                <span class="jr-avatar jr-avatar--lg" style="background:#EF9D5E">민준</span>
                <div>
                  <div class="dw-name">김민준</div>
                  <div class="dw-sub">햇살반 · 관찰기록 <b>24</b>개</div>
                </div>
              </div>
              <div class="dw-sec-h"><b>관찰 기록 타임라인</b><span>최신순 · 영역 칩을 눌러 수정</span></div>
              <div class="dw-chips">
                <span class="jr-toggle is-on">전체</span>
                <span class="jr-toggle">신체운동·건강</span>
                <span class="jr-toggle">의사소통</span>
                <span class="jr-toggle">사회관계</span>
              </div>
              <div class="dw-date">오늘</div>
              <div class="dw-tl-list">
                <div class="jr-area-block" style="--strip:#FFC94D">
                  <div class="dw-block-top">
                    <span class="jr-chip jr-area-social"><span class="dot" />사회관계</span>
                    <span class="dw-time">오후 2:14</span>
                  </div>
                  <p>블록놀이 중 친구와 자리를 나눠 앉으며 “같이 쓰자”고 먼저 제안했어요</p>
                </div>
                <div class="jr-area-block" style="--strip:#7EC4E8">
                  <div class="dw-block-top">
                    <span class="jr-chip jr-area-comm"><span class="dot" />의사소통</span>
                    <span class="dw-time">오전 10:32</span>
                  </div>
                  <p>친구에게 아침에 있었던 일을 먼저 이야기했어요</p>
                </div>
                <div class="jr-area-block" style="--strip:#FF9E80">
                  <div class="dw-block-top">
                    <span class="jr-chip jr-area-body"><span class="dot" />신체운동·건강</span>
                    <span class="dw-time">오전 9:48</span>
                  </div>
                  <p>정리 시간까지 자리에 남아 놀이를 스스로 마무리했어요</p>
                </div>
              </div>
            </DesktopMockup>
          </div>
          <div class="feature-text rv d1">
            <span class="step-n">2</span>
            <h3>한 아이의 1년이 한 화면에</h3>
            <p>흩어진 메모가 아이별로 모입니다. 영역으로 걸러 보면 무엇이 쌓였고 무엇이 비었는지 바로 보입니다.</p>
            <ul>
              <li><svg width="18" height="18" viewBox="0 0 17 17" fill="none"><path d="M4 9l3 3 6-7" stroke="#F5B940" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>5영역 필터 · 미분류 확인</li>
              <li><svg width="18" height="18" viewBox="0 0 17 17" fill="none"><path d="M4 9l3 3 6-7" stroke="#F5B940" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>학기 단위 개인평가 생성</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 기능 3 · AI 일지 초안 -->
      <div class="feature">
        <div class="wrap feature-inner">
          <div class="feature-media rv">
            <DesktopMockup active="journal" height="auto">
              <div class="dw-jr-head">
                <div>
                  <div class="dw-name">하루 일지</div>
                  <div class="dw-sub">8월 7일 목요일 · 햇살반 · AI 초안</div>
                </div>
                <span class="dw-draft">AI 초안</span>
              </div>
              <div class="dw-hint">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 20l4-1 11-11-3-3L5 16l-1 4z" stroke-linejoin="round" /></svg>
                수정 버튼으로 내용을 다듬을 수 있어요
              </div>
              <div class="dw-blocks">
                <div class="jr-area-block" style="--strip:#FFD45E">
                  <span class="dw-sum-chip">오늘 요약</span>
                  <p>블록놀이에 오래 몰입하며 친구와 자연스럽게 어울린 하루였습니다.</p>
                </div>
                <div class="jr-area-block" style="--strip:#FFC94D">
                  <span class="jr-chip jr-area-social"><span class="dot" />사회관계</span>
                  <p>친구와 자리를 나눠 앉으며 “같이 쓰자”고 먼저 제안하는 모습을 보였습니다.</p>
                </div>
                <div class="jr-area-block" style="--strip:#FF9E80">
                  <span class="jr-chip jr-area-body"><span class="dot" />신체운동·건강</span>
                  <p>정리 시간까지 자리에 남아 놀이를 스스로 마무리하였습니다.</p>
                </div>
              </div>
              <div class="dw-actions">
                <span class="dw-btn ghost">수정</span>
                <span class="dw-btn sec">다시</span>
                <span class="dw-btn pri">확정</span>
              </div>
            </DesktopMockup>
          </div>
          <div class="feature-text rv d1">
            <span class="step-n">3</span>
            <h3>일지는 초안부터 시작</h3>
            <p>오늘의 메모를 모아 문장으로 정리해 드립니다. 선생님은 읽고 고치고 저장하면 됩니다.</p>
            <ul>
              <li><svg width="18" height="18" viewBox="0 0 17 17" fill="none"><path d="M4 9l3 3 6-7" stroke="#F5B940" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>문장 그대로 수정 가능</li>
              <li><svg width="18" height="18" viewBox="0 0 17 17" fill="none"><path d="M4 9l3 3 6-7" stroke="#F5B940" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>다시 만들 때는 항상 확인 후 덮어쓰기</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- 5 · 신뢰 -->
    <section class="trust" id="trust">
      <div class="wrap">
        <div style="text-align:center;max-width:620px;margin:0 auto">
          <span class="eyebrow rv">안심하고 쓰세요</span>
          <h2 class="big rv d1">기록은 사라지지 않습니다</h2>
        </div>
        <div class="trust-grid">
          <div class="jr-card card rv d1">
            <div class="ic">
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none"><path d="M11 2l7 3v6c0 4.2-2.9 7.6-7 9-4.1-1.4-7-4.8-7-9V5l7-3z" stroke="#F5B940" stroke-width="1.8" stroke-linejoin="round" /><path d="M8 11l2.2 2.2L14.5 9" stroke="#F5B940" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg>
            </div>
            <h4>메모는 항상 저장됩니다</h4>
            <p>일지 생성이 실패해도 선생님이 쓴 메모는 그대로 남습니다. 다시 시도하면 됩니다.</p>
          </div>
          <div class="jr-card card rv d2">
            <div class="ic">
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none"><rect x="3" y="4" width="16" height="14" rx="3" stroke="#F5B940" stroke-width="1.8" /><path d="M7 9h8M7 13h5" stroke="#F5B940" stroke-width="1.8" stroke-linecap="round" /></svg>
            </div>
            <h4>누리과정 영역, 언제든 정리</h4>
            <p>메모를 남긴 뒤 타임라인에서 5영역을 지정하고, 언제든 다시 바꿀 수 있어요. 미분류로 둔 기록도 한눈에 보여요.</p>
            <div class="areas">
              <span class="jr-chip jr-area-body"><span class="dot" />신체운동·건강</span>
              <span class="jr-chip jr-area-comm"><span class="dot" />의사소통</span>
              <span class="jr-chip jr-area-social"><span class="dot" />사회관계</span>
              <span class="jr-chip jr-area-art"><span class="dot" />예술경험</span>
              <span class="jr-chip jr-area-nature"><span class="dot" />자연탐구</span>
            </div>
          </div>
          <div class="jr-card card rv d3">
            <div class="ic">
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none"><path d="M11 19s-7-4.3-7-9.2A4.1 4.1 0 0111 7a4.1 4.1 0 017 2.8c0 4.9-7 9.2-7 9.2z" stroke="#F5B940" stroke-width="1.8" stroke-linejoin="round" /></svg>
            </div>
            <h4>떠난 아이의 기록도 남습니다</h4>
            <p>명단에서 내려도 기록은 보존됩니다. 학기가 끝나도 지난 반을 그대로 볼 수 있습니다.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 6 · CTA (한 화면에 담기) -->
    <section class="cta">
      <div class="wrap cta-in">
        <h2 class="rv">오늘 저녁은<br />조금 일찍 퇴근하세요</h2>
        <p class="lead rv d1" style="max-width:34ch;margin-inline:auto">우리 반 아이들을 등록하고, 첫 메모를 남겨보세요.</p>
        <div class="cta-row rv d2">
          <button class="jr-btn jr-btn--primary jr-btn--lg" @click="goLogin(true)">우리 반 시작하기</button>
          <button class="jr-btn jr-btn--secondary jr-btn--lg" @click="goLogin()">로그인하고 이어가기</button>
        </div>
      </div>
      <footer>
        <div class="wrap">
          <div class="foot">
            <div class="brand">
              <Logo variant="lockup" :height="22" />
            </div>
            <span>유치원 교사 업무 도구 · 관찰기록과 일지</span>
          </div>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
/* 로컬 별칭 — 목업 변수명을 프로젝트 토큰에 매핑 */
.intro {
  --sub: var(--text-sub);
  --faint: var(--text-faint);
  --hair2: var(--hair-strong);
  --sh: var(--shadow);
  --sh-lg: var(--shadow-lg);
  --nav: 74px;
  position: relative;
  background: var(--bg);
  color: var(--text);
  min-height: 100vh;
  /* clip(hidden 아님) — hidden 은 스크롤 컨테이너를 만들어 내부 sticky(모핑 섹션)를 깨뜨린다. */
  overflow-x: clip;
  text-wrap: pretty;
}
.intro :deep(a) { color: var(--brand-700); text-decoration: none; }
.intro :deep(a:hover) { color: #d89f28; }

/* 배경 */
.warm {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background: radial-gradient(1100px 620px at 78% -8%, rgba(255,212,94,.30), transparent 62%),
    radial-gradient(880px 560px at 8% 18%, rgba(255,196,152,.16), transparent 60%);
}
#introDust { position: fixed; inset: 0; width: 100%; height: 100%; pointer-events: none; z-index: 0; }
#introGlow {
  position: fixed; width: 760px; height: 760px; left: 0; top: 0; margin: -380px 0 0 -380px;
  pointer-events: none; z-index: 0; opacity: 0; transition: opacity .6s;
  background: radial-gradient(circle, rgba(255,212,94,.20) 0%, rgba(255,224,138,.10) 38%, transparent 68%);
}

/* 내비 */
.nav {
  position: fixed; inset: 0 0 auto; height: var(--nav); z-index: 60;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 clamp(20px, 4vw, 44px); background: rgba(255,252,245,0);
  border-bottom: 1px solid transparent; transition: background .3s, border-color .3s, backdrop-filter .3s;
}
.nav.on { background: rgba(255,252,245,.86); backdrop-filter: blur(14px); border-bottom-color: var(--hair); }
.brand { display: flex; align-items: center; gap: 9px; font-size: 21px; font-weight: 700; }
.nav-r { display: flex; align-items: center; gap: clamp(10px, 2.4vw, 28px); }
.nav-l { display: none; gap: 26px; font-size: 15px; font-weight: 600; color: var(--sub); }
.nav-l :deep(a) { color: inherit; }
.nav-l :deep(a:hover) { color: var(--text); }
@media (min-width: 880px) { .nav-l { display: flex; } }

/* 섹션 공통 */
section { position: relative; z-index: 1; }
.wrap { max-width: 1180px; margin: 0 auto; padding: 0 clamp(20px, 5vw, 40px); }
.eyebrow {
  display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 800; letter-spacing: .06em;
  color: #9a6b12; background: var(--brand-100); border: 1px solid rgba(245,185,64,.35); padding: 8px 15px; border-radius: 999px;
}
h2.big { font-size: clamp(30px, 5.2vw, 54px); font-weight: 800; line-height: 1.24; margin: 22px 0 0; letter-spacing: -.035em; }
.lead { font-size: clamp(16px, 2vw, 19px); font-weight: 500; line-height: 1.72; color: var(--sub); margin: 20px 0 0; max-width: 44ch; }

/* 스크롤 등장 */
.rv { opacity: 0; transform: translateY(26px); transition: opacity .8s cubic-bezier(.2,.7,.2,1), transform .8s cubic-bezier(.2,.7,.2,1); }
.rv.in { opacity: 1; transform: none; }
.rv.d1 { transition-delay: .1s; } .rv.d2 { transition-delay: .2s; } .rv.d3 { transition-delay: .3s; }

/* 1 · 히어로 */
.hero { min-height: 100svh; display: grid; place-items: center; text-align: center; padding: calc(var(--nav) + 40px) 20px 90px; }
.hero h1 { font-size: clamp(36px, 7.4vw, 82px); font-weight: 800; line-height: 1.2; letter-spacing: -.045em; margin: 26px 0 0; }
.hero h1 .ln { display: block; }
.hero :deep(.ch) {
  display: inline-block; opacity: 0; filter: blur(11px); transform: translateY(30px) scale(.96);
  animation: chIn .95s cubic-bezier(.2,.75,.2,1) forwards;
}
@keyframes chIn { to { opacity: 1; filter: blur(0); transform: none; } }
.mark { position: relative; display: inline-block; z-index: 1; }
.mark::after {
  content: ''; position: absolute; left: -.06em; right: -.06em; bottom: .04em; height: .42em; border-radius: .2em;
  background: linear-gradient(100deg, var(--brand-300), var(--brand-500)); z-index: -1; transform: scaleX(0); transform-origin: left;
  animation: sweep .85s cubic-bezier(.3,.8,.25,1) 1.5s forwards;
}
@keyframes sweep { to { transform: scaleX(1); } }
.hsub {
  font-size: clamp(16px, 2.2vw, 21px); font-weight: 500; color: var(--sub); line-height: 1.7; margin: 26px auto 0;
  max-width: 30ch; opacity: 0; animation: fadeUp .9s ease 2.1s forwards;
}
.hero-cta { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; margin-top: 36px; opacity: 0; animation: fadeUp .9s ease 2.3s forwards; }
.hero-note { margin-top: 20px; font-size: 14px; color: var(--faint); font-weight: 500; opacity: 0; animation: fadeUp .9s ease 2.45s forwards; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: none; } }
.cue {
  position: absolute; left: 50%; bottom: 26px; transform: translateX(-50%); display: flex; flex-direction: column;
  align-items: center; gap: 7px; font-size: 12px; font-weight: 700; color: var(--faint); opacity: 0; animation: fadeUp 1s ease 2.7s forwards;
}
.cue svg { width: 34px; height: 44px; overflow: visible; }
.cue :deep(.st) { stroke-dasharray: 1; stroke-dashoffset: 1; animation: draw 1.1s ease 2.9s forwards; }
@keyframes draw { to { stroke-dashoffset: 0; } }
.cue :deep(.lf) { transform-origin: 17px 34px; transform: scale(0); animation: pop .6s cubic-bezier(.34,1.56,.64,1) 3.6s forwards; }
.cue :deep(.plant) { transform-origin: 17px 40px; animation: sway 3.6s ease-in-out 4.3s infinite; }
.cue :deep(.sn) { transform-origin: 17px 8px; animation: spin 6s linear infinite; }
@keyframes pop { 0% { transform: scale(0); } 62% { transform: scale(1.12); } 100% { transform: scale(1); } }
@keyframes sway { 0%, 100% { transform: rotate(0); } 25% { transform: rotate(3deg); } 75% { transform: rotate(-3deg); } }
@keyframes spin { to { transform: rotate(360deg); } }

/* 2 · 문제 */
.prob { padding: clamp(100px, 15vh, 170px) 0; position: relative; overflow: hidden; }
.scraps { position: absolute; inset: 0; pointer-events: none; }
.scrap {
  position: absolute; background: var(--surface); border: 1px solid var(--hair); border-radius: 14px; padding: 13px 16px;
  font-size: 14px; font-weight: 500; color: var(--faint); box-shadow: 0 6px 18px rgba(180,150,90,.10); max-width: 280px;
  opacity: 0; transform: translateY(34px) rotate(var(--r, 0deg)); transition: opacity .9s ease, transform 1.1s cubic-bezier(.2,.7,.2,1);
}
.prob.in .scrap { opacity: 1; transform: translateY(0) rotate(var(--r, 0deg)); }
.prob-mid { position: relative; text-align: center; max-width: 640px; margin: 0 auto; }
.prob-mid h2 { font-size: clamp(28px, 5vw, 50px); font-weight: 800; line-height: 1.28; letter-spacing: -.035em; margin: 0; }
.prob-mid .lead { margin: 22px auto 0; text-align: center; }
.tally { display: flex; gap: 5px; justify-content: center; margin-top: 34px; flex-wrap: wrap; max-width: 420px; margin-inline: auto; }
.tally :deep(i) {
  width: 11px; height: 26px; border-radius: 3px; background: var(--brand-300); opacity: 0; transform: scaleY(.2); transform-origin: bottom;
  transition: opacity .4s, transform .5s cubic-bezier(.34,1.4,.6,1);
}
.prob.in .tally :deep(i) { opacity: .9; transform: scaleY(1); }
.tally-cap { margin-top: 14px; font-size: 13px; font-weight: 700; color: var(--sub); }

/* 3 · 전환 (모핑) — 높이 = 스크롤 스크럽 구간. 짧게 잡아 애니메이션 뒤 빈 구간을 줄인다. */
.morph { height: 200vh; position: relative; }
.morph-stick { position: sticky; top: 0; height: 100svh; display: grid; place-items: center; overflow: hidden; padding: clamp(48px, var(--nav), 74px) 20px 0; }
.morph-in { position: relative; width: 100%; max-width: 760px; text-align: center; }
.morph h2 { font-size: clamp(26px, 4.6vw, 46px); font-weight: 800; letter-spacing: -.035em; line-height: 1.3; margin: 0 0 8px; }
.morph .lead { margin: 14px auto 0; text-align: center; max-width: 38ch; }
.stack { position: relative; height: clamp(210px, 42vh, 340px); margin-top: clamp(18px, 4vh, 48px); }
.memo {
  position: absolute; left: 50%; top: 50%; width: min(78vw, 300px); background: var(--surface); border: 1px solid var(--hair);
  border-radius: 18px; padding: 15px 17px; text-align: left; box-shadow: var(--sh);
  transform: translate(-50%, -50%) translate(var(--tx), var(--ty)) rotate(var(--rt)) scale(var(--sc, 1)); opacity: var(--op, 1);
  transition: transform .12s linear, opacity .12s linear;
}
.memo .k { font-size: 11px; font-weight: 800; letter-spacing: .04em; color: #9a6b12; background: var(--brand-100); padding: 4px 9px; border-radius: 999px; display: inline-block; }
.memo .t { margin-top: 9px; font-size: 14px; font-weight: 500; line-height: 1.55; color: var(--text); }
.sheet {
  position: absolute; left: 50%; top: 50%; width: min(88vw, 470px); background: var(--surface); border: 1px solid var(--hair);
  border-radius: 22px; padding: 22px 24px; text-align: left; box-shadow: var(--sh-lg);
  transform: translate(-50%, -50%) scale(var(--ssc, .86)); opacity: var(--sop, 0); transition: transform .12s linear, opacity .12s linear;
}
.sheet-h { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 13px; border-bottom: 1px solid var(--hair); }
.sheet-h b { font-size: 16px; font-weight: 800; }
.sheet-h span { font-size: 12px; font-weight: 700; color: #9a6b12; background: var(--brand-100); padding: 5px 10px; border-radius: 999px; }
.sheet p { margin: 14px 0 0; font-size: 14.5px; font-weight: 500; line-height: 1.78; color: var(--text); }
.sheet .chips { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 16px; }
.bar { margin: 0 auto; margin-top: clamp(14px, 3.4vh, 34px); width: min(70vw, 260px); height: 5px; border-radius: 99px; background: var(--hair); }
.bar :deep(i) { display: block; height: 100%; border-radius: 99px; background: linear-gradient(90deg, var(--brand-300), var(--brand-700)); width: calc(var(--p, 0) * 100%); }

/* 4 · 기능 — 각 기능이 한 화면(100vh)에 하나씩 */
.feat { padding: clamp(70px, 12vh, 130px) 0 0; }
.feat-head { text-align: center; max-width: 620px; }
.feat-head .lead { margin-inline: auto; text-align: center; }
.feature { min-height: 100vh; display: grid; place-items: center; }
.feature-inner { display: grid; gap: clamp(28px, 5vw, 72px); align-items: center; width: 100%; }
@media (min-width: 940px) {
  .feature-inner { grid-template-columns: 1fr 1fr; }
  .feature.alt .feature-media { order: 2; }
}
.feature-media { display: grid; place-items: center; }
.feature-text .step-n {
  display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; border-radius: 50%;
  background: var(--brand-500); color: var(--text); font-size: 15px; font-weight: 800; box-shadow: 0 4px 12px rgba(245,185,64,.4);
}
.feature-text h3 { font-size: clamp(22px, 3vw, 30px); font-weight: 800; letter-spacing: -.03em; line-height: 1.32; margin: 16px 0 0; }
.feature-text p { font-size: 16px; font-weight: 500; line-height: 1.74; color: var(--sub); margin: 14px 0 0; max-width: 40ch; }
.feature-text ul { margin: 20px 0 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 10px; }
.feature-text li { display: flex; gap: 9px; align-items: flex-start; font-size: 15px; font-weight: 600; color: var(--text); }
.feature-text li svg { flex: 0 0 auto; margin-top: 2px; }

/* 디바이스 화면 내부 목업 (DeviceMockup slot 콘텐츠 — 부모 스코프 스타일) */
/* 빠른 메모 */
.mk-head { display: flex; flex-direction: column; gap: 2px; }
.mk-date { font-size: 11px; font-weight: 700; color: var(--brand-700); }
.mk-h1 { font-size: 19px; font-weight: 800; letter-spacing: -.02em; }
.mk-kids { display: flex; gap: 10px; }
.mk-kid { display: flex; flex-direction: column; align-items: center; gap: 4px; opacity: .55; }
.mk-kid em { font-size: 10px; font-style: normal; font-weight: 600; color: var(--sub); }
.mk-kid.on { opacity: 1; }
.mk-kid.on .jr-avatar { box-shadow: 0 0 0 2.5px var(--brand-500); }
.mk-kid.on em { color: var(--text); font-weight: 800; }
.mk-fields { display: flex; flex-direction: column; gap: 9px; }
.mk-field { background: var(--surface); border: 1.5px solid var(--hair-strong); border-radius: 14px; padding: 10px 12px; }
.mk-field .mk-lab { font-size: 10.5px; font-weight: 800; color: var(--sub); }
.mk-field p { margin: 5px 0 0; font-size: 12.5px; line-height: 1.5; font-weight: 500; color: var(--text); }
.mk-field.empty { background: var(--surface-soft); border-style: dashed; }
.mk-field p.ph { color: var(--text-faint); }
.mk-save {
  margin-top: auto; border: none; font-family: inherit; cursor: default; background: var(--brand-500); color: var(--text);
  font-weight: 800; font-size: 14px; border-radius: 999px; padding: 13px; box-shadow: 0 4px 14px rgba(245,185,64,.35);
}
.mk-save.sm { padding: 10px 20px; font-size: 13px; margin: 0; }
/* 타임라인 */
.mk-tl-head { display: flex; align-items: center; gap: 11px; }
.mk-tl-head b { font-size: 16px; font-weight: 800; display: block; }
.mk-sub { font-size: 11px; color: var(--sub); font-weight: 600; }
.mk-filter { display: flex; gap: 6px; flex-wrap: wrap; }
.mk-filter .jr-chip { font-size: 11px; padding: 5px 10px; }
.mk-filter .jr-chip.on { background: var(--brand-500); color: var(--text); }
.mk-tl { position: relative; padding-left: 20px; display: flex; flex-direction: column; gap: 10px; margin-top: 2px; }
.mk-tl::before { content: ''; position: absolute; left: 5px; top: 6px; bottom: 6px; width: 2px; background: var(--hair-strong); }
.mk-tl-item { position: relative; }
.mk-tl-node { position: absolute; left: -18px; top: 8px; width: 12px; height: 12px; border-radius: 50%; border: 3px solid var(--bg); }
.mk-tl-card { background: var(--surface); border: 1px solid var(--hair); border-radius: 13px; padding: 9px 11px; box-shadow: var(--shadow-sm); }
.mk-tl-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 5px; }
.mk-tl-top .jr-chip { font-size: 10.5px; padding: 4px 9px; }
.mk-time { font-size: 10px; color: var(--text-faint); font-weight: 600; }
.mk-tl-card p { margin: 0; font-size: 12px; line-height: 1.5; font-weight: 500; }
/* 일지 초안 */
.mk-jr-head { display: flex; align-items: center; justify-content: space-between; }
.mk-jr-head b { font-size: 17px; font-weight: 800; display: block; }
.mk-draft { font-size: 10.5px; font-weight: 800; color: #9a6b12; background: var(--brand-100); padding: 5px 10px; border-radius: 999px; }
.jr-area-block .jr-chip { font-size: 10.5px; padding: 4px 9px; }
.jr-area-block p { margin: 7px 0 0; font-size: 12px; line-height: 1.6; font-weight: 500; color: var(--text); }
.mk-jr-actions { margin-top: auto; display: flex; gap: 8px; justify-content: flex-end; align-items: center; }
.mk-ghost { border: none; background: transparent; font-family: inherit; font-weight: 700; font-size: 13px; color: var(--sub); cursor: default; }

/* 데스크톱 창(DesktopMockup) 내부 콘텐츠 — 타임라인·일지 공통 */
.dw-tl-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.dw-tl-head .jr-avatar--lg { width: 46px; height: 46px; font-size: 16px; }
.dw-name { font-size: 17px; font-weight: 800; letter-spacing: -.02em; }
.dw-sub { font-size: 11.5px; color: var(--sub); font-weight: 600; margin-top: 2px; }
.dw-sec-h { display: flex; align-items: baseline; gap: 8px; margin-bottom: 11px; }
.dw-sec-h b { font-size: 14px; font-weight: 800; }
.dw-sec-h span { font-size: 10.5px; color: var(--faint); font-weight: 600; }
.dw-chips { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 14px; }
.dw-chips .jr-toggle { font-size: 11px; padding: 6px 11px; min-height: 0; cursor: default; }
.dw-date { font-size: 11px; font-weight: 700; color: var(--sub); margin-bottom: 8px; }
.dw-tl-list, .dw-blocks { display: flex; flex-direction: column; gap: 9px; }
.dw-tl-list .jr-area-block, .dw-blocks .jr-area-block { padding: 10px 12px 10px 16px; border-radius: 12px; box-shadow: var(--shadow-sm); }
.dw-block-top { display: flex; align-items: center; justify-content: space-between; }
.dw-block-top .jr-chip { font-size: 10.5px; padding: 4px 9px; }
.dw-time { font-size: 10px; color: var(--faint); font-weight: 600; }
/* 일지 초안(데스크톱) */
.dw-jr-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 14px; }
.dw-draft { font-size: 10.5px; font-weight: 800; color: #9a6b12; background: var(--brand-100); padding: 5px 10px; border-radius: 999px; white-space: nowrap; }
.dw-hint { display: flex; align-items: center; gap: 5px; font-size: 10.5px; color: var(--faint); font-weight: 600; margin-bottom: 12px; }
.dw-sum-chip { display: inline-block; font-size: 10.5px; font-weight: 800; color: var(--brand-700); background: var(--brand-100); padding: 4px 10px; border-radius: 999px; }
.dw-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 14px; }
.dw-btn { font-size: 11.5px; font-weight: 700; padding: 8px 16px; border-radius: 999px; }
.dw-btn.ghost { color: var(--sub); }
.dw-btn.sec { background: var(--surface); border: 1.5px solid var(--hair-strong); color: var(--text); }
.dw-btn.pri { background: var(--brand-500); color: var(--text); box-shadow: 0 4px 12px rgba(245,185,64,.35); }

/* 5 · 신뢰 */
.trust { padding: clamp(90px, 14vh, 160px) 0; }
.trust-grid { display: grid; gap: 16px; margin-top: 44px; }
@media (min-width: 760px) { .trust-grid { grid-template-columns: repeat(3, 1fr); } }
.card { padding: 26px; transition: transform .3s cubic-bezier(.2,.8,.2,1.1), box-shadow .3s; }
.card:hover { transform: translateY(-4px); box-shadow: var(--sh-lg); }
.card .ic { width: 44px; height: 44px; border-radius: 13px; background: var(--brand-100); display: grid; place-items: center; }
.card h4 { font-size: 18px; font-weight: 800; margin: 16px 0 0; letter-spacing: -.025em; }
.card p { font-size: 14.5px; font-weight: 500; line-height: 1.68; color: var(--sub); margin: 9px 0 0; }
.areas { display: flex; gap: 7px; flex-wrap: wrap; margin-top: 18px; }
.areas .jr-chip { font-size: 12px; padding: 5px 11px; opacity: 0; transform: translateY(8px) scale(.9); transition: opacity .4s, transform .45s cubic-bezier(.34,1.5,.6,1); }
.trust.in .areas .jr-chip { opacity: 1; transform: none; }

/* 6 · CTA — 한 화면에 담기 */
.cta { position: relative; min-height: 100vh; display: flex; flex-direction: column; overflow: hidden; }
.cta::before {
  content: ''; position: absolute; left: 50%; top: 50%; width: min(150vw, 1300px); height: 100%; transform: translate(-50%, -50%);
  background: radial-gradient(60% 60% at 50% 45%, rgba(255,212,94,.42), rgba(255,224,138,.14) 52%, transparent 76%); pointer-events: none;
}
.cta-in { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: calc(var(--nav) + 20px) 0 40px; }
.cta h2 { position: relative; font-size: clamp(30px, 5.6vw, 58px); font-weight: 800; letter-spacing: -.04em; line-height: 1.24; margin: 0; }
.cta .lead { margin: 22px auto 0; text-align: center; }
.cta-row { position: relative; display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; margin-top: 38px; }
footer { border-top: 1px solid var(--hair); padding: 26px 0 34px; position: relative; z-index: 1; }
.foot { display: flex; flex-wrap: wrap; gap: 14px; align-items: center; justify-content: space-between; font-size: 13.5px; font-weight: 500; color: var(--faint); }

@media (prefers-reduced-motion: reduce) {
  .intro :deep(.ch), .hsub, .hero-cta, .hero-note, .cue { opacity: 1 !important; filter: none !important; transform: none !important; animation: none !important; }
  .rv { opacity: 1; transform: none; }
}
</style>
