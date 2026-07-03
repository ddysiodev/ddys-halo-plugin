import { definePlugin } from '@halo-dev/console-shared'
import { IconPlug } from '@halo-dev/components'
import { markRaw } from 'vue'
import HomeView from './views/HomeView.vue'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/ddys-open',
        name: 'DDYSOpen',
        component: HomeView,
        meta: {
          title: 'DDYS Open',
          searchable: true,
          permissions: ['plugin:ddys-open:view'],
          menu: {
            name: 'DDYS Open',
            group: '内容',
            icon: markRaw(IconPlug),
            priority: 20,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})

