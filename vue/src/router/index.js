// vue/src/router/index.js
import {createRouter, createWebHistory} from 'vue-router'
import Home from '../views/Home.vue'
import Group from '../views/Group.vue'

const routes = [{
    path: '/', name: 'Home', component: Home
},
    {path: '/groups/:groupId', name: 'Group', component: Group}]

export const router = createRouter({
    history: createWebHistory(),
    routes
})