<script setup>

import Leaderboard from "../components/Leaderboard.vue";
import SessionTable from "../components/SessionTable.vue";
import {computed, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import GameInfo from "../components/GameInfo.vue";
import GameRecord from "../components/GameRecord.vue";
import BaseCard from "../components/BaseCard.vue";

const route = useRoute()
const group = ref(null)
const stats = ref(null)
const loading = ref(true)
const fetchFailed = ref(false)

const sortedRecords = computed(() => {
  if (!stats.value?.recordByGame) return []
  return Object.entries(stats.value.recordByGame)
      .sort(([, a], [, b]) => a.seconds - b.seconds)
      .map(([game]) => game)
})

const sortedAverages = computed(() => {
  if (!stats.value?.averagePerGame) return []
  return Object.entries(stats.value.averagePerGame)
      .sort(([, a], [, b]) => a.average - b.average)
      .map(([game]) => game)
})


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
      <div class="section leaderboard-section">
        <h2>Leaderboard</h2>
        <Leaderboard class="leaderboard" :group="group"/>
      </div>
      <div class="times-section">
        <div class="section">
          <h2>Best Times</h2>
          <div class="card-section">
            <GameRecord
                class="card"
                v-for="game in sortedRecords"
                :key="game"
                :record="stats.recordByGame[game]"
            />
          </div>
        </div>
        <div class="section">
          <h2>Average Times</h2>
          <div class="card-section">
            <GameInfo
                class="card"
                v-for="game in sortedAverages"
                :key="game"
                :game="game"
                :info="stats.averagePerGame[game]"
            />
          </div>
        </div>
      </div>
      <div class="section sessions-section">
        <h2>Sessions</h2>
        <SessionTable v-if="group" :group="group"/>
      </div>
    </div>


  </div>
</template>

<style lang="sass" scoped>
.group-page
  margin: 2rem auto
  padding: 1rem 2rem
  text-align: center
  max-width: 1400px

  p
    color: var(--text-color-secondary)

  .group-header
    display: flex
    align-items: center
    justify-content: center
    font-size: 2rem
    gap: 1rem

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

.group-stats
  display: flex
  flex-direction: column
  gap: 1rem

.section
  display: flex
  flex-direction: column
  gap: 1rem
  border-top: 1px solid var(--surface-border)
  font-size: 0.85em

  h2
    font-size: 1.75rem
    margin-bottom: 1rem
    color: var(--primary-color)

  .card-section
    display: flex
    flex-wrap: wrap
    flex-direction: row
    gap: 1rem
    justify-content: space-around

    .card
      width: 145px
      font-size: 0.9em

.sessions-section
  font-size: 0.6em
.card:hover
  transform: scale(1.1) translateY(-4px)
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2)
  transition: transform 0.2s ease, box-shadow 0.2s ease


@media (min-width: 600px)
  .sessions-section
    font-size: 1em

@media (min-width: 1024px)
  .group-stats
    flex-direction: row
    flex-wrap: wrap

  .section
    font-size: 1em

  .leaderboard-section
    flex: 1 1 auto
    max-width: 600px
    order: 1

  .times-section
    flex: 1 1 0
    order: 2
    display: flex
    gap: 1rem
    flex-direction: column

    .card-section
      .card
        width: 165px
        font-size: 1em

  .sessions-section
    flex: 0 0 100%
    order: 3

</style>