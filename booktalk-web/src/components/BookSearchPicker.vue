<script setup>
import { Check, Search, X } from 'lucide-vue-next'
import { ref } from 'vue'
import { getBooks, searchBooks } from '../api/booktalk'
import BookCover from './BookCover.vue'

defineProps({ modelValue: { type: Object, default: null }, label: { type: String, default: '关联图书（可选）' } })
const emit = defineEmits(['update:modelValue'])
const query = ref('')
const results = ref([])
const searching = ref(false)
const error = ref('')
let timer

function scheduleSearch() {
  clearTimeout(timer)
  timer = setTimeout(runSearch, 300)
}

async function runSearch() {
  const keyword = query.value.trim()
  if (!keyword) {
    results.value = []
    error.value = ''
    return
  }
  searching.value = true
  error.value = ''
  try {
    let result
    try {
      result = await searchBooks(keyword)
    } catch (_) {
      result = await getBooks({ title: keyword, pageNum: 1, pageSize: 8 })
    }
    results.value = (result?.records || []).slice(0, 8)
  } catch (cause) {
    results.value = []
    error.value = cause.message
  } finally {
    searching.value = false
  }
}

function selectBook(book) {
  emit('update:modelValue', { ...book, id: book.id || book.bookId })
  query.value = ''
  results.value = []
}
</script>

<template>
  <div class="book-picker"><span>{{ label }}</span>
    <div v-if="modelValue" class="selected-book"><span class="selected-cover"><BookCover :src="modelValue.coverUrl || modelValue.bookCover" :alt="`${modelValue.title} 封面`" /></span><span><strong>{{ modelValue.title }}</strong><small>{{ modelValue.author }}</small></span><button class="icon-button" type="button" aria-label="取消关联图书" @click="emit('update:modelValue', null)"><X :size="15" /></button></div>
    <div v-else class="book-search"><Search :size="17" /><input v-model="query" placeholder="搜索书名或作者" aria-label="搜索关联图书" @input="scheduleSearch" /></div>
    <div v-if="!modelValue && (query || searching)" class="book-search-results">
      <p v-if="searching">正在搜索...</p>
      <template v-else><button v-for="book in results" :key="book.id || book.bookId" type="button" @click="selectBook(book)"><span class="result-cover"><BookCover :src="book.coverUrl || book.bookCover" :alt="`${book.title} 封面`" /></span><span><strong>{{ book.title }}</strong><small>{{ book.author }}</small></span><Check :size="15" /></button><p v-if="query && !results.length">{{ error || '没有找到相关图书' }}</p></template>
    </div>
  </div>
</template>

<style scoped>
.book-picker { position:relative; }.book-picker > span { display:block; margin-bottom:7px; color:#59655c; font-size:14px; }.book-search { display:flex; align-items:center; gap:8px; border:1px solid #cdd7ce; border-radius:5px; padding:0 10px; color:#6a756c; }.book-search:focus-within { border-color:#568669; box-shadow:0 0 0 3px #e2efe1; }.book-search input { width:100%; border:0; padding:10px 0; outline:0; background:#fff; font:inherit; }.book-search-results { position:absolute; z-index:5; top:70px; left:0; right:0; max-height:260px; overflow:auto; border:1px solid #d5ddd5; border-radius:5px; background:#fff; box-shadow:0 12px 30px rgba(30,45,34,.14); }.book-search-results > p { margin:0; padding:12px; color:#748078; font-size:13px; }.book-search-results button { width:100%; display:grid; grid-template-columns:34px minmax(0,1fr) 18px; gap:9px; align-items:center; border:0; border-top:1px solid #edf0ec; padding:9px 10px; background:#fff; text-align:left; }.book-search-results button:hover { background:#f2f6f1; }.book-search-results strong,.book-search-results small,.selected-book strong,.selected-book small { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.book-search-results strong,.selected-book strong { font-size:14px; }.book-search-results small,.selected-book small { margin-top:2px; color:#788279; font-size:12px; }.result-cover,.selected-cover { width:34px; aspect-ratio:2/3; overflow:hidden; border-radius:3px; background:#e2e7e1; }.result-cover :deep(img),.selected-cover :deep(img) { width:100%; height:100%; object-fit:cover; }.selected-book { display:grid; grid-template-columns:34px minmax(0,1fr) 32px; gap:10px; align-items:center; border:1px solid #bcd0bf; border-radius:5px; padding:9px; background:#f2f7f1; }
.book-picker > span { font-size:16px; }
.book-search-results > p { font-size:15px; }
.book-search-results strong, .selected-book strong { font-size:16px; }
.book-search-results small, .selected-book small { font-size:14px; }
</style>
