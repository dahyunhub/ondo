// 실행 화면 캡처 — 설치된 Chrome 사용(브라우저 다운로드 없음).
// 사용: node scripts/shoot.mjs  (frontend dev:5273 + backend:8090 기동 상태에서)
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

async function loginAndEnter(page) {
  await login(page)
  await page.getByPlaceholder('teacher@jaram.dev').fill('teacher@jaram.dev')
  await page.getByPlaceholder('비밀번호를 입력해주세요').fill('password1234')
  await page.locator('form button[type="submit"]').click()
  // 반 선택 또는 홈
  await page.waitForLoadState('networkidle')
  await sleep(700)
  // 반 선택 화면이면 시작하기 클릭
  const start = page.getByRole('button', { name: /시작하기/ })
  if (await start.count()) {
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
  await page.goto(BASE + '/memo', { waitUntil: 'networkidle' }); await shot(page, 'memo-mobile')
  await ctx.close()
}

await browser.close()
console.log('done')
