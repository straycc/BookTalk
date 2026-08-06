export const routes = [
  { path: '/', name: 'home' },
  { path: '/search', name: 'search' },
  { path: '/books/:bookId', name: 'book-detail' },
  { path: '/shelf', name: 'shelf', requiresAuth: true },
  { path: '/community', name: 'community' },
  { path: '/posts/:postId', name: 'post-detail' },
  { path: '/me', name: 'profile', requiresAuth: true },
]
