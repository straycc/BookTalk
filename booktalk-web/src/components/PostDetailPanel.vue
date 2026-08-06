<script setup>
import { ArrowLeft, BookOpen, Eye, Heart, Send, Trash2 } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import CommentThreadItem from './CommentThreadItem.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import { deleteComment, deletePost, getLikeCount, getLikeStatus, getPostComments, getPostDetail, publishComment, toggleLike } from '../api/booktalk'

const props = defineProps({ postId: { type: [String, Number], required: true }, currentUserId: { type: [String, Number], default: null }, backLabel: { type: String, default: '返回社区' } })
const emit = defineEmits(['back', 'deleted', 'open-book'])
const post = ref(null)
const comments = ref([])
const draft = ref('')
const replyTarget = ref(null)
const pendingComment = ref(null)
const confirmPostDelete = ref(false)
const deleting = ref(false)
const error = ref('')
const commentCount = computed(() => countComments(comments.value))

function countComments(items) {
  return (items || []).reduce((total, item) => total + 1 + countComments(item.replies), 0)
}

async function enrichComment(comment) {
  const [liked, likeCount] = await Promise.all([getLikeStatus(comment.id, 'COMMENT'), getLikeCount(comment.id, 'COMMENT')])
  return { ...comment, liked, likeCount, replies: await Promise.all((comment.replies || []).map(enrichComment)) }
}

async function load() {
  error.value = ''
  try {
    const [detail, rawComments, liked, likeCount] = await Promise.all([
      getPostDetail(props.postId), getPostComments(props.postId),
      getLikeStatus(props.postId, 'POST'), getLikeCount(props.postId, 'POST'),
    ])
    post.value = { ...detail, liked, likeCount }
    comments.value = await Promise.all((rawComments || []).map(enrichComment))
  } catch (cause) {
    error.value = cause.message
  }
}

async function loadComments() {
  const rawComments = await getPostComments(props.postId)
  comments.value = await Promise.all((rawComments || []).map(enrichComment))
}

async function submit() {
  if (!draft.value.trim()) return
  try {
    await publishComment(props.postId, { rootId: props.postId, targetType: 'POST', parentId: replyTarget.value?.id || null, content: draft.value.trim() })
    draft.value = ''
    replyTarget.value = null
    await loadComments()
  } catch (cause) { error.value = cause.message }
}

async function likePost() {
  try {
    await toggleLike(props.postId, 'POST')
    const [liked, likeCount] = await Promise.all([getLikeStatus(props.postId, 'POST'), getLikeCount(props.postId, 'POST')])
    post.value = { ...post.value, liked, likeCount }
  } catch (cause) { error.value = cause.message }
}

async function likeComment(comment) {
  try {
    await toggleLike(comment.id, 'COMMENT')
    const [liked, likeCount] = await Promise.all([getLikeStatus(comment.id, 'COMMENT'), getLikeCount(comment.id, 'COMMENT')])
    comment.liked = liked
    comment.likeCount = likeCount
  } catch (cause) { error.value = cause.message }
}

async function removeComment() {
  if (!pendingComment.value || deleting.value) return
  deleting.value = true
  try {
    await deleteComment(pendingComment.value.id)
    pendingComment.value = null
    await loadComments()
  } catch (cause) { error.value = cause.message } finally { deleting.value = false }
}

async function removePost() {
  if (deleting.value) return
  deleting.value = true
  try {
    await deletePost(props.postId)
    confirmPostDelete.value = false
    emit('deleted')
  } catch (cause) { error.value = cause.message } finally { deleting.value = false }
}

onMounted(load)
</script>

