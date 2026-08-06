<script setup>
import { ref, watch } from 'vue'
import { BookOpen } from 'lucide-vue-next'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, required: true },
})

const loadFailed = ref(false)
watch(() => props.src, () => { loadFailed.value = false })
</script>

<template>
  <img v-if="src && !loadFailed" :src="src" :alt="alt" @error="loadFailed = true" />
  <span v-else class="book-cover-placeholder" role="img" :aria-label="`${alt}，暂无封面`">
    <span class="cover-bar" aria-hidden="true"></span>
    <BookOpen :size="30" stroke-width="1.8" aria-hidden="true" />
    <strong>BOOKTALK</strong>
    <small>READ / SHARE</small>
  </span>
</template>

<style scoped>
img, .book-cover-placeholder { width:100%; height:100%; object-fit:cover; }
.book-cover-placeholder { container-type:inline-size; position:relative; display:grid; place-items:center; align-content:center; gap:7px; overflow:hidden; padding:8%; color:#202326; background:#f2f8f8; border:1px solid #d5e4e4; }
.book-cover-placeholder::after { content:""; position:absolute; right:0; bottom:13%; width:32%; height:8px; background:#ec5d83; }
.cover-bar { position:absolute; top:0; left:0; width:100%; height:9px; background:#118f98; }
.book-cover-placeholder svg { color:#118f98; }
.book-cover-placeholder strong { max-width:100%; font-size:clamp(10px,14cqw,18px); line-height:1; letter-spacing:0; white-space:nowrap; }
.book-cover-placeholder small { max-width:100%; font-size:clamp(7px,8cqw,11px); line-height:1; color:#687174; letter-spacing:0; white-space:nowrap; }
</style>
