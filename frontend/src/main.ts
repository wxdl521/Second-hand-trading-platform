import { createApp } from 'vue'
import App from '@/App.vue'
import router from '@/router'
import { pinia } from '@/stores/pinia'
import { useAppStore } from '@/stores/app'
import 'element-plus/dist/index.css'
import '@/styles/global.scss'

const app = createApp(App)

app.use(pinia)
const store = useAppStore()
store.hydrate()

app.use(router)
app.mount('#app')
