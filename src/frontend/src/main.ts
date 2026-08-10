import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Vuetify
import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'material-design-icons-iconfont/dist/material-design-icons.css'
import '@mdi/font/css/materialdesignicons.css'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)
app.use(router)

const vuetify = createVuetify({
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: {
      mdi
    }
  },
  components,
  directives,
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        dark: false,
        colors: {
          primary: '#2196F3', // blue
          secondary: '#424242', // grey
          accent: '#82B1FF', // light blue
          error: '#FF5252', // red
          info: '#2196F3', // blue
          success: '#4CAF50', // green
          warning: '#FFC107', // yellow
          background: '#f5f5f5', // grey lighten-3
          contrast: '#757575' // black
        }
      },
      dark: {
        dark: true,
        colors: {
          primary: '#FFFFFF', // white
          secondary: '#424242', // grey
          accent: '#82B1FF', // light blue
          error: '#FF5252', // red
          info: '#2196F3', // blue
          success: '#4CAF50', // green
          warning: '#FFC107', // yellow
          background: '#757575', // grey darken-3
          contrast: '#f5f5f5' // white
        }
      }
    }
  }
})

app.use(vuetify)
app.mount('#app')
