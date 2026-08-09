// 실행 화면 캡처 — 설치된 Chrome 사용(브라우저 다운로드 없음).
// 읽기 전용 데모 계정(demo@ondo.app)으로 로그인해 캡처한다 → README·라이브 데모가 정확히 일치.
// 사용: 백엔드를 DEMO_ENABLED=true 로 띄운 뒤(시드: 반·아이 23명·메모·일지·평가),
//       frontend dev:5273 기동 상태에서  node scripts/shoot.mjs
import { chromium } from 'playwright'
import { mkdirSync } from 'node:fs'

const BASE = 'http://localhost:5273'
const OUT = '../docs/screenshots'
mkdirSync(OUT, { recursive: true })

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function login(page) {
  await page.goto(BASE + '/login', { waitUntil: 'networkidle' })
  await sleep(400)
  return page
}

// 캡처용 반. 데모 시드(DemoDataInitializer)가 만드는 반으로, 아이·메모·일지·평가가 갖춰져 있어야
// 관찰 온도 같은 기능이 화면에 실제로 나온다.
const DEMO_CLASSROOM = '만 4세반'
const DEMO_EMAIL = 'demo@ondo.app'
const DEMO_PASSWORD = 'ondo-demo'

async function loginAndEnter(page) {
  await login(page)
  await page.getByPlaceholder('teacher@ondo.dev').fill(DEMO_EMAIL)
  await page.getByPlaceholder('비밀번호를 입력해주세요').fill(DEMO_PASSWORD)
  await page.locator('form button[type="submit"]').click()
  // 반 선택 또는 홈
  await page.waitForLoadState('networkidle')
  await sleep(700)
  // 반 선택 화면이면 데모 반을 명시적으로 고르고 시작하기
  const start = page.getByRole('button', { name: /시작하기/ })
  if (await start.count()) {
    const demo = page.getByText(DEMO_CLASSROOM, { exact: true })
    if (await demo.count()) {
      await demo.first().click()
      await sleep(300)
    } else {
      console.warn(`[shoot] '${DEMO_CLASSROOM}' 을 찾지 못해 기본 선택으로 진행합니다.`)
    }
    await start.first().click()
    await page.waitForLoadState('networkidle')
    await sleep(700)
  }
}

async function shot(page, name) {
  await sleep(500)
  await page.screenshot({ path: `${OUT}/${name}.png` })
  console.log('shot', name)
}

// 일지 목록 → 오늘 확정 일지 상세(5영역 본문)로 들어간다.
async function openTodayJournal(page) {
  await page.goto(BASE + '/journal', { waitUntil: 'networkidle' })
  await sleep(400)
  const cta = page.getByText('오늘 일지 보기', { exact: false })
  if (await cta.count()) {
    await cta.first().click()
    await page.waitForLoadState('networkidle')
    await sleep(600)
  }
}

const browser = await chromium.launch({ channel: 'chrome', headless: true })

// ---------- 데스크톱 ----------
{
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 820 }, deviceScaleFactor: 2 })
  const page = await ctx.newPage()
  await login(page)
  await shot(page, 'login-desktop')
  await loginAndEnter(page)
  await shot(page, 'home-desktop')
  await page.goto(BASE + '/children', { waitUntil: 'networkidle' }); await shot(page, 'children-desktop')
  // 첫 아이 → 타임라인
  const kid = page.locator('.kid').first()
  if (await kid.count()) { await kid.click(); await page.waitForLoadState('networkidle'); await shot(page, 'timeline-desktop') }
  await openTodayJournal(page); await shot(page, 'journal-desktop')
  await page.goto(BASE + '/memo', { waitUntil: 'networkidle' }); await shot(page, 'memo-desktop')
  await page.goto(BASE + '/me', { waitUntil: 'networkidle' }); await shot(page, 'me-desktop')
  await ctx.close()
}

// ---------- 모바일 ----------
{
  const ctx = await browser.newContext({ viewport: { width: 402, height: 860 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true })
  const page = await ctx.newPage()
  await login(page)
  await shot(page, 'login-mobile')
  await loginAndEnter(page)
  await shot(page, 'home-mobile')
  await page.goto(BASE + '/children', { waitUntil: 'networkidle' }); await shot(page, 'children-mobile')
  const kid = page.locator('.kid').first()
  if (await kid.count()) { await kid.click(); await page.waitForLoadState('networkidle'); await shot(page, 'timeline-mobile') }
  await openTodayJournal(page); await shot(page, 'journal-mobile')
  await page.goto(BASE + '/memo', { waitUntil: 'networkidle' }); await shot(page, 'memo-mobile')
  await ctx.close()
}

await browser.close()
console.log('done')
