<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    description?: string
    width?: string
  }>(),
  {
    description: '',
    width: '720px'
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const close = () => emit('update:modelValue', false)
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="admin-dialog-backdrop" @click.self="close">
      <section class="admin-dialog" :style="{ width }">
        <header class="admin-dialog__header">
          <div>
            <h3>{{ title }}</h3>
            <p v-if="description">{{ description }}</p>
          </div>
          <button class="admin-dialog__close" type="button" @click="close">×</button>
        </header>

        <div class="admin-dialog__body">
          <slot />
        </div>

        <footer v-if="$slots.footer" class="admin-dialog__footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.admin-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  padding: 28px;
  background: rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(10px);
  display: grid;
  place-items: center;
}

.admin-dialog {
  max-width: 100%;
  max-height: calc(100vh - 56px);
  overflow: hidden;
  border-radius: 32px;
  border: 1px solid rgba(219, 228, 241, 0.96);
  background: rgba(255, 255, 255, 0.99);
  box-shadow: 0 28px 64px rgba(15, 23, 42, 0.18);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.admin-dialog__header,
.admin-dialog__footer {
  padding: 24px 28px;
}

.admin-dialog__header {
  border-bottom: 1px solid rgba(226, 232, 240, 0.88);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.admin-dialog__header h3,
.admin-dialog__header p {
  margin: 0;
}

.admin-dialog__header h3 {
  font-size: 26px;
  line-height: 1.15;
}

.admin-dialog__header p {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.7;
}

.admin-dialog__close {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 14px;
  background: #f1f5f9;
  color: #0f172a;
  font-size: 26px;
  line-height: 1;
}

.admin-dialog__body {
  padding: 24px 28px 4px;
  overflow-y: auto;
}

.admin-dialog__footer {
  border-top: 1px solid rgba(226, 232, 240, 0.88);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
