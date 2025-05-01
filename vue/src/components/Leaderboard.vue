<script setup>
import {computed, defineProps} from 'vue'

const props = defineProps({
  leaderboard: {
    type: Array,
    required: true
  }
})

const formatDuration = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60

  return [
    h ? `${h}h` : '',
    m ? `${m}m` : '',
    (!h && !m) || s ? `${s}s` : ''
  ].filter(Boolean).join(' ')
}


const rows = computed(() =>
    props.leaderboard.map((entry, index) => ({
      id: entry.userId,
      position: index + 1,
      fullName: entry.firstName + ' ' + entry.lastName + (entry.userName ? ` (@${entry.userName})` : ''),
      duration: formatDuration(entry.totalDuration),
      points: entry.totalPoints
    }))
)
</script>

<template>
  <section class="group-leaderboard">
    <DataTable :value="rows" responsiveLayout="scroll">
      <Column field="position" header="#" style="width: 60px"/>
      <Column header=" " style="width: 50px">
        <template #body="slotProps">
          <img
              :src="`/api/images/users/${slotProps.data.id}`"
              alt="Avatar"
              class="avatar"
          />
        </template>
      </Column>
      <Column field="fullName" header="Name"/>
      <Column field="duration" header="Time"/>
      <Column field="points" header="Points"/>
    </DataTable>
  </section>
</template>

<style lang="sass" scoped>
.group-leaderboard
  margin-top: 2rem

  h2
    text-align: center
    margin-bottom: 1rem
    color: var(--text-color)

  .avatar
    width: 32px
    height: 32px
    border-radius: 50%
    object-fit: cover
    border: 1px solid var(--surface-border)
</style>