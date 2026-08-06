<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  Bell, BookOpen, Bookmark, ChevronRight, Compass, Flame, Heart, Home,
  Library, LogOut, MessageCircle, Search, Settings, Sparkles,
  Star, Users, X
} from 'lucide-vue-next'
import {
  addToShelf, getBookDetail, getHotRecommendations, getNotifications,
  getRecommendations, getShelf, getShelfStats, getSquareFeed, getUnreadNotificationCount, getCategories, getBooks,
  login as loginRequest, removeFromShelf, searchBooks as searchBooksRequest,
  updateShelfStatus,
} from './api/booktalk'
import BookDetailPanel from './components/BookDetailPanel.vue'
import BookCover from './components/BookCover.vue'
import CommunityPage from './components/CommunityPage.vue'
import PostDetailPanel from './components/PostDetailPanel.vue'
import ReviewDetailPanel from './components/ReviewDetailPanel.vue'
import SettingsPage from './components/SettingsPage.vue'

const currentView = ref('home')
const query = ref('')
const selectedBook = ref(null)
const selectedReviewId = ref(null)
const selectedPostId = ref(null)
const reviewBackView = ref('book')
const postBackView = ref('community')
const shelfBooks = ref(new Set())
const showNotifications = ref(false)
const showAccountMenu = ref(false)
const activeShelf = ref('all')
const loading = ref(false)
const errorMessage = ref('')
const session = ref(JSON.parse(localStorage.getItem('booktalk.user') || 'null'))
const loginForm = ref({ username: '', password: '' })
const loginError = ref('')
const notifications = ref([])
const unreadCount = ref(0)
const shelfEntries = ref([])
const categories = ref([])
const activeCategoryId = ref(null)
const activeBookSort = ref('default')
const yearlyReadCount = ref(0)

const books = ref([])
const hotBooks = ref([])
const reviews = ref([])

const filteredBooks = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return books.value
  return books.value.filter((book) => [book.title, book.author, book.category, ...book.tags]
    .join(' ').toLowerCase().includes(keyword))
})

const shelfList = computed(() => shelfEntries.value.length
  ? shelfEntries.value.map(normalizeShelfBook)
  : books.value.filter((book) => shelfBooks.value.has(book.id)))

const isAuthenticated = computed(() => Boolean(session.value?.token))

function normalizeBook(book, source = 'book') {
  const averageScore = book.averageScore ?? book.bookInfo?.avgRating
  const metricScore = Number(book.score ?? 0)
  return {
    id: book.id || book.bookId,
    title: book.title || book.bookTitle || 'Untitled',
    author: book.author || 'Unknown author',
    score: Number(averageScore ?? 0).toFixed(1),
    ratings: book.scoreCount ?? book.bookInfo?.ratingCount ?? 0,
    recommendationScore: source === 'recommendation' ? metricScore : null,
    hotScore: source === 'hot' ? metricScore : Number(book.hotScore ?? 0),
    category: typeof book.categoryName === 'string' ? book.categoryName : '',
    categoryName: typeof book.categoryName === 'string' ? book.categoryName : '',
    tags: book.tags?.map((tag) => tag.name || tag) || [],
    reason: book.reason || '热门趋势推荐',
    cover: book.coverUrl || book.bookCover || '',
    coverUrl: book.coverUrl || book.bookCover || '',
    description: book.description || '暂无图书简介。',
  }
}

function normalizeShelfBook(entry) {
  return {
    id: entry.bookId,
    shelfId: entry.id,
    title: entry.bookName,
    author: entry.author,
    cover: entry.bookCover || '',
    status: entry.status,
    statusDesc: entry.statusDesc,
    score: '0.0',
    ratings: 0,
    category: 'BookTalk',
    tags: [],
    description: '',
  }
}

