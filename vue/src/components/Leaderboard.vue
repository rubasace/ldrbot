<script setup>
import {computed, defineProps, onMounted, ref, watch} from 'vue'
import {usePrimeVue} from 'primevue/config'
import Select from 'primevue/select'

const leaderboard = ref(null)
const loading = ref(true)
const fetchFailed = ref(false)

const props = defineProps({
  group: {
    type: Object,
    required: true
  }
})

const viewMode = ref('global')
const gameNames = computed(() => Object.keys(leaderboard.value?.gamesLeaderboard ?? {}).sort((a, b) => a.localeCompare(b)))
const selectedGame = ref('')

watch(viewMode, () => {
  if (viewMode.value === 'game') {
    selectedGame.value = gameNames.value[0] || ''
  }
}, {immediate: true})


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

const calculatePosition = (index) =>
    ['1st', '2nd', '3rd'][index] || `${index + 1}th`

const formatDuration = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return [h ? `${h}h` : '', m ? `${m}m` : '', (!h && !m) || s ? `${s}s` : '']
      .filter(Boolean)
      .join(' ')
}

onMounted(async () => {
  const groupId = props.group.groupId
  try {
    const response = await fetch(`/api/leaderboard/${groupId}`)
    if (!response.ok) return
    leaderboard.value = await response.json()
    selectedGame.value = Object.keys(leaderboard.value.gamesLeaderboard || {})[0]
  } catch (err) {
    fetchFailed.value = true
    console.error(err)
  } finally {
    loading.value = false
  }
})


const rows = computed(() => {
  const source =
      viewMode.value === 'global'
          ? leaderboard.value?.globalLeaderboard
          : leaderboard.value?.gamesLeaderboard[selectedGame.value] || []

  return source.map((entry, index) => ({
    id: entry.userId,
    position: calculatePosition(index),
    name: entry.username ? `@${entry.username}` : entry.firstName,
    points: `${entry.totalPoints} pts`,
    style: getPositionStyle(index),
    totalDuration: formatDuration(entry.totalDuration)
  }))
})
</script>

<!--TODO show arrows indicating position movement  -->
<template>
  <section class="group-leaderboard">
    <div class="controls-float">
      <div class="toggle-container">
        <span class="global" :class="{ active: viewMode === 'global' }" @click="viewMode = 'global'">Global</span>
        <span class="separator">|</span>
        <span class="game" :class="{ active: viewMode === 'game' }" @click="viewMode = 'game'">Game</span>
      </div>

      <Select
          v-if="viewMode === 'game'"
          class="game-select"
          v-model="selectedGame"
          :options="gameNames"
          :placeholder="'Select game'"
      />
    </div>


    <div v-if="loading">Loading leaderboard...</div>
    <div v-else>
      <div
          v-for="(row, index) in rows"
          :key="row.id"
          class="leaderboard-row"
          :class="row.style"
      >
        <span v-if="index < 3" class="medal">{{ ['🥇', '🥈', '🥉'][index] }}</span>
        <div class="user-info">
          <span class="position">{{ row.position }}.</span>
          <img :src="`/api/images/users/${row.id}`" alt="avatar" class="avatar"/>
          <span class="name">{{ row.name }}</span>
        </div>
        <div class="results">
          <span class="points">{{ row.points }}</span>
          <span class="time">{{ row.totalDuration }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="sass">
@use "sass:color"
$chip-base: #a6b7e3
$chip-bg: linear-gradient(135deg, $chip-base, lighten($chip-base, 10%))
$chip-border: 1px solid var(--surface-border)
$chip-radius: 10px

.group-leaderboard
  position: relative
  display: flex
  flex-direction: column
  gap: 1rem

  .controls-float
    display: flex
    justify-content: center
    align-items: center
    flex-wrap: wrap
    font-size: 0.9rem

    .toggle-container
      display: flex
      align-items: center
      background: $chip-bg
      border: $chip-border
      border-radius: $chip-radius


      span
        cursor: pointer
        padding: 0.5rem 0.6rem
        color: black
        transition: background 0.2s ease
        $internal-padding: 0.3rem
        &.global
          padding-right: $internal-padding
        &.game
          padding-left: $internal-padding

        &.active
          //background-color: color.adjust($chip-base, $lightness: -20%)
          border-radius: $chip-radius
          font-weight: 800

      .separator
        opacity: 0.5
        padding: 0

    .game-select
      position: absolute
      right: 0
      ::v-deep(.p-select-label)
        padding: 0.5rem 0 0.5rem 0.5rem

.leaderboard-row
  position: relative
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
  font-size: 1.2em

.points
  font-size: 1.1em
  font-weight: bold

.results
  display: flex
  flex-direction: column
  align-items: end
  justify-content: center
  gap: 10px

.medal
  position: absolute
  bottom: -0.4em
  left: -0.4em
  font-size: 2.3rem
  z-index: 2

.time
  font-size: 0.85em
  font-style: italic
  color: #222

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