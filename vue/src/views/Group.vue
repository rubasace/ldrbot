<script setup>
import {onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'

const route = useRoute()
const group = ref(null)
const loading = ref(true)
const fetchFailed = ref(false)

onMounted(async () => {
  const groupId = route.params.groupId
  try {
    const response = await fetch(`/api/dashboard/${groupId}`)
    if (!response.ok) throw new Error('Group not found')
    group.value = await response.json()
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

    <div v-if="group?.idontexist" class="group-stats">
      <h2>Stats</h2>
      <div class="content">
        <!--        <Card class="stat-card">-->
        <!--          <template #title>Members</template>-->
        <!--          <template #content>{{ group.members.length }}</template>-->
        <!--        </Card>-->

        <!--        <Card class="stat-card">-->
        <!--          <template #title>Games Tracked</template>-->
        <!--          <template #content>ZIP, TANGO, ZIP, TANGO</template>-->
        <!--        </Card>-->

        <!--        <Card class="stat-card">-->
        <!--          <template #title>Games Tracked</template>-->
        <!--          <template #content>ZIP, TANGO, ZIP, TANGO</template>-->
        <!--        </Card>-->

        <!--      <Card class="stat-card">-->
        <!--        <template #title>Total Scores</template>-->
        <!--        <template #content>{{ stats.totalScores }}</template>-->
        <!--      </Card>-->

        <!--      <Card class="stat-card">-->
        <!--        <template #title>Days Active</template>-->
        <!--        <template #content>{{ stats.daysTracked }}</template>-->
        <!--      </Card>-->
      </div>
    </div>
    <Card v-if="group" class="leaderboard-card">
      <template #title>Leaderboard</template>
      <template #content>
        <Leaderboard :leaderboard="group?.leaderboard?.globalLeaderboard"/>
      </template>
    </Card>

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

  .leaderboard-card
    text-align: center
    max-width: 600px

  .group-stats
    .content
      display: grid
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr))
      gap: 1rem
      margin-top: 2rem

      .stat-card
        text-align: center
        max-width: 300px

        ::v-deep(.p-card-title)
          font-size: 0.9rem
          color: var(--text-color-secondary)

        ::v-deep(.p-card-content)
          font-size: 1.6rem
          font-weight: bold
          color: var(--text-color)
</style>