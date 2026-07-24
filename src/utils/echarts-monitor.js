import * as echarts from 'echarts/core'
import { GaugeChart, PieChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  GaugeChart,
  PieChart,
  TooltipComponent,
  CanvasRenderer
])

export default echarts