function applyHomeFeed(feed) {
  reviews.value = (feed?.records || []).map((item) => ({
    id: item.id,
    user: item.authorName || 'BookTalk reader',
    initial: (item.authorName || 'R').slice(0, 1),
    type: String(item.type || '').toUpperCase(),
    title: item.title || '',
    book: item.relatedBookName || '',
    relatedBookId: item.relatedBookId,
    time: item.lastActiveTime || '',
    text: item.contentSummary || '',
    likes: item.likeCount || 0,
    comments: item.commentCount || 0,
  }))
}

async function refreshHomeFeed() {
  try {
    applyHomeFeed(await getSquareFeed())
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function loadHome() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [recommendations, hot, feed, notificationData, unread] = await Promise.all([
      getRecommendations(), getHotRecommendations(), getSquareFeed(), getNotifications(), getUnreadNotificationCount(),
    ])
    if (recommendations?.length) books.value = recommendations.map((item) => normalizeBook(item, 'recommendation'))
    hotBooks.value = (hot || []).map((item) => normalizeBook(item, 'hot'))
    applyHomeFeed(feed)
    notifications.value = notificationData?.records || []
    unreadCount.value = unread || 0
    await loadShelf()
  } catch (error) {
    errorMessage.value = error.message
    try {
      const fallback = await getHotRecommendations()
      if (fallback?.length) {
        hotBooks.value = fallback.map((item) => normalizeBook(item, 'hot'))
        books.value = fallback.map((item) => normalizeBook(item, 'hot'))
      }
    } catch (_) {}
  } finally {
    loading.value = false
  }
}

async function loadShelf() {
  const status = activeShelf.value === 'all' ? '' : activeShelf.value
  const shelf = await getShelf(status)
  shelfEntries.value = shelf?.records || []
  shelfBooks.value = new Set(shelfEntries.value.map((item) => item.bookId))
  try {
    const stats = await getShelfStats()
    yearlyReadCount.value = stats?.yearlyReadCount ?? stats?.readCount ?? 0
  } catch (_) {
    if (!status) {
      const currentYear = new Date().getFullYear()
      yearlyReadCount.value = shelfEntries.value.filter((item) => item.status === 'READ'
        && new Date(item.updateTime || item.createTime).getFullYear() === currentYear).length
    }
  }
}

async function loadDiscover() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getBooks({
      categoryId: activeCategoryId.value || undefined,
      sortField: activeBookSort.value === 'score' ? 'averageScore' : undefined,
      sortOrder: activeBookSort.value === 'score' ? 'desc' : undefined,
    })
    books.value = (result?.records || []).map(normalizeBook)
  } catch (error) { errorMessage.value = error.message } finally { loading.value = false }
}

async function selectCategory(categoryId) {
  activeCategoryId.value = categoryId
  query.value = ''
  await loadDiscover()
}

async function selectBookSort(sort) {
  activeBookSort.value = sort
  await loadDiscover()
}

async function changeShelfStatus(entry, status) {
  try {
    await updateShelfStatus(entry.shelfId, status)
    await loadShelf()
  } catch (error) { errorMessage.value = error.message }
}

async function submitLogin() {
  loginError.value = ''
  try {
    const user = await loginRequest(loginForm.value.username, loginForm.value.password)
    session.value = user
    localStorage.setItem('booktalk.token', user.token)
    localStorage.setItem('booktalk.user', JSON.stringify(user))
    await loadHome()
  } catch (error) {
    loginError.value = error.message
  }
}

function logout() {
  localStorage.removeItem('booktalk.token')
  localStorage.removeItem('booktalk.user')
  session.value = null
  showNotifications.value = false
  showAccountMenu.value = false
}

function goTo(view) {
  currentView.value = view
  selectedBook.value = null
  selectedReviewId.value = null
  selectedPostId.value = null
  if (view === 'search' && isAuthenticated.value) loadDiscover()
  if (view === 'shelf' && isAuthenticated.value) loadShelf()
}

