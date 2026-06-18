<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  PhArrowLeft,
  PhCheck,
  PhFileArrowUp,
  PhFileHtml,
  PhLayout,
  PhSpinnerGap,
} from '@phosphor-icons/vue'
import BaseModal from './BaseModal.vue'
import { api, errorMessage } from '@/lib/api'
import { useToast } from '@/composables/useToast'
import type { ApiEnvelope, Site } from '@/types'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: []; created: [site: Site] }>()
const toast = useToast()
const step = ref<1 | 2>(1)
const busy = ref(false)
const selectedSource = ref<'template' | 'upload'>('template')
const selectedTemplate = ref('starter')
const file = ref<File | null>(null)
const createdSite = ref<Site | null>(null)
const form = reactive({
  name: '',
  slug: '',
  framework: 'Vue 3',
  description: '',
})

const canContinue = computed(() => form.name.trim() && /^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$/.test(form.slug))

watch(
  () => form.name,
  (name) => {
    if (!form.slug || form.slug.startsWith('site-')) {
      const normalized = name
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '')
      form.slug = normalized.length >= 3 ? normalized : `site-${Math.random().toString(36).slice(2, 7)}`
    }
  },
)

watch(
  () => props.open,
  (open) => {
    if (!open) return
    step.value = 1
    selectedSource.value = 'template'
    selectedTemplate.value = 'starter'
    file.value = null
    createdSite.value = null
    Object.assign(form, { name: '', slug: '', framework: 'Vue 3', description: '' })
  },
)

function chooseFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0] || null
}

async function create() {
  if (!canContinue.value) return
  busy.value = true
  try {
    const response = await api.post<ApiEnvelope<Site>>('/sites', form)
    createdSite.value = response.data.data
    step.value = 2
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    busy.value = false
  }
}

async function publish() {
  if (!createdSite.value) return
  if (selectedSource.value === 'upload' && !file.value) {
    toast.show('请选择 HTML 文件或 ZIP 项目包', 'error')
    return
  }
  busy.value = true
  try {
    if (selectedSource.value === 'template') {
      await api.post(`/sites/${createdSite.value.id}/templates/${selectedTemplate.value}`)
    } else {
      const formData = new FormData()
      formData.append('file', file.value as File)
      await api.post(`/sites/${createdSite.value.id}/deploy`, formData)
    }
    toast.show('项目已创建，正在进行首次发布')
    emit('created', createdSite.value)
    emit('close')
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="step === 1 ? '创建新项目' : '选择首次发布方式'"
    :description="step === 1 ? '填写项目基础信息，稍后可以继续配置域名和构建设置。' : `${createdSite?.name} 已创建，选择一个内容来源即可发布。`"
    wide
    @close="emit('close')"
  >
    <form v-if="step === 1" class="form-grid" @submit.prevent="create">
      <label class="field field--span">
        <span>项目名称</span>
        <input v-model="form.name" autofocus maxlength="80" placeholder="例如：2026 秋季招新" />
      </label>
      <label class="field">
        <span>项目标识</span>
        <div class="input-prefix">
          <span>sites/</span>
          <input v-model="form.slug" maxlength="50" placeholder="recruit-2026" />
        </div>
        <small>仅支持小写字母、数字和连字符，至少 3 位。</small>
      </label>
      <label class="field">
        <span>项目框架</span>
        <select v-model="form.framework">
          <option>Vue 3</option>
          <option>React</option>
          <option>HTML</option>
        </select>
      </label>
      <label class="field field--span">
        <span>项目说明</span>
        <textarea v-model="form.description" rows="3" maxlength="240" placeholder="简单说明这个站点的用途" />
      </label>
    </form>

    <div v-else class="source-picker">
      <button
        class="source-option"
        :class="{ selected: selectedSource === 'template' }"
        @click="selectedSource = 'template'"
      >
        <span class="source-icon"><PhLayout :size="24" /></span>
        <span><strong>从模板创建</strong><small>立即获得一个可编辑的响应式站点</small></span>
        <PhCheck v-if="selectedSource === 'template'" :size="19" weight="bold" />
      </button>
      <button
        class="source-option"
        :class="{ selected: selectedSource === 'upload' }"
        @click="selectedSource = 'upload'"
      >
        <span class="source-icon"><PhFileArrowUp :size="24" /></span>
        <span><strong>上传项目文件</strong><small>支持 HTML 文件或包含构建产物/源码的 ZIP</small></span>
        <PhCheck v-if="selectedSource === 'upload'" :size="19" weight="bold" />
      </button>

      <div v-if="selectedSource === 'template'" class="template-options">
        <button
          class="template-option"
          :class="{ selected: selectedTemplate === 'starter' }"
          @click="selectedTemplate = 'starter'"
        >
          <PhFileHtml :size="22" />
          <strong>极简启动页</strong>
          <span>适合官网、活动页与项目介绍</span>
        </button>
        <button
          class="template-option"
          :class="{ selected: selectedTemplate === 'portfolio' }"
          @click="selectedTemplate = 'portfolio'"
        >
          <PhLayout :size="22" />
          <strong>作品展示页</strong>
          <span>突出社团项目与成员成果</span>
        </button>
      </div>

      <label v-else class="upload-dropzone">
        <PhFileArrowUp :size="30" />
        <strong>{{ file?.name || '选择项目文件' }}</strong>
        <span>HTML / HTM / ZIP，最大 100 MB</span>
        <input type="file" accept=".html,.htm,.zip" @change="chooseFile" />
      </label>
    </div>

    <template #footer>
      <button v-if="step === 2" class="button button--secondary" :disabled="busy" @click="step = 1">
        <PhArrowLeft :size="17" />上一步
      </button>
      <span class="modal-footer__spacer" />
      <button class="button button--secondary" :disabled="busy" @click="emit('close')">取消</button>
      <button
        v-if="step === 1"
        class="button button--primary"
        :disabled="busy || !canContinue"
        @click="create"
      >
        <PhSpinnerGap v-if="busy" class="spin" :size="17" />继续
      </button>
      <button v-else class="button button--primary" :disabled="busy" @click="publish">
        <PhSpinnerGap v-if="busy" class="spin" :size="17" />创建并发布
      </button>
    </template>
  </BaseModal>
</template>

