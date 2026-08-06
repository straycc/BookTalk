<script setup>
import { onMounted, reactive, ref } from 'vue'
import { KeyRound, Save, UserRound } from 'lucide-vue-next'
import { changeMyPassword, getMyProfile, updateMyProfile } from '../api/booktalk'

const emit = defineEmits(['profile-updated'])
const loading = ref(true)
const savingProfile = ref(false)
const savingPassword = ref(false)
const profileMessage = ref('')
const profileError = ref('')
const passwordMessage = ref('')
const passwordError = ref('')
const profile = reactive({
  userId: null, username: '', nickname: '', email: '', phone: '', gender: 'O',
  birthday: '', region: '', signature: '', avatarUrl: '', background: '',
})
const password = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

async function loadProfile() {
  loading.value = true
  profileError.value = ''
  try {
    Object.assign(profile, await getMyProfile())
    profile.gender ||= 'O'
    profile.birthday ||= ''
  } catch (error) {
    profileError.value = error.message
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  profileError.value = ''
  profileMessage.value = ''
  savingProfile.value = true
  try {
    await updateMyProfile({
      userId: profile.userId,
      username: profile.username,
      nickname: profile.nickname,
      email: profile.email,
      phone: profile.phone,
      gender: profile.gender,
      birthday: profile.birthday || null,
      region: profile.region,
      signature: profile.signature,
      avatar: profile.avatarUrl,
      background: profile.background,
    })
    const updated = await getMyProfile()
    Object.assign(profile, updated)
    profileMessage.value = '个人资料已保存'
    emit('profile-updated', updated)
  } catch (error) {
    profileError.value = error.message
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  passwordError.value = ''
  passwordMessage.value = ''
  if (password.newPassword !== password.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  savingPassword.value = true
  try {
    await changeMyPassword({ ...password })
    password.currentPassword = ''
    password.newPassword = ''
    password.confirmPassword = ''
    passwordMessage.value = '密码修改成功'
  } catch (error) {
    passwordError.value = error.message
  } finally {
    savingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <section class="page settings-page">
    <div class="page-heading"><div><p class="eyebrow">ACCOUNT</p><h1>账户设置</h1></div></div>
    <p v-if="loading" class="data-notice">正在加载个人资料...</p>
    <template v-else>
      <form class="settings-section" @submit.prevent="saveProfile">
        <header><UserRound :size="22" /><div><h2>个人资料</h2><p>用于社区、书评和讨论中的公开展示。</p></div></header>
        <div class="settings-grid">
          <label><span>用户名</span><input :value="profile.username" disabled /></label>
          <label><span>昵称</span><input v-model.trim="profile.nickname" maxlength="30" required /></label>
          <label><span>邮箱</span><input v-model.trim="profile.email" type="email" /></label>
          <label><span>手机号</span><input v-model.trim="profile.phone" inputmode="tel" maxlength="11" /></label>
          <label><span>性别</span><select v-model="profile.gender"><option value="M">男</option><option value="F">女</option><option value="O">其他</option></select></label>
          <label><span>生日</span><input v-model="profile.birthday" type="date" /></label>
          <label class="wide"><span>所在地区</span><input v-model.trim="profile.region" maxlength="60" /></label>
          <label class="wide"><span>个性签名</span><textarea v-model.trim="profile.signature" maxlength="200" rows="4"></textarea></label>
        </div>
        <p v-if="profileError" class="form-error">{{ profileError }}</p>
        <p v-if="profileMessage" class="form-success">{{ profileMessage }}</p>
        <div class="settings-actions"><button class="primary-button" type="submit" :disabled="savingProfile"><Save :size="17" />{{ savingProfile ? '保存中...' : '保存资料' }}</button></div>
      </form>

      <form class="settings-section" @submit.prevent="savePassword">
        <header><KeyRound :size="22" /><div><h2>修改密码</h2><p>修改前需要验证当前密码。</p></div></header>
        <div class="settings-grid password-grid">
          <label><span>当前密码</span><input v-model="password.currentPassword" type="password" autocomplete="current-password" required /></label>
          <span></span>
          <label><span>新密码</span><input v-model="password.newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="64" required /></label>
          <label><span>确认新密码</span><input v-model="password.confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="64" required /></label>
        </div>
        <p v-if="passwordError" class="form-error">{{ passwordError }}</p>
        <p v-if="passwordMessage" class="form-success">{{ passwordMessage }}</p>
        <div class="settings-actions"><button class="primary-button" type="submit" :disabled="savingPassword"><KeyRound :size="17" />{{ savingPassword ? '修改中...' : '修改密码' }}</button></div>
      </form>
    </template>
  </section>
</template>

<style scoped>
.settings-page { width:100%; max-width:920px; margin:0 auto; }
.settings-section { padding:28px 0 34px; border-top:1px solid #dfe3e3; }
.settings-section header { display:flex; align-items:flex-start; gap:13px; margin-bottom:24px; color:#087f87; }
.settings-section h2 { margin:0; color:#202326; font-size:22px; }
.settings-section header p { margin:4px 0 0; color:#737b7d; font-size:15px; }
.settings-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:19px 24px; }
.settings-grid label { display:grid; gap:7px; color:#4d5557; font-size:15px; font-weight:650; }
.settings-grid .wide { grid-column:1 / -1; }
.settings-grid input,.settings-grid select,.settings-grid textarea { width:100%; border:1px solid #cfd6d6; border-radius:5px; padding:11px 12px; background:#fff; color:#202326; font:inherit; outline:0; }
.settings-grid input,.settings-grid select { height:46px; }
.settings-grid textarea { resize:vertical; line-height:1.55; }
.settings-grid input:focus,.settings-grid select:focus,.settings-grid textarea:focus { border-color:#118f98; box-shadow:0 0 0 3px #dff3f3; }
.settings-grid input:disabled { color:#8a9294; background:#f3f4f4; }
.settings-actions { display:flex; justify-content:flex-end; margin-top:20px; }
.form-error,.form-success { margin:16px 0 0; font-size:15px; }
.form-error { color:#a53d45; }.form-success { color:#087f87; }
@media (max-width:650px) { .settings-grid { grid-template-columns:1fr; }.settings-grid .wide { grid-column:auto; }.password-grid > span { display:none; }.settings-actions .primary-button { width:100%; } }
</style>
