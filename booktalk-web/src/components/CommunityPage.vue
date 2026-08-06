<script setup>
import { computed, onMounted, ref } from 'vue'
import { BookOpen, Eye, Heart, MessageCircle, PenLine, Send, X } from 'lucide-vue-next'
import BookSearchPicker from './BookSearchPicker.vue'
import ReviewComposer from './ReviewComposer.vue'
import TagPicker from './TagPicker.vue'
import { createPost, getLikeCount, getLikeStatus, getSquareFeed, toggleLike } from '../api/booktalk'

const emit = defineEmits(['open-review', 'open-post', 'open-book'])
const feed = ref([])
const type = ref('all')
const sort = ref('latest')
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const showComposer = ref(false)
const showReviewComposer = ref(false)
const publishing = ref(false)
const selectedRelatedBook = ref(null)
const form = ref({ title: '', content: '', tagIds: [] })
const hasMore = computed(() => feed.value.length < total.value)

function normalizedType(item) {
  return String(item.type || '').toUpperCase()
}

async function enrichLike(item) {
  const targetType = normalizedType(item)
  try {
    const liked = await getLikeStatus(item.id, targetType)
    return { ...item, liked }
  } catch (_) {
    return { ...item, liked: false }
  }
}

async function load(reset = true) {
  if (loading.value) return
  loading.value = true
  error.value = ''
  if (reset) pageNum.value = 1
  try {
    const result = await getSquareFeed({ pageNum: pageNum.value, pageSize: 10, type: type.value, sort: sort.value })
    const records = await Promise.all((result?.records || []).map(enrichLike))
    feed.value = reset ? records : [...feed.value, ...records]
    total.value = result?.total || 0
  } catch (cause) {
    error.value = cause.message
  } finally {
    loading.value = false
  }
}

async function selectType(nextType) {
  type.value = nextType
  await load()
}

async function selectSort(nextSort) {
  sort.value = nextSort
  await load()
}

async function loadMore() {
  pageNum.value += 1
  await load(false)
}

function openItem(item) {
  if (normalizedType(item) === 'REVIEW') emit('open-review', item.id)
  else emit('open-post', item.id)
}

async function likeItem(item) {
  const targetType = normalizedType(item)
  try {
    await toggleLike(item.id, targetType)
    const [liked, count] = await Promise.all([getLikeStatus(item.id, targetType), getLikeCount(item.id, targetType)])
    item.liked = liked
    item.likeCount = count
  } catch (cause) {
    error.value = cause.message
  }
}

async function openComposer() {
  if (type.value === 'review') showReviewComposer.value = true
  else showComposer.value = true
}

async function publish() {
  if (!form.value.title.trim() || !form.value.content.trim()) return
  publishing.value = true
  error.value = ''
  try {
    const postId = await createPost({
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      relatedBookId: selectedRelatedBook.value?.id ? Number(selectedRelatedBook.value.id) : null,
      tagIds: form.value.tagIds,
    })
    form.value = { title: '', content: '', tagIds: [] }
    selectedRelatedBook.value = null
    showComposer.value = false
    await load()
    emit('open-post', postId)
  } catch (cause) {
    error.value = cause.message
  } finally {
    publishing.value = false
  }
}

