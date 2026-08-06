<script setup>
import { onMounted, ref } from 'vue'
import { ArrowLeft, Heart, Send } from 'lucide-vue-next'
import CommentThreadItem from './CommentThreadItem.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import { deleteComment, getLikeCount, getLikeStatus, getReviewComments, getReviewDetail, publishComment, toggleLike } from '../api/booktalk'

const props = defineProps({ reviewId: { type: [String, Number], required: true }, currentUserId: { type: [String, Number], default: null }, backLabel: { type: String, default: '返回图书' } })
const emit = defineEmits(['back'])
const review = ref(null)
const comments = ref([])
const draft = ref('')
const replyTarget = ref(null)
const pendingDelete = ref(null)
const deleting = ref(false)
const error = ref('')

async function enrichComment(comment) {
  const [liked, likeCount] = await Promise.all([getLikeStatus(comment.id, 'COMMENT'), getLikeCount(comment.id, 'COMMENT')])
  return { ...comment, liked, likeCount, replies: await Promise.all((comment.replies || []).map(enrichComment)) }
}

async function load() {
  error.value = ''
  try {
    const [detail, rawComments] = await Promise.all([getReviewDetail(props.reviewId), getReviewComments(props.reviewId)])
    const [liked, likeCount] = await Promise.all([getLikeStatus(props.reviewId, 'REVIEW'), getLikeCount(props.reviewId, 'REVIEW')])
    review.value = { ...detail, liked, likeCount }
    comments.value = await Promise.all((rawComments || []).map(enrichComment))
  } catch (cause) { error.value = cause.message }
}

async function submit() {
  if (!draft.value.trim()) return
  try {
    await publishComment(props.reviewId, { rootId: props.reviewId, targetType: 'REVIEW', parentId: replyTarget.value?.id || null, content: draft.value.trim() })
    draft.value = ''
    replyTarget.value = null
    await load()
  } catch (cause) { error.value = cause.message }
}

function requestRemove(comment) {
  pendingDelete.value = comment
}

async function confirmRemove() {
  if (!pendingDelete.value || deleting.value) return
  deleting.value = true
  try {
    await deleteComment(pendingDelete.value.id)
    pendingDelete.value = null
    await load()
  } catch (cause) {
    error.value = cause.message
  } finally {
    deleting.value = false
  }
}

async function likeReview() {
  try { await toggleLike(props.reviewId, 'REVIEW'); await load() } catch (cause) { error.value = cause.message }
}

async function likeComment(comment) {
  try { await toggleLike(comment.id, 'COMMENT'); await load() } catch (cause) { error.value = cause.message }
}

onMounted(load)
</script>

<template>
  <section class="page review-detail-page">
    <button class="back-button" @click="emit('back')"><ArrowLeft :size="16" />{{ backLabel }}</button>
    <p v-if="error" class="data-notice">{{ error }}</p>
    <article v-if="review" class="review-article">
      <p class="eyebrow">READER REVIEW</p>
      <h1>{{ review.title || '读者书评' }}</h1>
      <p class="review-byline">{{ review.nickName || 'BookTalk 读者' }} · {{ review.createTime }} · 评分 {{ review.score || '-' }}/10</p>
      <div class="review-text">{{ review.content }}</div>
      <button :class="['review-like', { active: review.liked }]" @click="likeReview"><Heart :size="16" :fill="review.liked ? 'currentColor' : 'none'" />{{ review.likeCount || 0 }}</button>
    </article>
    <section v-if="review" class="comments-section">
      <h2>评论 {{ review.commentCount || comments.length }}</h2>
      <CommentThreadItem v-for="comment in comments" :key="comment.id" :comment="comment" :current-user-id="currentUserId" @reply="replyTarget = $event" @delete="requestRemove" @like="likeComment" />
      <p v-if="!comments.length" class="empty-notification">还没有评论。</p>
      <form class="comment-form" @submit.prevent="submit"><p v-if="replyTarget">回复 {{ replyTarget.nickName }} <button type="button" @click="replyTarget = null">取消</button></p><div><input v-model="draft" :placeholder="replyTarget ? `回复 ${replyTarget.nickName}` : '写下你的评论'" /><button class="primary-button" aria-label="发送评论"><Send :size="16" />发送</button></div></form>
    </section>
    <ConfirmDialog v-if="pendingDelete" title="删除评论" description="这条评论及其下的所有回复都会被删除，操作无法撤销。" :confirming="deleting" @cancel="pendingDelete = null" @confirm="confirmRemove" />
  </section>
</template>

<style scoped>
.review-detail-page { max-width:840px; }.review-article { padding:8px 0 30px; border-bottom:1px solid #dce2db; }.review-article h1 { margin:0; font-size:30px; }.review-byline { color:#778078; font-size:13px; }.review-text { margin-top:24px; color:#354139; font-size:16px; line-height:1.8; white-space:pre-wrap; }.review-like { display:inline-flex; align-items:center; gap:5px; margin-top:20px; border:0; background:transparent; color:#727c74; }.review-like.active { color:#286548; }.comments-section { margin-top:28px; }.comments-section h2 { font-size:18px; }.comment-form { margin-top:20px; padding-top:16px; border-top:1px solid #dce2db; }.comment-form > p { color:#52645a; font-size:12px; }.comment-form > p button { border:0; background:transparent; color:#286548; }.comment-form > div { display:flex; gap:8px; }.comment-form input { flex:1; min-width:0; border:1px solid #cdd7ce; border-radius:5px; padding:9px 10px; }
.review-byline { font-size:15px; }
.review-text { font-size:18px; }
.comments-section h2 { font-size:21px; }
.comment-form > p { font-size:14px; }
.comment-form input { font-size:16px; }
</style>
