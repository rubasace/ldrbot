<script setup>

import Leaderboard from "../components/Leaderboard.vue";
import {computed, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import GameInfo from "../components/GameInfo.vue";
import GameRecord from "../components/GameRecord.vue";

const route = useRoute()
const group = ref(null)
const stats = ref(null)
const loading = ref(true)
const fetchFailed = ref(false)

const sortedGames = computed(() =>
    Object.keys(stats.value?.recordByGame ?? {}).sort((a, b) => a.localeCompare(b))
)


onMounted(async () => {
  const groupId = route.params.groupId
  try {
    const groupResponse = await fetch(`/api/group/${groupId}`)
    if (!groupResponse.ok) throw new Error('Group not found')
    group.value = await groupResponse.json()
    const statsResponse = await fetch(`/api/stats/${groupId}`)
    if (!statsResponse.ok) throw new Error('Stats not found')
    stats.value = await statsResponse.json()
  } catch (err) {
    fetchFailed.value = true
    console.error(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="group-page">
    <div class="group-header">
      <p v-if="loading">Loading group data...</p>
      <h1 v-else-if="!group">Group not found</h1>
      <h1 v-else>{{ group.title }}</h1>
    </div>
    <div v-if="group" class="group-stats">
      <div class="section">
        <h2>Leaderboard</h2>
        <Leaderboard class="leaderboard" :group="group"/>
      </div>
      <div class="section">
        <h2>Best Times</h2>
        <div class="card-section">
          <GameRecord
              v-for="game in sortedGames"
              :key="game"
              :record="stats.recordByGame[game]"
          />
        </div>
      </div>
      <div class="section">
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
    </div>

  </div>
</template>

<style lang="sass" scoped>
.group-page
  margin: 2rem auto
  padding: 1rem 2rem
  text-align: center
  max-width: 1200px

  p
    color: var(--text-color-secondary)

  .group-header
    display: flex
    align-items: center
    justify-content: center
    font-size: 2rem
    gap: 2rem
    margin-bottom: 2rem

    img
      $size: 2.5em
      width: $size
      height: $size
      object-fit: cover
      border-radius: 50%
      border: 2px solid var(--surface-border)

    h1
      color: var(--text-color)
      margin: 0


.section
  display: flex
  flex-direction: column
  gap: 2rem
  padding: 2rem 0
  border-top: 1px solid var(--surface-border)

  h2
    font-size: 1.75rem
    margin-bottom: 1rem
    color: var(--primary-color)

  .card-section
    display: flex
    flex-wrap: wrap
    gap: 1rem
    justify-content: space-around

.card:hover
  transform: scale(1.1) translateY(-4px)
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2)
  transition: transform 0.2s ease, box-shadow 0.2s ease

</style>