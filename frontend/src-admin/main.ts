import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import App from '@admin/App.vue'
import router from '@admin/router'
import { pinia } from '@admin/stores/pinia'
import 'element-plus/dist/index.css'
import '@admin/styles.css'

const app = createApp(App)

app.use(pinia)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