function handleProfileUpdated(profile) {
  session.value = {
    ...session.value,
    username: profile.username,
    nickname: profile.nickname,
    avatarUrl: profile.avatarUrl,
  }
  localStorage.setItem('booktalk.user', JSON.stringify(session.value))
}

async function searchBooks(event) {
  const input = event?.currentTarget
  const inputValue = input?.tagName === 'FORM'
    ? input.querySelector('input')?.value
    : input?.value
  const keyword = (inputValue || query.value).trim()

  currentView.value = 'search'
  selectedBook.value = null
  if (!keyword || !isAuthenticated.value) return

  query.value = keyword
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await searchBooksRequest(keyword)
    books.value = (result?.records || []).map(normalizeBook)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function openBook(book) {
  selectedBook.value = book
  selectedReviewId.value = null
  selectedPostId.value = null
  currentView.value = 'book'
  if (!isAuthenticated.value || !book?.id) return
  try {
    const detail = await getBookDetail(book.id)
    selectedBook.value = normalizeBook(detail)
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function openBookById(bookId) {
  await openBook({ id: bookId, title: '加载中...', author: '', tags: [] })
}

function openReview(reviewId, backView = 'book') {
  selectedReviewId.value = reviewId
  selectedPostId.value = null
  reviewBackView.value = backView
  currentView.value = 'review'
}

async function backFromReview() {
  selectedReviewId.value = null
  const targetView = reviewBackView.value === 'book' && selectedBook.value ? 'book' : reviewBackView.value
  currentView.value = targetView
  if (targetView === 'home') await refreshHomeFeed()
}

function openPost(postId, backView = 'community') {
  selectedPostId.value = postId
  selectedReviewId.value = null
  postBackView.value = backView
  currentView.value = 'post'
}

async function backFromPost() {
  selectedPostId.value = null
  currentView.value = postBackView.value
  if (postBackView.value === 'home') await refreshHomeFeed()
}

function openHomeContent(item) {
  if (item.type === 'REVIEW') openReview(item.id, 'home')
  else openPost(item.id, 'home')
}

async function toggleShelf(book) {
  const next = new Set(shelfBooks.value)
  const existing = shelfEntries.value.find((item) => Number(item.bookId) === Number(book.id))
  try {
    if (existing?.id) {
      await removeFromShelf(existing.id)
      shelfEntries.value = shelfEntries.value.filter((item) => item.id !== existing.id)
      next.delete(book.id)
    } else {
      await addToShelf(book.id)
      next.add(book.id)
      await loadShelf()
    }
    shelfBooks.value = next
  } catch (error) {
    errorMessage.value = error.message
  }
}

onMounted(() => {
  getCategories().then((items) => { categories.value = items || [] }).catch(() => {})
  if (isAuthenticated.value) loadHome()
})
</script>

<template>
  <div class="app-shell">
    <div v-if="!isAuthenticated" class="login-overlay">
      <form class="login-panel" @submit.prevent="submitLogin">
        <span class="brand-mark"><BookOpen :size="21" /></span>
        <p class="eyebrow">WELCOME TO BOOKTALK</p>
        <h1>记录每一次阅读</h1>
        <p class="login-copy">登录后查看你的书架、推荐与社区动态。</p>
        <label>用户名<input v-model.trim="loginForm.username" autocomplete="username" placeholder="输入用户名" required /></label>
        <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="输入密码" required /></label>
        <p v-if="loginError" class="form-error">{{ loginError }}</p>
        <button class="primary-button login-submit" type="submit">登录并继续</button>
      </form>
    </div>
    <header class="topbar">
      <button class="brand" aria-label="BookTalk 首页" @click="goTo('home')">
        <span class="brand-mark"><BookOpen :size="20" /></span>
        <span>BookTalk</span>
      </button>

      <form class="global-search" @submit.prevent="searchBooks">
        <Search :size="18" />
        <input v-model="query" placeholder="搜索书名、作者或标签" aria-label="搜索图书" />
      </form>

      <div class="topbar-actions">
        <button class="icon-button" aria-label="通知" @click="showNotifications = !showNotifications">
          <Bell :size="19" />
          <span v-if="unreadCount" class="notification-dot"></span>
        </button>
        <button class="avatar-button" aria-label="账户菜单" @click="showAccountMenu = !showAccountMenu; showNotifications = false">{{ session?.username?.slice(0, 2).toUpperCase() || 'AR' }}</button>
      </div>
    </header>

    <aside class="sidebar">
      <nav aria-label="主导航">
        <button :class="['nav-item', { active: currentView === 'home' }]" @click="goTo('home')"><Home :size="18" />首页</button>
        <button :class="['nav-item', { active: currentView === 'search' }]" @click="goTo('search')"><Compass :size="18" />发现图书</button>
        <button :class="['nav-item', { active: currentView === 'shelf' }]" @click="goTo('shelf')"><Library :size="18" />我的书架</button>
        <button :class="['nav-item', { active: currentView === 'community' || currentView === 'post' }]" @click="goTo('community')"><Users :size="18" />社区</button>
      </nav>
      <div class="sidebar-bottom">
        <button :class="['nav-item', { active: currentView === 'settings' }]" @click="goTo('settings')"><Settings :size="18" />设置</button>
        <div class="reader-summary">
          <span class="summary-number">{{ yearlyReadCount }}</span>
          <span>今年读完</span>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <section v-if="currentView === 'home'" class="page home-page">
        <p v-if="errorMessage" class="data-notice">后端数据暂不可用：{{ errorMessage }}</p>
        <div class="page-heading">
          <div>
            <p class="eyebrow">GOOD EVENING, ALICE</p>
            <h1>接下来读什么？</h1>
          </div>
          <button class="quiet-action" @click="goTo('search')">浏览全部 <ChevronRight :size="16" /></button>
        </div>

        <section class="recommendation-band" aria-labelledby="recommend-title">
          <div class="section-heading">
            <div><h2 id="recommend-title">为你推荐</h2><p>基于你的书架、评分与近期浏览</p></div>
            <Sparkles :size="19" class="section-icon" />
          </div>
          <div class="book-grid">
            <article v-for="book in books.slice(0, 4)" :key="book.id" class="book-tile" @click="openBook(book)">
              <div class="cover-frame"><BookCover :src="book.cover" :alt="`${book.title} 封面`" /></div>
              <div class="book-tile-copy">
                <p class="reason"><Sparkles :size="13" />{{ book.reason }}</p>
                <h3>{{ book.title }}</h3>
                <p class="author">{{ book.author }}</p>
                <p v-if="book.recommendationScore !== null" class="rating"><Sparkles :size="14" />推荐分 {{ book.recommendationScore.toFixed(1) }}</p><p v-else class="rating"><Star :size="14" fill="currentColor" />{{ book.score }} <span>{{ book.ratings }} 人评分</span></p>
              </div>
              <button class="bookmark-button" :aria-label="shelfBooks.has(book.id) ? '移出书架' : '加入书架'" @click.stop="toggleShelf(book)">
                <Bookmark :size="17" :fill="shelfBooks.has(book.id) ? 'currentColor' : 'none'" />
              </button>
            </article>
          </div>
        </section>

        <section class="content-split">
          <div class="activity-panel">
            <div class="section-heading"><div><h2>来自社区</h2><p>正在被讨论的阅读片段</p></div><button class="text-button" @click="goTo('community')">查看全部</button></div>
            <article v-for="review in reviews" :key="`${review.type}-${review.id}`" class="review-row" role="button" tabindex="0" @click="openHomeContent(review)" @keydown.enter="openHomeContent(review)">
              <div class="review-avatar">{{ review.initial }}</div>
              <div class="review-content">
                <p class="review-meta"><strong>{{ review.user }}</strong> 发布了{{ review.type === 'REVIEW' ? '书评' : '讨论' }}<button v-if="review.relatedBookId" @click.stop="openBookById(review.relatedBookId)">《{{ review.book }}》</button><span>{{ review.time }}</span></p>
                <strong v-if="review.title" class="activity-title">{{ review.title }}</strong><p>{{ review.text }}</p>
                <div class="review-actions"><span><Heart :size="15" />{{ review.likes }}</span><span><MessageCircle :size="15" />{{ review.comments }}</span></div>
              </div>
            </article>
          </div>
          <aside class="trend-panel">
            <div class="section-heading"><div><h2>本周热门</h2><p>读者互动最多</p></div><Flame :size="19" class="flame" /></div>
            <ol class="trend-list">
              <li v-for="(book, index) in hotBooks.slice(0, 5)" :key="book.id"><span>{{ String(index + 1).padStart(2, '0') }}</span><button @click="openBook(book)"><strong>{{ book.title }}</strong><small>{{ book.author }}</small></button><em>热度 {{ book.hotScore.toFixed(1) }}</em></li>
            </ol>
          </aside>
        </section>
      </section>

      <section v-else-if="currentView === 'search'" class="page search-page">
        <div class="page-heading"><div><p class="eyebrow">DISCOVER</p><h1>寻找下一本书</h1></div></div>
        <div class="search-panel"><Search :size="20" /><input v-model="query" autofocus placeholder="搜索书名、作者或标签" @keyup.enter="searchBooks" /><button v-if="query" class="icon-button small" aria-label="清空搜索" @click="query = ''"><X :size="17" /></button></div>
        <div class="filter-row"><button :class="['filter', { active: !activeCategoryId }]" @click="selectCategory(null)">全部</button><button v-for="category in categories" :key="category.id" :class="['filter', { active: activeCategoryId === category.id }]" @click="selectCategory(category.id)">{{ category.name }}</button><button :class="['filter', { active: activeBookSort === 'score' }]" @click="selectBookSort(activeBookSort === 'score' ? 'default' : 'score')">评分最高</button></div>
        <p class="result-count">找到 {{ filteredBooks.length }} 本图书</p>
        <div class="search-results">
          <article v-for="book in filteredBooks" :key="book.id" class="search-result" role="button" tabindex="0" @click="openBook(book)" @keydown.enter="openBook(book)">
            <div class="search-cover"><BookCover :src="book.cover" :alt="`${book.title} 封面`" /></div>
            <div><p class="eyebrow">{{ book.category }}</p><h2>{{ book.title }}</h2><p class="author">{{ book.author }}</p><p class="result-description">{{ book.description }}</p><div class="tag-list"><span v-for="tag in book.tags" :key="tag">{{ tag }}</span></div></div>
            <div class="result-actions"><p class="rating"><Star :size="16" fill="currentColor" />{{ book.score }}</p><button class="primary-button result-detail-button" @click.stop="openBook(book)">查看详情</button><button class="secondary-button result-shelf-button" :aria-label="shelfBooks.has(book.id) ? '移出书架' : '加入书架'" @click.stop="toggleShelf(book)"><Bookmark :size="17" :fill="shelfBooks.has(book.id) ? 'currentColor' : 'none'" />{{ shelfBooks.has(book.id) ? '移出书架' : '加入书架' }}</button></div>
          </article>
        </div>
      </section>

      <BookDetailPanel v-else-if="currentView === 'book' && selectedBook" :book="selectedBook" :in-shelf="shelfBooks.has(selectedBook.id)" @back="goTo('search')" @toggle-shelf="toggleShelf" @open-review="openReview" />

      <ReviewDetailPanel v-else-if="currentView === 'review' && selectedReviewId" :review-id="selectedReviewId" :current-user-id="session?.userId" :back-label="reviewBackView === 'community' ? '返回社区' : reviewBackView === 'home' ? '返回首页' : '返回图书'" @back="backFromReview" />

      <PostDetailPanel v-else-if="currentView === 'post' && selectedPostId" :post-id="selectedPostId" :current-user-id="session?.userId" :back-label="postBackView === 'home' ? '返回首页' : '返回社区'" @back="backFromPost" @deleted="backFromPost" @open-book="openBookById" />

      <section v-else-if="currentView === 'shelf'" class="page shelf-page">
        <div class="page-heading"><div><p class="eyebrow">MY LIBRARY</p><h1>我的书架</h1></div></div>
        <div class="shelf-tabs"><button :class="{ active: activeShelf === 'all' }" @click="activeShelf = 'all'; loadShelf()">全部</button><button :class="{ active: activeShelf === 'WANT_TO_READ' }" @click="activeShelf = 'WANT_TO_READ'; loadShelf()">想读</button><button :class="{ active: activeShelf === 'READING' }" @click="activeShelf = 'READING'; loadShelf()">在读</button><button :class="{ active: activeShelf === 'READ' }" @click="activeShelf = 'READ'; loadShelf()">已读</button></div>
        <div class="shelf-grid"><article v-for="book in shelfList" :key="book.shelfId" class="shelf-book" @click="openBook(book)"><div class="shelf-cover"><BookCover :src="book.cover" :alt="`${book.title} 封面`" /></div><p class="shelf-status">{{ book.statusDesc || book.status }}</p><h2>{{ book.title }}</h2><p>{{ book.author }}</p><select :value="book.status" aria-label="阅读状态" @click.stop @change="changeShelfStatus(book, $event.target.value)"><option value="WANT_TO_READ">想读</option><option value="READING">在读</option><option value="READ">已读</option></select></article></div>
      </section>

      <SettingsPage v-else-if="currentView === 'settings'" @profile-updated="handleProfileUpdated" />

      <CommunityPage v-else-if="currentView === 'community'" @open-review="openReview($event, 'community')" @open-post="openPost" @open-book="openBookById" />
    </main>

    <aside v-if="showNotifications" class="notification-popover"><div class="popover-heading"><strong>通知</strong><button class="icon-button small" aria-label="关闭通知" @click="showNotifications = false"><X :size="17" /></button></div><div v-for="notification in notifications.slice(0, 4)" :key="notification.id" class="notification-item"><span class="review-avatar">{{ (notification.senderName || 'B').slice(0, 1) }}</span><p><strong>{{ notification.senderName || 'BookTalk' }}</strong> {{ notification.title || notification.content }}<br /><small>{{ notification.createTime || '刚刚' }}</small></p></div><p v-if="!notifications.length" class="empty-notification">暂无通知</p><button class="text-button full">查看全部通知</button></aside>
    <aside v-if="showAccountMenu" class="account-popover"><div class="popover-heading"><strong>账户</strong><button class="icon-button small" aria-label="关闭账户菜单" @click="showAccountMenu = false"><X :size="17" /></button></div><div class="account-summary"><span class="avatar-button">{{ session?.username?.slice(0, 2).toUpperCase() }}</span><div><strong>{{ session?.nickname || session?.username }}</strong><small>@{{ session?.username }}</small></div></div><button class="logout-button" @click="logout"><LogOut :size="16" />退出登录</button></aside>

    <nav class="mobile-nav" aria-label="移动端导航"><button :class="{ active: currentView === 'home' }" @click="goTo('home')"><Home :size="19" /><span>首页</span></button><button :class="{ active: currentView === 'search' }" @click="goTo('search')"><Search :size="19" /><span>发现</span></button><button :class="{ active: currentView === 'shelf' }" @click="goTo('shelf')"><Library :size="19" /><span>书架</span></button><button :class="{ active: currentView === 'community' || currentView === 'post' }" @click="goTo('community')"><Users :size="19" /><span>社区</span></button></nav>
  </div>
</template>