<template>
  <section class="page post-detail-page">
    <button class="back-button" @click="emit('back')"><ArrowLeft :size="16" />{{ backLabel }}</button>
    <p v-if="error" class="data-notice">{{ error }}</p>
    <article v-if="post" class="post-article">
      <div class="post-heading"><div><p class="eyebrow">COMMUNITY DISCUSSION</p><h1>{{ post.title }}</h1></div><button v-if="String(post.userId) === String(currentUserId)" class="delete-post" @click="confirmPostDelete = true"><Trash2 :size="15" />删除帖子</button></div>
      <p class="post-byline">{{ post.authorName || 'BookTalk 读者' }} · {{ post.createTime }}</p>
      <button v-if="post.relatedBookId" class="related-book" @click="emit('open-book', post.relatedBookId)"><BookOpen :size="15" />《{{ post.relatedBookName }}》</button>
      <div v-if="post.tags?.length" class="tag-list"><span v-for="tag in post.tags" :key="tag.id">{{ tag.name }}</span></div>
      <div class="post-text">{{ post.content }}</div>
      <div class="post-stats"><button :class="{ active: post.liked }" @click="likePost"><Heart :size="16" :fill="post.liked ? 'currentColor' : 'none'" />{{ post.likeCount || 0 }}</button><span><Eye :size="16" />{{ post.viewCount || 0 }}</span></div>
    </article>
    <section v-if="post" class="comments-section">
      <h2>评论 {{ commentCount }}</h2>
      <CommentThreadItem v-for="comment in comments" :key="comment.id" :comment="comment" :current-user-id="currentUserId" @reply="replyTarget = $event" @delete="pendingComment = $event" @like="likeComment" />
      <p v-if="!comments.length" class="empty-notification">还没有评论。</p>
      <form class="comment-form" @submit.prevent="submit"><p v-if="replyTarget">回复 {{ replyTarget.nickName }} <button type="button" @click="replyTarget = null">取消</button></p><div><input v-model="draft" :placeholder="replyTarget ? `回复 ${replyTarget.nickName}` : '写下你的评论'" /><button class="primary-button" aria-label="发送评论"><Send :size="16" />发送</button></div></form>
    </section>
    <ConfirmDialog v-if="pendingComment" title="删除评论" description="这条评论及其下的所有回复都会被删除，操作无法撤销。" :confirming="deleting" @cancel="pendingComment = null" @confirm="removeComment" />
    <ConfirmDialog v-if="confirmPostDelete" title="删除帖子" description="帖子、所有评论和相关点赞都会被删除，操作无法撤销。" :confirming="deleting" @cancel="confirmPostDelete = false" @confirm="removePost" />
  </section>
</template>

<style scoped>
.post-detail-page { max-width:840px; }.post-article { padding:8px 0 30px; border-bottom:1px solid #dce2db; }.post-heading { display:flex; justify-content:space-between; align-items:flex-start; gap:18px; }.post-heading h1 { margin:0; font-size:30px; line-height:1.25; }.post-byline { color:#778078; font-size:13px; }.post-text { margin-top:24px; color:#354139; font-size:16px; line-height:1.8; white-space:pre-wrap; }.related-book { display:inline-flex; align-items:center; gap:5px; border:0; padding:0; background:transparent; color:#286548; font-size:13px; }.tag-list { margin-top:12px; }.post-stats { display:flex; gap:18px; margin-top:22px; }.post-stats button,.post-stats span { display:inline-flex; align-items:center; gap:5px; border:0; padding:0; background:transparent; color:#727c74; font-size:13px; }.post-stats button.active { color:#286548; }.delete-post { display:inline-flex; align-items:center; gap:5px; border:0; padding:5px 0; background:transparent; color:#9b453c; font-size:12px; white-space:nowrap; }.comments-section { margin-top:28px; }.comments-section h2 { font-size:18px; }.comment-form { margin-top:20px; padding-top:16px; border-top:1px solid #dce2db; }.comment-form > p { color:#52645a; font-size:12px; }.comment-form > p button { border:0; background:transparent; color:#286548; }.comment-form > div { display:flex; gap:8px; }.comment-form input { flex:1; min-width:0; border:1px solid #cdd7ce; border-radius:5px; padding:9px 10px; }
@media (max-width:520px) { .post-heading { display:block; }.delete-post { margin-top:10px; }.post-heading h1 { font-size:25px; } }
.post-byline, .related-book, .post-stats button, .post-stats span { font-size:15px; }
.post-text { font-size:18px; }
.delete-post, .comment-form > p { font-size:14px; }
.comments-section h2 { font-size:21px; }
.comment-form input { font-size:16px; }
</style>
