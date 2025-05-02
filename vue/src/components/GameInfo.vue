<script setup>
import BaseCard from './BaseCard.vue'

defineProps({
  info: Object
})
</script>

<template>
  <BaseCard :class="['game-info-card', info.game.toLowerCase() + '-gradient']">
    <template #header>
      <div class="header-content">
        <div class="game">{{ info.game }}</div>
        <div class="avatar-container">
          <img :src="`/images/games/${info.game.toLowerCase()}.png`" alt="Game" class="avatar"/>
        </div>
        <div class="time-chip">{{ info.average.toFixed(2) }}s</div>
      </div>
    </template>

    <div class="games-count">
      <i class="fas fa-chart-line"/>
      {{ info.count }} sessions
    </div>
  </BaseCard>
</template>

<style scoped lang="sass">
.game-info-card
  display: flex
  flex-direction: column
  border-radius: 12px

.header-content
  display: flex
  flex-direction: column
  align-items: center
  justify-content: center
  text-align: center

  .game
    color: white
    font-size: 1.7em
    font-weight: bold
    margin-bottom: 0.6em

  .avatar-container
    padding: 1.2em
    border-radius: 10%
    background: linear-gradient(135deg, #FDEBB9, #FDB803)

    .avatar
      $size: 4.5em
      width: $size
      height: $size
      border-radius: 10%
      border: 4px solid white
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2)
      background-color: var(--surface-card)
      object-fit: cover

  .time-chip
    margin-top: -1em
    padding: 0.3em 0.75em
    background-color: #3c3c4b
    color: white
    font-size: 1.1em
    font-weight: bold
    border-radius: 999px
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15)
    z-index: 2
    position: relative

.games-count
  display: flex
  align-items: center
  justify-content: center
  gap: 0.6em
  font-size: 1em
  font-weight: bold
  color: white

  i
    font-size: 1em
    color: #ccc
</style>