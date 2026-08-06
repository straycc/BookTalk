import { request } from './client'

export const login = (username, password) => request('/user/login', {
  method: 'POST',
  body: JSON.stringify({ username, password }),
})

export const getRecommendations = () => request('/user/recommendations/books?limit=8')
export const getHotRecommendations = () => request('/user/recommendations/books/hot?limit=8')
export const searchBooks = (keyword) => request('/user/book/search', {
  method: 'POST',
  body: JSON.stringify({ keyword, pageNum: 1, pageSize: 20 }),
})
export const getCategories = () => request('/user/book/category/list')
export const getBooks = ({ categoryId, title, author, sortField, sortOrder = 'desc', pageNum = 1, pageSize = 20 } = {}) => request('/user/book/page', {
  method: 'POST',
  body: JSON.stringify({ categoryId, title, author, sortField, sortOrder, pageNum, pageSize }),
})
export const getBookDetail = (bookId) => request(`/user/book/detail/${bookId}`)
export const getBookReviews = (bookId, pageNum = 1, pageSize = 10) => request(
  `/user/bookReview/page?bookId=${bookId}&pageNum=${pageNum}&pageSize=${pageSize}&sortField=createTime&sortOrder=desc`,
)
export const publishBookReview = (payload) => request('/user/bookReview/publish', {
  method: 'POST',
  body: JSON.stringify(payload),
})
export const getReviewDetail = (reviewId) => request(`/user/bookReview/detail/${reviewId}`)
export const getReviewComments = (reviewId) => request(`/user/comment/bookReview/${reviewId}`)
export const publishComment = (reviewId, payload) => request(`/user/comment/publish/${reviewId}`, {
  method: 'POST',
  body: JSON.stringify(payload),
})
export const deleteComment = (commentId) => request(`/user/comment/delete/${commentId}`, { method: 'POST' })
export const toggleLike = (targetId, likeTargetType) => request('/user/like/likeOrNot', {
  method: 'POST',
  body: JSON.stringify({ targetId, likeTargetType }),
})
export const getLikeStatus = (targetId, likeTargetType) => request('/user/like/isLiked', {
  method: 'POST',
  body: JSON.stringify({ targetId, likeTargetType }),
})
export const getLikeCount = (targetId, likeTargetType) => request(`/user/like/count/${targetId}`, {
  method: 'POST',
  body: JSON.stringify({ targetId, likeTargetType }),
})
export const getShelf = (status = '') => request('/user/book-shelf/list', {
  method: 'POST',
  body: JSON.stringify({ page: 1, size: 50, status, sortBy: 'CREATE_TIME', sortOrder: 'DESC' }),
})
export const addToShelf = (bookId) => request('/user/book-shelf/add', {
  method: 'POST',
  body: JSON.stringify({ bookId, status: 'WANT_TO_READ' }),
})
export const removeFromShelf = (shelfId) => request(`/user/book-shelf/remove/${shelfId}`, { method: 'DELETE' })
export const updateShelfStatus = (shelfId, status) => request(`/user/book-shelf/status/${shelfId}?status=${status}`, { method: 'PUT' })
export const getSquareFeed = ({ pageNum = 1, pageSize = 10, type = 'all', sort = 'latest', tagId } = {}) => {
  const params = new URLSearchParams({ pageNum, pageSize, type, sort })
  if (tagId) params.set('tagId', tagId)
  return request(`/user/square/feed?${params}`)
}
export const createPost = (payload) => request('/user/posts', { method: 'POST', body: JSON.stringify(payload) })
export const getPostDetail = (postId) => request(`/user/posts/${postId}`)
export const deletePost = (postId) => request(`/user/posts/${postId}`, { method: 'DELETE' })
export const getPostComments = (postId) => request(`/user/comment/post/${postId}`)
export const getHotTags = () => request('/user/tag/hotTag')
export const searchTags = (keyword, limit = 10) => request(`/user/tag/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`)
export const getNotifications = () => request('/user/notifications?pageNum=1&pageSize=8')
export const getUnreadNotificationCount = () => request('/user/notifications/unread/count')
export const getMyProfile = () => request('/user/me')
export const updateMyProfile = (payload) => request('/user/profile/user-info', {
  method: 'PUT',
  body: JSON.stringify(payload),
})
export const changeMyPassword = (payload) => request('/user/profile/password', {
  method: 'PUT',
  body: JSON.stringify(payload),
})
export const getShelfStats = () => request('/user/book-shelf/stats')
