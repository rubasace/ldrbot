<script setup>
import {useDarkMode} from '../composables/useDarkMode'

const {darkMode, toggleDarkMode} = useDarkMode()
</script>
<template>
  <header class="app-header">
    <div class="container">
      <div class="branding">
        <img :src="darkMode ? '/logo-dark.png' : '/logo.png'" alt="LDRBot Logo" class="logo-image"/>
        <h1 class="logo-text">LDRBot</h1>
      </div>
      <div class="navigation">
        <nav class="nav nav-links">
          <a
              href="https://t.me/LinkedinDailyRankingBot"
              target="_blank"
              rel="noopener noreferrer"
              class="nav-icon-link"
              title="Telegram Bot"
          >
            <i class="fab fa-telegram-plane"/>
          </a>
          <a
              href="https://github.com/rubasace/ldrbot"
              target="_blank"
              rel="noopener noreferrer"
              class="nav-icon-link"
              title="GitHub Repo"
          >
            <i class="fab fa-github"/>
          </a>
        </nav>
        <nav class="nav commands">
          <button
              class="nav-icon-link"
              @click="toggleDarkMode"
              :title="`Switch to ${darkMode ? 'light' : 'dark'} mode`"
          >
            <i :class="darkMode ? 'fas fa-moon' : 'fas fa-sun'"/>
          </button>
        </nav>
      </div>

    </div>
  </header>
</template>


<style lang="sass" scoped>
.app-header
  background-color: var(--p-content-background)
  border-bottom: 1px solid var(--surface-border)
  padding: 1rem 0
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05)
  position: sticky
  top: 0
  z-index: 10

  .container
    margin: 0 auto
    padding: 0 3rem
    display: flex
    justify-content: space-between
    align-items: center

  .branding
    display: flex
    align-items: center
    gap: 0.5rem

    //TODO fix color of logo for dark mode
    .logo-image
      width: 32px
      height: 32px

    .logo-text
      font-size: 1.5rem
      color: var(--text-color)
      font-weight: bold
      margin: 0


  .navigation
    display: flex
    align-items: center
    justify-content: center
    gap: 4rem

    .nav
      display: flex
      align-items: center
      justify-content: center
      gap: 1.4rem

      *
        color: var(--text-color-secondary)
        background: none
        border: none

      i
        font-size: 1.6em

        &:hover
          color: var(--p-primary-color)

//
//.nav-icon-link
//  display: flex
//  align-items: center
//  justify-content: center
//  background: none
//  border: none
//  padding: 0
//  cursor: pointer
//  font-size: 1.25rem
//  color: var(--text-color-secondary)
//  transition: color 0.2s ease
//  outline: none
//
//  i
//    display: block
//    vertical-align: middle
//    font-size: 1.25rem
//
//  &:focus-visible
//    outline: 2px solid var(--primary-color)
//    outline-offset: 2px
</style>