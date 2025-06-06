<script setup>

import Leaderboard from "../components/Leaderboard.vue";
import SessionTable from "../components/SessionTable.vue";
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import GameInfo from "../components/GameInfo.vue";
import GameRecord from "../components/GameRecord.vue";
import BaseCard from "../components/BaseCard.vue";
import DatePicker from 'primevue/datepicker'

const route = useRoute()
const group = ref(null)
const stats = ref(null)
const loading = ref(true)
const fetchFailed = ref(false)
const selectedDate = ref(new Date())

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

const isToday = computed(() => {
  const today = new Date()
  return selectedDate.value?.toDateString() === today.toDateString()
})

function adjustDate(offset) {
  const newDate = new Date(selectedDate.value)
  newDate.setDate(newDate.getDate() + offset)
  if (newDate <= new Date()) {
    selectedDate.value = newDate
  }
}

watch(selectedDate, (newDate) => {
  if (!newDate) {
    selectedDate.value = new Date()
  }
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
      <div class="section results-section">
        <div class="results-header">
          <h2>Daily Results</h2>
          <div class="date-nav">
            <span class="date-arrow" @click="adjustDate(-1)">◀</span>
            <DatePicker
                v-model="selectedDate"
                showIcon
                showButtonBar
                dateFormat="yy-mm-dd"
                :maxDate="new Date()"
                class="date-picker"
                :manualInput="false"
                touchUI
            />
            <span :class="isToday ? 'invisible' : ''" class="date-arrow" @click="adjustDate(1)">▶</span>
          </div>
        </div>
        <Leaderboard v-if="selectedDate" :group="group" :date="selectedDate?.toISOString().split('T')[0]"/>
      </div>
      <div class="section sessions-section">
        <h2>Sessions History</h2>
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
    flex-direction: column
    align-items: center
    justify-content: center
    font-size: 2rem
    gap: 1rem
    margin-bottom: 3rem

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

    h2
      font-size: 0.6em

.group-stats
  display: flex
  flex-direction: column
  gap: 1rem 2rem

.section
  display: flex
  flex-direction: column
  gap: 1rem 2rem
  border-top: 1px solid var(--surface-border)

  h2
    font-size: 1.75rem
    margin-bottom: 1rem
    color: var(--primary-color)

.card-section
  position: relative
  display: flex
  gap: 2rem
  overflow-x: auto
  padding-bottom: 0.5rem
  scroll-snap-type: x mandatory
  scrollbar-width: none
  -ms-overflow-style: none

  &::-webkit-scrollbar
    display: none

  .card
    flex: 0 0 auto
    scroll-snap-align: start
    width: 165px
    margin-left: auto
    margin-right: auto

.results-header
  display: flex
  flex-direction: column
  align-items: center
  gap: 1rem

.date-nav
  display: flex
  align-items: center
  gap: 0.5rem

.date-arrow
  background: var(--surface-card)
  border: 1px solid var(--surface-border)
  border-radius: 50%
  font-size: 1.2rem
  width: 2rem
  height: 2rem
  display: flex
  align-items: center
  justify-content: center
  cursor: pointer
  transition: background 0.2s ease

  &:hover
    color: var(--p-primary-color)

@media (min-width: 1024px)
  $max-leaderboard-width: 600px

  .group-stats
    flex-direction: row
    flex-wrap: wrap

  .leaderboard-section
    flex: 1 1 auto
    max-width: $max-leaderboard-width
    order: 1

  .times-section
    flex: 1 1 0
    order: 2
    display: flex
    gap: 1rem
    flex-direction: column

  .results-section
    max-width: $max-leaderboard-width
    flex: 1 1 $max-leaderboard-width
    order: 3
    margin-left: auto
    margin-right: auto
  .sessions-section
    flex: 1 1 auto
    order: 4

</style>