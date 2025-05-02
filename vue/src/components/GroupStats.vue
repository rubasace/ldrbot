<script setup>
import {computed, defineProps, onMounted, ref} from 'vue'

import GameInfo from './GameInfo.vue'
import GameRecord from './GameRecord.vue'


const stats = ref(null)
const loading = ref(true)
const fetchFailed = ref(false)

const props = defineProps({
  group: {
    type: Object,
    required: true
  }
})

const sortedGames = computed(() =>
    Object.keys(stats.value?.recordByGame ?? {}).sort((a, b) => a.localeCompare(b))
)


onMounted(async () => {
  const groupId = props.group.chatId
  try {
    const response = await fetch(`/api/stats/${groupId}`)
    if (!response.ok) throw new Error('Stats not found')
    stats.value = await response.json()
  } catch (err) {
    fetchFailed.value = true
    console.error(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="group-stats">
    <p v-if="loading">Loading stats...</p>
    <p v-else-if="!stats">Stats not available</p>

    <div v-else class="cards-container">

      <h2>Best Times</h2>
      <div class="card-section">
        <GameRecord
            v-for="game in sortedGames"
            :key="game"
            :record="stats.recordByGame[game]"
        />
      </div>

      <h2>Average Times</h2>
      <div class="card-section">
        <GameInfo
            v-for="game in sortedGames"
            :key="game"
            :game="game"
            :info="stats.averagePerGame[game]"
        />
      </div>
    </div>
  </section>
</template>

<style scoped lang="sass">
.group-stats
  margin-top: 2rem

  h2
    text-align: center
    margin-bottom: 1.5rem
    color: var(--text-color)

.cards-container
  display: flex
  flex-direction: column
  gap: 2rem

.card-section
  display: flex
  flex-wrap: wrap
  gap: 1rem
  justify-content: space-around
</style>