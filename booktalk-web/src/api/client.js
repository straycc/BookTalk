const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8081'

export class ApiError extends Error {
  constructor(message, status = 500) {
    super(message)
    this.status = status
  }
}

export async function request(path, options = {}) {
  const token = localStorage.getItem('booktalk.token')
  const headers = new Headers(options.headers || {})
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  const payload = await response.json().catch(() => null)
  if (response.status === 401) {
    localStorage.removeItem('booktalk.token')
    localStorage.removeItem('booktalk.user')
  }
  if (!response.ok || !payload || payload.code !== 200) {
    throw new ApiError(payload?.msg || payload?.message || '请求失败，请稍后重试', response.status)
  }
  return payload.data
}

export function apiUrl(path) {
  return `${API_BASE_URL}${path}`
}
