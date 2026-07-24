import components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

export default function createComponents() {
  return components({
    dts: false,
    resolvers: [
      AntDesignVueResolver({
        importStyle: false
      })
    ]
  })
}
