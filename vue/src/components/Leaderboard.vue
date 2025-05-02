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

const getPositionStyle = (index) => {
  switch (index) {
    case 0:
      return 'first'
    case 1:
      return 'second'
    case 2:
      return 'third'
    default:
      return 'rest'
  }
}

onMounted(async () => {
  const groupId = props.group.chatId
  try {
    const response = await fetch(`/api/leaderboard/${groupId}`)
    if (!response.ok) return
    leaderboard.value = await response.json()
  } catch (err) {
    fetchFailed.value = true
    console.error(err)
  } finally {
    loading.value = false
  }
})

function calculatePosition(index) {
  switch (index) {
    case 0:
      return '1st'
    case 1:
      return '2nd'
    case 2:
      return '3rd'
    default:
      return index + 1 + 'th'
  }
}

const rows = computed(() =>
    leaderboard.value?.globalLeaderboard.map((entry, index) => ({
      id: entry.userId,
      position: calculatePosition(index),
      name: entry.username ? `@${entry.username}` : entry.firstName,
      points: `${entry.totalPoints} pts`,
      style: getPositionStyle(index)
    }))
)
</script>

<template>
  <section class="group-leaderboard">
    <div v-if="loading">Loading leaderboard...</div>
    <div v-else>
      <div
          v-for="row in rows"
          :key="row.id"
          class="leaderboard-row"
          :class="row.style"
      >
        <div class="user-info">
          <span class="position">{{ row.position }}.</span>
          <img :src="`/api/images/users/${row.id}`" alt="avatar" class="avatar"/>
          <span class="name">{{ row.name }}</span>
        </div>
        <span class="points">{{ row.points }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped lang="sass">
.group-leaderboard
  display: flex
  flex-direction: column
  gap: 10rem

.leaderboard-row
  display: flex
  justify-content: space-between
  align-items: center
  padding: 0.75rem 1.25rem
  border-radius: 12px
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1)
  margin-bottom: 1rem
  color: black
  font-weight: 600

.user-info
  display: flex
  align-items: center
  gap: 10px

.avatar
  $size: 44px
  width: $size
  height: $size
  border-radius: 50%
  object-fit: cover
  border: 3px solid rgba(21, 20, 20, 0.8)

.name
  font-size: 1rem

.points
  font-size: 1rem

.first
  background: linear-gradient(135deg, #ffeb85, #f9c700)

  .avatar
    border-color: #956802

.second
  background: linear-gradient(135deg, #f0f0f0, #cfcfcf)

  .avatar
    border-color: #6c6c6c

.third
  background: linear-gradient(135deg, #f2b27a, #d58550)

  .avatar
    border-color: #8f3601

.rest
  background: linear-gradient(135deg, #88aaed, #3770e6)

  .avatar
    border-color: #3770e6

.name, .points
  color: #2b2b2b
</style>