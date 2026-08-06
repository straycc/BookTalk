<script setup>
import { Heart, Reply, Trash2 } from 'lucide-vue-next'

defineProps({
  comment: { type: Object, required: true },
  currentUserId: { type: [String, Number], default: null },
})
defineEmits(['reply', 'delete', 'like'])
</script>

<template>
  <article class="thread-comment">
    <div class="review-avatar">{{ (comment.nickName || 'R').slice(0, 1) }}</div>
    <div class="thread-body">
      <p class="thread-meta"><strong>{{ comment.nickName || 'BookTalk 读者' }}</strong><span>{{ comment.createTime }}</span></p>
      <p class="thread-content">{{ comment.content }}</p>
      <div class="thread-actions">
        <button :class="{ active: comment.liked }" @click="$emit('like', comment)"><Heart :size="14" :fill="comment.liked ? 'currentColor' : 'none'" />{{ comment.likeCount || 0 }}</button>
        <button @click="$emit('reply', comment)"><Reply :size="14" />回复</button>
        <button v-if="String(comment.userId) === String(currentUserId)" @click="$emit('delete', comment)"><Trash2 :size="14" />删除</button>
      </div>
      <div v-if="comment.replies?.length" class="thread-replies">
        <CommentThreadItem v-for="replyItem in comment.replies" :key="replyItem.id" :comment="replyItem" :current-user-id="currentUserId" @reply="$emit('reply', $event)" @delete="$emit('delete', $event)" @like="$emit('like', $event)" />
      </div>
    </div>
  </article>
</template>

<style scoped>
.thread-comment { display:grid; grid-template-columns:34px minmax(0,1fr); gap:11px; padding:16px 0; border-top:1px solid #e1e6e0; }.thread-meta { display:flex; gap:8px; align-items:center; margin:0 0 6px; font-size:13px; }.thread-meta span { color:#8b948d; }.thread-content { margin:0; color:#414b44; font-size:15px; line-height:1.65; }.thread-actions { display:flex; gap:15px; margin-top:9px; }.thread-actions button { display:inline-flex; align-items:center; gap:4px; border:0; padding:0; background:transparent; color:#717c73; font-size:13px; }.thread-actions button:hover,.thread-actions button.active { color:#286548; }.thread-replies { margin:11px 0 0 4px; padding-left:15px; border-left:2px solid #d7e4d7; }
.thread-meta, .thread-actions button { font-size:14px; }
.thread-content { font-size:17px; }
</style>
