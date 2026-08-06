import { computed, ref } from 'vue'

const storedUser = localStorage.getItem('booktalk.user')
const user = ref(storedUser ? JSON.parse(storedUser) : null)

export function useSession() {
  const isAuthenticated = computed(() => Boolean(user.value?.token))

  function saveSession(nextUser) {
    user.value = nextUser
    localStorage.setItem('booktalk.token', nextUser.token)
    localStorage.setItem('booktalk.user', JSON.stringify(nextUser))
  }

  function clearSession() {
    user.value = null
    localStorage.removeItem('booktalk.token')
    localStorage.removeItem('booktalk.user')
  }

  return { user, isAuthenticated, saveSession, clearSession }
}
