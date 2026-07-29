<script setup>
// "이번 주 만나볼 아이" — 최근 기록이 옅은 아이를 빠른 메모로 연결한다.
// 서버가 LOW 를 무조건 3명 이하로 제한하므로 이 목록도 최대 3행이다.
// 인지에서 끝내지 않고 바로 행동으로 이어지게 하는 게 이 카드의 존재 이유.
// 대상이 없으면 부모가 아예 렌더하지 않는다 — "오늘은 알려드릴 게 없어요" 같은 빈 상태 문구도
// 결국 기능을 상기시켜 독촉이 되므로 두지 않는다(spec-child-warmth).
import { useRouter } from 'vue-router'
import Avatar from './Avatar.vue'
import AppIcon from './AppIcon.vue'
import WarmthMascot from './WarmthMascot.vue'

defineProps({ children: { type: Array, required: true } })

const router = useRouter()
function openMemo(c) { router.push({ name: 'memo', query: { childId: c.id } }) }
</script>

<template>
  <div class="jr-card meet">
    <div class="meet-top">
      <WarmthMascot level="low" :size="56" />
      <div class="meet-tx">
        <div class="t">이번 주 만나볼 아이</div>
        <!-- 과거형 질책("못 보셨어요") 대신 사실 서술 + 가벼운 제안. -->
        <div class="d">요즘 기록이 조금 옅어요.<br />한 줄이면 충분해요.</div>
      </div>
    </div>
    <div class="meet-list">
      <button v-for="c in children" :key="c.id" class="meet-row" @click="openMemo(c)">
        <Avatar :name="c.name" size="sm" :photo-url="`/children/${c.id}/photo`" :photo-key="c.photoUpdatedAt || ''" />
        <span class="meet-name">{{ c.name }}</span>
        <span class="meet-go">메모 남기기</span>
        <AppIcon name="chevR" :size="16" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.meet { padding: 18px; }
.meet-top { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.meet-tx { min-width: 0; }
.meet-tx .t { font-size: 15.5px; font-weight: 800; }
.meet-tx .d { font-size: 12.5px; color: var(--text-sub); font-weight: 600; margin-top: 3px; line-height: 1.45; }

.meet-list { display: flex; flex-direction: column; gap: 8px; }
.meet-row {
  display: flex; align-items: center; gap: 10px; width: 100%; padding: 9px 12px; border-radius: 14px;
  background: var(--surface-soft); border: 1.5px solid var(--hair); color: var(--text);
  font-family: inherit; text-align: left; cursor: pointer; transition: border-color .12s, background .12s;
}
.meet-row:hover { border-color: var(--brand-500); background: var(--brand-100); }
.meet-name { font-size: 14.5px; font-weight: 800; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meet-go { margin-left: auto; font-size: 12px; font-weight: 700; color: var(--text-faint); white-space: nowrap; }
.meet-row :deep(.jr-avatar) { width: 30px; height: 30px; font-size: 12px; }
</style>
