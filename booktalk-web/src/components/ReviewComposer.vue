<script setup>
import { ref } from 'vue'
import { X } from 'lucide-vue-next'
import { publishBookReview } from '../api/booktalk'
import BookCover from './BookCover.vue'
import BookSearchPicker from './BookSearchPicker.vue'
import TagPicker from './TagPicker.vue'

const props = defineProps({ book: { type: Object, default: null } })
const emit = defineEmits(['close', 'published'])
const selectedBook = ref(props.book)
const submitting = ref(false)
const error = ref('')
const form = ref({ score: 8, title: '', content: '', tagIds: [] })

async function submit() {
  if (!selectedBook.value?.id || !form.value.content.trim()) return
  submitting.value = true
  error.value = ''
  try {
    await publishBookReview({
      bookId: selectedBook.value.id,
      type: form.value.title.trim() ? 1 : 0,
      score: form.value.score,
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      tagIds: form.value.tagIds,
    })
    emit('published', selectedBook.value)
  } catch (cause) {
    error.value = cause.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="composer-mask" @click.self="emit('close')">
    <form class="review-composer" @submit.prevent="submit">
      <button class="icon-button composer-close" type="button" aria-label="关闭" @click="emit('close')"><X :size="17" /></button>
      <p class="eyebrow">WRITE A REVIEW</p><h2>写书评</h2>
      <div v-if="book" class="review-book"><span><BookCover :src="book.coverUrl || book.cover" :alt="`${book.title} 封面`" /></span><div><strong>{{ book.title }}</strong><small>{{ book.author }}</small></div></div>
      <BookSearchPicker v-else v-model="selectedBook" label="选择要评论的图书" />
      <label>评分<input v-model.number="form.score" type="number" min="1" max="10" required /></label>
      <label>标题（可选）<input v-model.trim="form.title" maxlength="100" /></label>
      <label>内容<textarea v-model="form.content" rows="7" maxlength="5000" required /></label>
      <TagPicker v-model="form.tagIds" :max="5" />
      <p v-if="error" class="form-error">{{ error }}</p>
      <div class="composer-actions"><button class="secondary-button" type="button" @click="emit('close')">取消</button><button class="primary-button" :disabled="submitting || !selectedBook">{{ submitting ? '发布中...' : '发布书评' }}</button></div>
    </form>
  </div>
</template>

<style scoped>
.composer-mask { position:fixed; inset:0; z-index:75; display:grid; place-items:center; overflow:auto; padding:20px; background:rgba(25,35,28,.38); backdrop-filter:blur(2px); }.review-composer { position:relative; width:min(620px,100%); max-height:calc(100vh - 40px); overflow:auto; display:grid; gap:13px; padding:26px; border:1px solid #d9dfd9; border-radius:6px; background:#fff; }.review-composer h2 { margin:0 0 3px; font-size:24px; }.review-composer label { display:grid; gap:6px; color:#59655c; font-size:14px; }.review-composer input,.review-composer textarea { width:100%; border:1px solid #cdd7ce; border-radius:5px; padding:10px; background:#fff; font:inherit; }.review-composer textarea { resize:vertical; }.composer-close { position:absolute; top:10px; right:10px; }.composer-actions { display:flex; justify-content:flex-end; gap:8px; }.review-book { display:grid; grid-template-columns:42px minmax(0,1fr); gap:11px; align-items:center; border:1px solid #d8dfd8; border-radius:5px; padding:9px; background:#f5f7f4; }.review-book > span { width:42px; aspect-ratio:2/3; overflow:hidden; border-radius:3px; }.review-book :deep(img) { width:100%; height:100%; object-fit:cover; }.review-book strong,.review-book small { display:block; }.review-book small { margin-top:4px; color:#788179; }.form-error { margin:0; color:#a33f35; font-size:13px; }
@media (max-width:720px) { .review-composer { padding:22px 18px; } }
.review-composer label { font-size:16px; }
.form-error { font-size:14px; }
</style>
