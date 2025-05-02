<script setup>
const props = defineProps({
  info: Object
})

</script>

<template>
  <div class="game-info-card">
    <div class="card-header" :class="info.game.toLowerCase() + '-gradient'">
      <div class="header-content">
        <div class="game">{{ info.game }}</div>
        <div class="avatar-container">
          <img :src="`/images/games/${info.game.toLowerCase()}.png`" alt="Game" class="avatar"/>
        </div>
        <div class="time-chip">{{ info.average.toFixed(2) }}s</div>
      </div>
    </div>

    <div class="card-body">
      <div class="games-count">
        <i class="fas fa-chart-line"/>
        {{ info.count }} times recorded
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.game-info-card
  $border-radius: 12px
  flex: 1 1 270px
  max-width: 270px
  height: 360px
  position: relative
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2)
  border-radius: $border-radius
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

      .avatar-container
        padding: 25px
        border-radius: 10%
        background-color: blue
        background: linear-gradient(135deg, #FDEBB9, #FDB803)

        .avatar
          $size: 128px
          width: $size
          height: $size
          border-radius: 10%
          border: 4px solid white
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2)
          background-color: var(--surface-card)
          z-index: 1

      .time-chip
        margin-top: -1.25rem
        padding: 0.4rem 0.75rem
        background-color: #3c3c4b
        color: white
        font-size: 1.1rem
        font-weight: bold
        border-radius: 999px
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15)
        z-index: 2
        position: relative

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

    .games-count
      display: flex
      align-items: center
      gap: 0.5rem
      font-size: 1rem

      i
        font-size: 1rem
        color: #ccc
</style>