async function reviewPublished() {
  showReviewComposer.value = false
  type.value = 'review'
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page community-page">
    <div class="page-heading"><div><p class="eyebrow">COMMUNITY</p><h1>阅读中的讨论</h1></div><button class="primary-button" @click="openComposer"><PenLine v-if="type === 'review'" :size="16" /><Send v-else :size="16" />{{ type === 'review' ? '写书评' : '发布帖子' }}</button></div>
    <div class="community-toolbar">
      <div class="community-filter"><button :class="{ active: type === 'all' }" @click="selectType('all')">全部</button><button :class="{ active: type === 'review' }" @click="selectType('review')">书评</button><button :class="{ active: type === 'discussion' }" @click="selectType('discussion')">讨论</button></div>
      <div class="sort-switch" aria-label="排序方式"><button :class="{ active: sort === 'latest' }" @click="selectSort('latest')">最新</button><button :class="{ active: sort === 'hot' }" @click="selectSort('hot')">热门</button></div>
    </div>
    <p v-if="error" class="data-notice">{{ error }}</p>
    <div class="community-feed">
      <article v-for="item in feed" :key="`${item.type}-${item.id}`" class="community-post" role="button" tabindex="0" @click="openItem(item)" @keydown.enter="openItem(item)">
        <div class="review-avatar">{{ (item.authorName || 'R').slice(0, 1) }}</div>
        <div class="community-post-body">
          <p class="review-meta"><strong>{{ item.authorName || 'BookTalk 读者' }}</strong><span>{{ item.lastActiveTime || item.createTime }}</span><em>{{ normalizedType(item) === 'REVIEW' ? '书评' : '讨论' }}</em></p>
          <h2>{{ item.title || (normalizedType(item) === 'REVIEW' ? '读者书评' : '社区讨论') }}</h2>
          <p>{{ item.contentSummary }}</p>
          <button v-if="item.relatedBookId" class="related-book" @click.stop="emit('open-book', item.relatedBookId)"><BookOpen :size="14" />《{{ item.relatedBookName }}》</button>
          <div v-if="item.tags?.length" class="tag-list"><span v-for="tag in item.tags" :key="tag.id">{{ tag.name }}</span></div>
          <div class="review-actions"><button :class="{ active: item.liked }" @click.stop="likeItem(item)"><Heart :size="15" :fill="item.liked ? 'currentColor' : 'none'" />{{ item.likeCount || 0 }}</button><button @click.stop="openItem(item)"><MessageCircle :size="15" />{{ item.commentCount || 0 }}</button><span v-if="normalizedType(item) === 'POST'"><Eye :size="15" />{{ item.viewCount || 0 }}</span></div>
        </div>
      </article>
      <p v-if="!loading && !feed.length" class="empty-notification">这里还没有内容。</p>
      <button v-if="hasMore" class="secondary-button load-more" :disabled="loading" @click="loadMore">{{ loading ? '加载中...' : '加载更多' }}</button>
      <p v-else-if="loading" class="empty-notification">正在加载...</p>
    </div>

    <div v-if="showComposer" class="composer-mask" @click.self="showComposer = false">
      <form class="post-composer" @submit.prevent="publish">
        <button class="icon-button composer-close" type="button" aria-label="关闭" @click="showComposer = false"><X :size="17" /></button>
        <p class="eyebrow">NEW DISCUSSION</p><h2>发布讨论</h2>
        <label>标题<input v-model.trim="form.title" maxlength="100" placeholder="清楚地说明你想讨论什么" required /></label>
        <label>正文<textarea v-model="form.content" rows="8" maxlength="5000" placeholder="写下你的观点或问题" required /></label>
        <BookSearchPicker v-model="selectedRelatedBook" />
        <TagPicker v-model="form.tagIds" :max="5" />
        <div class="composer-actions"><button class="secondary-button" type="button" @click="showComposer = false">取消</button><button class="primary-button" :disabled="publishing">{{ publishing ? '发布中...' : '发布帖子' }}</button></div>
      </form>
    </div>
    <ReviewComposer v-if="showReviewComposer" @close="showReviewComposer = false" @published="reviewPublished" />
  </section>
</template>

<style scoped>
.community-page { max-width:980px; }.community-toolbar { display:flex; justify-content:space-between; align-items:center; gap:18px; border-bottom:1px solid #dfe4dd; }.community-filter { margin:0 0 16px; }.sort-switch { display:flex; border:1px solid #d8dfd8; border-radius:5px; padding:2px; background:#fff; }.sort-switch button { border:0; border-radius:3px; padding:6px 11px; background:transparent; color:#69736b; font-size:13px; }.sort-switch button.active { background:#e5eee5; color:#245f43; font-weight:700; }
.community-post { max-width:none; padding:22px 8px; border-radius:5px; cursor:pointer; }.community-post:hover,.community-post:focus-visible { background:#f6f8f5; outline:none; }.community-post-body { min-width:0; }.review-meta { display:flex; align-items:center; flex-wrap:wrap; gap:8px; }.review-meta span { margin:0; }.review-meta em { border:1px solid #cadbcb; border-radius:4px; padding:2px 5px; color:#337052; font-size:11px; font-style:normal; }.related-book { display:inline-flex; align-items:center; gap:4px; margin-top:10px; border:0; padding:0; background:transparent; color:#286548; font-size:13px; }.tag-list { margin-top:10px; }.review-actions span { display:inline-flex; align-items:center; gap:4px; color:#798178; font-size:13px; }.review-actions button.active { color:#286548; }.load-more { display:flex; margin:20px auto; }
.composer-mask { position:fixed; inset:0; z-index:70; display:grid; place-items:center; overflow:auto; padding:20px; background:rgba(25,35,28,.38); backdrop-filter:blur(2px); }.post-composer { position:relative; width:min(620px,100%); max-height:calc(100vh - 40px); overflow:auto; display:grid; gap:13px; padding:26px; border:1px solid #d9dfd9; border-radius:6px; background:#fff; }.post-composer h2 { margin:0 0 3px; font-size:24px; }.post-composer label { display:grid; gap:6px; color:#59655c; font-size:14px; }.post-composer input,.post-composer textarea { width:100%; border:1px solid #cdd7ce; border-radius:5px; padding:10px; background:#fff; font:inherit; }.post-composer textarea { resize:vertical; }.composer-close { position:absolute; top:10px; right:10px; }.composer-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:5px; }
@media (max-width:720px) { .community-toolbar { align-items:flex-start; }.community-page .page-heading { align-items:center; }.post-composer { padding:22px 18px; }.community-post { padding-left:0; padding-right:0; } }
.sort-switch button { font-size:15px; }
.review-meta em { font-size:13px; }
.related-book, .review-actions span { font-size:15px; }
</style>
