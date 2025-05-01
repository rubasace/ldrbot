<template>
  <section class="landing">
    <div class="hero">
      <h1 class="title">Welcome to LDRBot</h1>
      <p class="subtitle">Your Telegram companion for tracking and ranking group game scores.</p>
      <router-link to="/dashboard">
        <button class="cta-button">Get Started</button>
      </router-link>
    </div>

    <div class="features">
      <div class="feature">
        <i class="pi pi-chart-line feature-icon"></i>
        <h2 class="feature-title">Real-Time Leaderboards</h2>
        <p class="feature-description">Stay updated with live rankings as your group competes.</p>
      </div>
      <div class="feature">
        <i class="pi pi-users feature-icon"></i>
        <h2 class="feature-title">Group Management</h2>
        <p class="feature-description">Easily manage multiple groups and track their progress.</p>
      </div>
      <div class="feature">
        <i class="pi pi-cog feature-icon"></i>
        <h2 class="feature-title">Customizable Settings</h2>
        <p class="feature-description">Tailor the bot's behavior to fit your group's preferences.</p>
      </div>
    </div>
  </section>
</template>

<script setup>
// No additional logic required for this static landing page
</script>

<style scoped>
.landing {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem 2rem;
  font-family: 'Inter', sans-serif;
  background-color: #f9f9f9;
}

.hero {
  text-align: center;
  margin-bottom: 3rem;
}

.title {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: #2c3e50;
}

.subtitle {
  font-size: 1.25rem;
  color: #7f8c8d;
  margin-bottom: 2rem;
}

.cta-button {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.cta-button:hover {
  background-color: #369870;
}

.features {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 2rem;
}

.feature {
  background-color: white;
  padding: 2rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-width: 300px;
  text-align: center;
}

.feature-icon {
  font-size: 2rem;
  color: #42b983;
  margin-bottom: 1rem;
}

.feature-title {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: #2c3e50;
}

.feature-description {
  font-size: 1rem;
  color: #7f8c8d;
}
</style>