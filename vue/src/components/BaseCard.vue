<script setup>
// No props needed, fully slot-based
</script>

<template>
  <div class="base-card">
    <div class="card-header">
      <slot name="header"/>
    </div>
    <div class="card-body">
      <slot/>
    </div>
  </div>
</template>

<style scoped lang="sass">
$border-radius: 12px

.base-card
  display: flex
  flex-direction: column
  border-radius: $border-radius
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2)
  overflow: hidden
  background-color: var(--surface-card)
  height: 100%
// allow external control, or use a fixed height if needed

.card-header
  flex: 1 1 auto
  display: flex
  flex-direction: column
  align-items: center
  justify-content: center
  padding: 1rem
  border-top-left-radius: $border-radius
  border-top-right-radius: $border-radius
  background: var(--primary-color)

.card-body
  margin-top: auto
  padding: 1rem
  display: flex
  justify-content: center
  align-items: center
  background: linear-gradient(135deg, #3a3f47, #2b2e35)
  border-bottom-left-radius: $border-radius
  border-bottom-right-radius: $border-radius
  color: white
</style>