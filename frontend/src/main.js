import { createApp } from 'vue'
import * as Sentry from '@sentry/vue'
import router from './router'
import App from './App.vue'
import './styles/tokens.css'
import './styles/base.css'

const app = createApp(App)

// Sentry(에러 모니터링). DSN 이 비어 있으면 초기화하지 않는다 → dev·로컬은 완전 무동작.
// DSN 은 공개값(클라이언트 노출 설계)이라 프론트 번들에 포함돼도 안전하다.
const sentryDsn = import.meta.env.VITE_SENTRY_DSN
if (sentryDsn) {
  Sentry.init({
    app,
    dsn: sentryDsn,
    environment: import.meta.env.MODE,
    // 라우트 전환 추적(성능). tracesSampleRate=0 이면 실제 전송은 없다(에러만).
    integrations: [Sentry.browserTracingIntegration({ router })],
    tracesSampleRate: Number(import.meta.env.VITE_SENTRY_TRACES_SAMPLE_RATE ?? 0),
    // IP 등 PII 미전송(NFR-1 개인정보 최소화와 정렬).
    sendDefaultPii: false,
  })
}

app.use(router).mount('#app')
