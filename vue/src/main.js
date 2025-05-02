import {createApp} from 'vue'
import App from './App.vue'
import {router} from './router'

import PrimeVue from 'primevue/config'

import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

import './assets/main.sass'
import Aura from '@primeuix/themes/aura';
import 'primeflex/primeflex.css'
import '@fortawesome/fontawesome-free/css/all.min.css'

const app = createApp(App);

app.use(router)
app.use(PrimeVue, {
    theme: {
        preset: Aura,
        options: {
            darkModeSelector: '.dark-mode'
        }
    }
});

app.component('Button', Button)
app.component('Dialog', Dialog)
app.component('Card', Card)
app.component('DataTable', DataTable)
app.component('Column', Column)

app.mount('#app')
