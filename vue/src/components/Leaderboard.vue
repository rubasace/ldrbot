<script setup>
import {computed, defineProps, onMounted, ref} from 'vue'

const leaderboard = ref(null)
const loading = ref(true)
const fetchFailed = ref(false)

const props = defineProps({
  group: {
    type: Object,
    required: true
  }
})

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

function getPosition(index) {
  switch (index) {
    case 0:
      return '🥇'
    case 1:
      return '🥈'
    case 2:
      return '🥉'
    default:
      return index + 1
  }
}

onMounted(async () => {
  const groupId = props.group.chatId
  try {
    const response = await fetch(`/api/leaderboard/${groupId}`)
    if (!response.ok) {
      return
    }
    leaderboard.value = await response.json()
  } catch (err) {
    fetchFailed.value = true
    console.error(err)
  } finally {
    loading.value = false
  }
})


const rows = computed(() =>
    leaderboard.value?.globalLeaderboard.map((entry, index) => ({
      id: entry.userId,
      position: getPosition(index),
      name: entry.username ? ` @${entry.username}` : entry.firstName,
      duration: formatDuration(entry.totalDuration),
      points: entry.totalPoints
    }))
)
</script>

<template>
  <section class="group-leaderboard">
    <h2>Leaderboard</h2>
    <DataTable :value="rows" responsiveLayout="scroll">
      <Column field="position" header="#" style="width: 60px"/>
      <Column header=" " style="width: 50px">
        <template #body="slotProps">
          <img
              :src="`/api/images/users/${slotProps.data.id}`"
              alt="Avatar"
              class="avatar"
          />
        </template>
      </Column>
      <Column field="name" header="Name"/>
      <!--      <Column field="duration" header="Time"/>-->
      <Column field="points" header="Points"/>
    </DataTable>
  </section>
</template>

<style lang="sass" scoped>
.group-leaderboard
  margin-top: 2rem

  h2
    text-align: center
    margin-bottom: 1rem
    color: var(--text-color)

  .avatar
    width: 32px
    height: 32px
    border-radius: 50%
    object-fit: cover
    border: 1px solid var(--surface-border)

</style>