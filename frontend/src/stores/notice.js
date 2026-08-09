// 전역 토스트 — 경량 reactive(스토어 라이브러리 미사용).
// 데모 계정의 읽기 전용 안내처럼 화면 어디서 발생하든 한 곳에서 알려줄 때 쓴다.
import { reactive } from 'vue'

let timer = null

export const notice = reactive({
  message: '',

  show(message) {
    this.message = message
    clearTimeout(timer)
    timer = setTimeout(() => { this.message = '' }, 3200)
  },

  clear() {
    this.message = ''
    clearTimeout(timer)
  },
})
