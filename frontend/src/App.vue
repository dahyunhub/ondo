<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BottomTab from './components/BottomTab.vue'

const route = useRoute()
const TAB_ROUTES = ['home', 'children', 'journal', 'me', 'timeline']
const showTab = computed(() => TAB_ROUTES.includes(route.name))
</script>

<template>
  <div class="jr app-root">
    <div class="app-col">
      <main class="app-main">
        <RouterView v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
      <BottomTab v-if="showTab" />
    </div>
  </div>
</template>
