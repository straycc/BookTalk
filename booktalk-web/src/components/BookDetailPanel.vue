<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Bookmark, MessageCircle, Star } from 'lucide-vue-next'
import BookCover from './BookCover.vue'
import ReviewComposer from './ReviewComposer.vue'
import { getBookReviews } from '../api/booktalk'

const props = defineProps({ book: { type: Object, required: true }, inShelf: Boolean })
const emit = defineEmits(['back', 'toggle-shelf', 'open-review'])
const tab = ref('reviews')
const reviews = ref([])
const showReviewForm = ref(false)
const error = ref('')
const reviewCount = computed(() => reviews.value.length)

async function loadReviews() {
  error.value = ''
  try {
    const result = await getBookReviews(props.book.id)
    reviews.value = result?.records || []
  } catch (cause) { error.value = cause.message }
}

async function reviewPublished() {
  showReviewForm.value = false
  await loadReviews()
}

watch(() => props.book.id, loadReviews)
onMounted(loadReviews)
</script>

<template>
  <section class="page book-page">
    <button class="back-button" @click="emit('back')"><ArrowLeft :size="16" />返回发现</button>
    <div class="book-hero">
      <div class="detail-cover"><BookCover :src="book.coverUrl || book.cover" :alt="`${book.title} 封面`" /></div>
      <div class="book-detail-copy">
        <p v-if="book.categoryName" class="eyebrow">{{ book.categoryName }}</p>
        <h1>{{ book.title }}</h1><p class="detail-author">{{ book.author || '作者信息暂缺' }}</p>
        <p class="detail-rating"><Star :size="18" fill="currentColor" />{{ Number(book.averageScore ?? book.score ?? 0).toFixed(1) }} <span>{{ book.scoreCount ?? book.ratings ?? 0 }} 人评分</span></p>
        <p class="detail-description">{{ book.description || '暂无图书简介。' }}</p>
        <div class="book-actions"><button class="primary-button" @click="emit('toggle-shelf', book)"><Bookmark :size="17" :fill="inShelf ? 'currentColor' : 'none'" />{{ inShelf ? '移出书架' : '加入书架' }}</button><button class="secondary-button" @click="showReviewForm = true">写书评</button></div>
      </div>
    </div>
    <div class="detail-tabs"><button :class="{ active: tab === 'reviews' }" @click="tab = 'reviews'">书评 {{ reviewCount }}</button><button :class="{ active: tab === 'discussion' }" @click="tab = 'discussion'">讨论</button></div>
    <p v-if="error" class="data-notice">{{ error }}</p>
    <section v-if="tab === 'reviews'" class="review-list">
      <article v-for="review in reviews" :key="review.bookReviewId" class="detail-review" role="button" tabindex="0" @click="emit('open-review', review.bookReviewId)" @keydown.enter="emit('open-review', review.bookReviewId)"><div class="review-avatar">{{ (review.nickName || 'R').slice(0, 1) }}</div><div><p class="review-meta"><strong>{{ review.nickName || 'BookTalk 读者' }}</strong><span>{{ review.createTime }}</span></p><h2 v-if="review.title">{{ review.title }}</h2><p>{{ review.content }}</p><div class="review-actions"><span class="score-action"><Star :size="15" />{{ review.score || '-' }}</span><button @click.stop="emit('open-review', review.bookReviewId)"><MessageCircle :size="15" />{{ review.commentCount || 0 }}</button></div></div></article>
      <p v-if="!reviews.length" class="empty-notification">还没有书评，成为第一个写下感受的人。</p>
    </section>
    <section v-else class="empty-notification">图书讨论将在社区模块接入后展示。</section>
    <ReviewComposer v-if="showReviewForm" :book="book" @close="showReviewForm = false" @published="reviewPublished" />
  </section>
</template>

<style scoped>
.review-list { max-width:760px; }.detail-review { display:grid; grid-template-columns:34px 1fr; gap:12px; border-top:1px solid #dfe4dd; padding:20px 0; }.detail-review h2 { font-size:18px; margin:0 0 8px; }.detail-review p { margin:0; line-height:1.6; }
.detail-review { padding:20px 8px; border-radius:5px; cursor:pointer; transition:background-color .15s ease; }
.detail-review:hover,.detail-review:focus-visible { background:#f6f8f5; outline:none; }
.score-action { display:inline-flex; align-items:center; gap:4px; color:#727c74; font-size:12px; }
.score-action { font-size:14px; }
</style>
