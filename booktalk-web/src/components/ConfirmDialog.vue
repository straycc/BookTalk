<script setup>
import { AlertTriangle, X } from 'lucide-vue-next'

defineProps({
  title: { type: String, default: '确认操作' },
  description: { type: String, required: true },
  confirming: Boolean,
  confirmText: { type: String, default: '确认删除' },
})
defineEmits(['cancel', 'confirm'])
</script>

<template>
  <div class="confirm-mask" @click.self="$emit('cancel')">
    <section class="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-dialog-title" aria-describedby="confirm-dialog-description">
      <button class="dialog-close" type="button" aria-label="关闭" :disabled="confirming" @click="$emit('cancel')"><X :size="17" /></button>
      <span class="warning-icon"><AlertTriangle :size="20" /></span>
      <h2 id="confirm-dialog-title">{{ title }}</h2>
      <p id="confirm-dialog-description">{{ description }}</p>
      <div class="confirm-actions"><button class="secondary-button" type="button" :disabled="confirming" @click="$emit('cancel')">取消</button><button class="danger-button" type="button" :disabled="confirming" @click="$emit('confirm')">{{ confirming ? '处理中...' : confirmText }}</button></div>
    </section>
  </div>
</template>

<style scoped>
.confirm-mask { position:fixed; inset:0; z-index:80; display:grid; place-items:center; padding:20px; background:rgba(25,35,28,.38); backdrop-filter:blur(2px); }
.confirm-dialog { position:relative; width:min(400px,100%); padding:26px; border:1px solid #d9dfd9; border-radius:6px; background:#fff; box-shadow:0 18px 55px rgba(24,35,27,.18); }
.warning-icon { display:grid; width:38px; height:38px; place-items:center; border-radius:50%; background:#fbecea; color:#a43f35; }
.confirm-dialog h2 { margin:15px 0 7px; font-size:19px; }.confirm-dialog p { margin:0; color:#657068; font-size:14px; line-height:1.6; }
.dialog-close { position:absolute; top:12px; right:12px; display:grid; width:30px; height:30px; place-items:center; border:0; border-radius:4px; background:transparent; color:#6f7971; }
.confirm-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:24px; }
.danger-button { display:inline-flex; min-width:92px; align-items:center; justify-content:center; border:1px solid #9d3b32; border-radius:5px; padding:8px 14px; background:#a94339; color:#fff; font:inherit; }
.danger-button:hover:not(:disabled) { background:#91372f; }.danger-button:disabled,.dialog-close:disabled { cursor:not-allowed; opacity:.6; }
.confirm-dialog h2 { font-size:21px; }
.confirm-dialog p { font-size:16px; }
</style>
