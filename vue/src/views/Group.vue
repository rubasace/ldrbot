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
    const response = await fetch(`/api/group/${groupId}`)
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

    <!--    <div v-if="group?.idontexist" class="group-stats">-->
    <!--      <h2>Stats</h2>-->
    <!--      <div class="content">-->
    <!--        &lt;!&ndash;        <Card class="stat-card">&ndash;&gt;-->
    <!--        &lt;!&ndash;          <template #title>Members</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;          <template #content>{{ group.members.length }}</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;        </Card>&ndash;&gt;-->

    <!--        &lt;!&ndash;        <Card class="stat-card">&ndash;&gt;-->
    <!--        &lt;!&ndash;          <template #title>Games Tracked</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;          <template #content>ZIP, TANGO, ZIP, TANGO</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;        </Card>&ndash;&gt;-->

    <!--        &lt;!&ndash;        <Card class="stat-card">&ndash;&gt;-->
    <!--        &lt;!&ndash;          <template #title>Games Tracked</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;          <template #content>ZIP, TANGO, ZIP, TANGO</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;        </Card>&ndash;&gt;-->

    <!--        &lt;!&ndash;      <Card class="stat-card">&ndash;&gt;-->
    <!--        &lt;!&ndash;        <template #title>Total Scores</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;        <template #content>{{ stats.totalScores }}</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;      </Card>&ndash;&gt;-->

    <!--        &lt;!&ndash;      <Card class="stat-card">&ndash;&gt;-->
    <!--        &lt;!&ndash;        <template #title>Days Active</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;        <template #content>{{ stats.daysTracked }}</template>&ndash;&gt;-->
    <!--        &lt;!&ndash;      </Card>&ndash;&gt;-->
    <!--      </div>-->
    <!--    </div>-->
    <div v-if="group" class="group-stats">
      <GroupStats :group="group"/>
      <Leaderboard class="leaderboard" :group="group"/>
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