<script setup>
import { Search, X } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { getHotTags, searchTags } from '../api/booktalk'

const props = defineProps({ modelValue: { type: Array, default: () => [] }, max: { type: Number, default: 5 } })
const emit = defineEmits(['update:modelValue'])
const query = ref('')
const candidates = ref([])
const selectedTags = ref(new Map())
const loading = ref(false)
const error = ref('')
const selected = computed(() => props.modelValue.map((id) => selectedTags.value.get(id)).filter(Boolean))
let timer

async function loadHot() {
  loading.value = true
  error.value = ''
  try { candidates.value = await getHotTags() || [] } catch (cause) { error.value = cause.message } finally { loading.value = false }
}

function scheduleSearch() {
  clearTimeout(timer)
  timer = setTimeout(runSearch, 250)
}

async function runSearch() {
  const keyword = query.value.trim()
  if (!keyword) return loadHot()
  loading.value = true
  error.value = ''
  try { candidates.value = await searchTags(keyword) || [] } catch (cause) { candidates.value = []; error.value = cause.message } finally { loading.value = false }
}

function toggle(tag) {
  const ids = new Set(props.modelValue)
  if (ids.has(tag.id)) {
    ids.delete(tag.id)
    selectedTags.value.delete(tag.id)
  } else if (ids.size < props.max) {
    ids.add(tag.id)
    selectedTags.value.set(tag.id, tag)
  }
  emit('update:modelValue', [...ids])
}

function remove(tag) {
  toggle(tag)
}

onMounted(loadHot)
</script>

<template>
  <fieldset class="tag-picker"><legend>标签（可选，最多 {{ max }} 个）</legend>
    <div v-if="selected.length" class="selected-tags"><span v-for="tag in selected" :key="tag.id">{{ tag.name }}<button type="button" :aria-label="`移除标签 ${tag.name}`" @click="remove(tag)"><X :size="12" /></button></span></div>
    <div class="tag-search"><Search :size="15" /><input v-model="query" aria-label="搜索标签" placeholder="搜索已有标签" @input="scheduleSearch" /></div>
    <div class="tag-candidates"><button v-for="tag in candidates" :key="tag.id" type="button" :class="{ active: modelValue.includes(tag.id) }" :disabled="!modelValue.includes(tag.id) && modelValue.length >= max" @click="toggle(tag)">{{ tag.name }}</button></div>
    <p v-if="loading" class="tag-notice">正在加载...</p><p v-else-if="error" class="tag-notice">{{ error }} <button type="button" @click="runSearch">重新加载</button></p><p v-else-if="!candidates.length" class="tag-notice">没有找到相关标签</p>
  </fieldset>
</template>

<style scoped>
.tag-picker { margin:0; border:0; padding:0; }.tag-picker legend { margin-bottom:7px; color:#59655c; font-size:14px; }.tag-search { display:flex; align-items:center; gap:7px; border:1px solid #cdd7ce; border-radius:5px; padding:0 9px; color:#6d776f; }.tag-search:focus-within { border-color:#568669; box-shadow:0 0 0 3px #e2efe1; }.tag-search input { width:100%; border:0; padding:9px 0; outline:0; background:#fff; font:inherit; }.tag-candidates,.selected-tags { display:flex; flex-wrap:wrap; gap:6px; margin-top:8px; }.tag-candidates button { border:1px solid #d6ded6; border-radius:4px; padding:6px 9px; background:#fff; color:#627067; font-size:13px; }.tag-candidates button.active { border-color:#9fbea3; background:#e5f0e3; color:#245f43; }.tag-candidates button:disabled { opacity:.45; }.selected-tags span { display:inline-flex; align-items:center; gap:4px; border-radius:4px; padding:5px 7px; background:#e5f0e3; color:#245f43; font-size:13px; }.selected-tags button { display:grid; place-items:center; border:0; padding:0; background:transparent; color:inherit; }.tag-notice { margin:8px 0 0; color:#7a847c; font-size:13px; }.tag-notice button { border:0; padding:0; background:transparent; color:#286548; }
.tag-picker legend { font-size:16px; }
.tag-candidates button, .selected-tags span, .tag-notice { font-size:15px; }
</style>
