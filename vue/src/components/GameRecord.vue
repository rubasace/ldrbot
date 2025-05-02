<script setup>
defineProps({
  record: Object
})
</script>

<template>
  <div class="game-record-card">
    <div class="card-header" :class="record.game.toLowerCase()+'-gradient'">
      <div class="header-content">
        <div class="game">{{ record.game }}</div>
        <img :src="`/api/images/users/${record.userId}`" alt="User" class="avatar"/>
        <div class="time-chip">{{ record.seconds }}s</div>
        <div class="username">{{ record.username ? `@${record.username}` : record.firstName }}</div>
      </div>
    </div>

    <div class="card-body">
      <div class="date">
        <i class="fas fa-calendar-alt"></i>
        {{ record.date }}
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.game-record-card
  $border-radius: 12px
  flex: 1 1 270px
  height: 360px
  max-width: 270px
  position: relative
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2)
  border-radius: 10px
  overflow: hidden
  display: flex
  flex-direction: column

  .card-header
    position: relative
    height: 80%
    display: flex
    align-items: center
    justify-content: center
    border-top-left-radius: $border-radius
    border-top-right-radius: $border-radius
    overflow: hidden

    .header-content
      position: relative
      display: flex
      flex-direction: column
      align-items: center
      justify-content: center
      text-align: center

      .game
        color: white
        font-size: 1.9rem
        font-weight: bold
        margin-bottom: 1.2rem

      .avatar
        $size: 128px
        width: $size
        height: $size
        border-radius: 50%
        border: 4px solid gold
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2)
        background-color: var(--surface-card)
        object-fit: cover
        z-index: 1

      .time-chip
        margin-top: -1.25rem
        padding: 0.4rem 0.75rem
        background-color: #3c3c4b
        color: white
        width: 55px
        font-size: 1.1rem
        font-weight: bold
        border-radius: 999px
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15)
        z-index: 2
        position: relative

      .username
        color: white
        font-size: 1.5rem
        font-weight: bold
        margin-top: 0.5rem

  .card-body
    display: flex
    height: 20%
    justify-content: center
    align-items: center
    text-align: center
    padding: 1rem
    font-weight: bold
    color: white
    background: linear-gradient(135deg, #3a3f47, #2b2e35)
    border-bottom-left-radius: $border-radius
    border-bottom-right-radius: $border-radius

    .date
      display: flex
      align-items: center
      gap: 0.5rem
      font-size: 1rem

      i
        font-size: 1rem
        color: #ccc
</style>