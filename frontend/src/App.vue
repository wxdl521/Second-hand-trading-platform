<script setup lang="ts">
import { computed } from 'vue'
import { useBreakpoints } from '@vueuse/core'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppHeaderMain from '@/components/layout/AppHeaderMain.vue'
import AppTabBar from '@/components/layout/AppTabBar.vue'

const route = useRoute()
const breakpoints = useBreakpoints({
  mobile: 0,
  tablet: 768,
  desktop: 992
})

const isDesktop = breakpoints.greaterOrEqual('desktop')
const showShell = computed(() => !route.path.startsWith('/admin'))
const pageShellClass = computed(() => (isDesktop.value ? 'page-shell' : 'page-shell page-shell--mobile'))
</script>

<template>
  <div class="app-shell">
    <AppHeaderMain v-if="showShell && isDesktop" />
    <AppHeader v-else-if="showShell" />
    <main :class="pageShellClass">
      <RouterView />
    </main>
    <AppTabBar v-if="showShell && !isDesktop" />
  </div>
</template>
