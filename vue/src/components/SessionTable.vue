<script setup>
import {onMounted, ref} from 'vue'

const props = defineProps({
  group: {
    type: Object,
    required: true
  }
})

const sessions = ref([])
const loading = ref(true)

//TODO extract common factor
const formatDuration = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return [
    h ? `${h}h` : '',
    m ? `${m}m` : '',
    (!h && !m) || s ? `${s}s` : ''
  ].filter(Boolean).join(' ')
}

onMounted(async () => {
  try {
    const res = await fetch(`/api/sessions/${props.group.groupId}`)
    if (!res.ok) throw new Error('Failed to fetch sessions')
    sessions.value = await res.json()
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="session-table">
    <DataTable :value="sessions" :loading="loading" responsiveLayout="scroll">
      <Column header="User" sortable sortField="username">
        <template #body="{ data }">
          <div class="user-cell">
            <img :src="`/api/images/users/${data.userId}`" alt="avatar" class="avatar"/>
            <span>{{ data.username ? `@${data.username}` : data.firstName }}</span>
          </div>
        </template>
      </Column>

      <Column field="game" header="Game" sortable/>
      <Column field="date" header="Date" sortable/>

      <Column header="Time" sortable sortField="seconds">
        <template #body="{ data }">
          <span class="time">{{ formatDuration(data.seconds) }}</span>
        </template>
      </Column>
    </DataTable>
  </section>
</template>

<style scoped lang="sass">
.user-cell
  display: flex
  align-items: center
  gap: 0.5rem

.avatar
  width: 32px
  height: 32px
  border-radius: 50%
  object-fit: cover
  border: 2px solid var(--surface-border)

.time
  display: inline-block
  width: 100%
  text-align: right
</style>