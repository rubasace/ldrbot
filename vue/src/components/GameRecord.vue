<script setup>
import BaseCard from './BaseCard.vue'

defineProps({
  record: Object
})
</script>

<template>
  <BaseCard :class="['game-record-card', record.game.toLowerCase() + '-gradient']">
    <template #header>
      <div class="header-content">
        <div class="game">{{ record.game }}</div>
        <img :src="`/api/images/users/${record.userId}`" alt="User" class="avatar"/>
        <div class="time-chip">{{ record.seconds }}s</div>
        <div class="username">{{ record.username ? `@${record.username}` : record.firstName }}</div>
      </div>
    </template>

    <div class="date">
      <i class="fas fa-calendar-alt"></i>
      {{ record.date }}
    </div>
  </BaseCard>
</template>

<style scoped lang="sass">
.game-record-card
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

  .avatar
    $size: 120px
    width: $size
    height: $size
    border-radius: 50%
    border: 4px solid gold
    box-shadow: 4px 4px 8px rgba(0, 0, 0, 0.2)
    background-color: var(--surface-card)
    object-fit: cover
    z-index: 1

  .time-chip
    margin-top: -1em
    padding: 0.3em 0.75em
    background-color: #3c3c4b
    color: white
    width: 55px
    font-size: 1.1em
    font-weight: bold
    border-radius: 999px
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15)
    z-index: 2
    position: relative

  .username
    color: white
    font-size: 1.2em
    font-weight: bold
    margin-top: 0.5em

.date
  display: flex
  align-items: center
  justify-content: center
  gap: 0.7em
  font-size: 1em
  font-weight: bold
  color: white

  i
    font-size: 1em
    color: #ccc
</style>