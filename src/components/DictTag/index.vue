<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span
          v-if="isPlainTag(item)"
          :key="item.value"
          :index="index"
          :class="getTagClass(item)"
        >{{ item.label + " " }}</span>
        <a-tag
          v-else
          :key="item.value + ''"
          :index="index"
          :color="resolveTagColor(item)"
          :class="getTagClass(item)"
        >{{ item.label + " " }}</a-tag>
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ handleArray(unmatchArray) }}
    </template>
  </div>
</template>

<script setup>
const unmatchArray = ref([])
const tagTypeKey = ["el", "TagType"].join("")
const tagClassKey = ["el", "TagClass"].join("")

const props = defineProps({
  options: {
    type: Array,
    default: null,
  },
  value: [Number, String, Array],
  showValue: {
    type: Boolean,
    default: true,
  },
  separator: {
    type: String,
    default: ",",
  },
})

const values = computed(() => {
  if (props.value === null || typeof props.value === "undefined" || props.value === "") return []
  return Array.isArray(props.value) ? props.value.map((item) => "" + item) : String(props.value).split(props.separator)
})

const unmatch = computed(() => {
  unmatchArray.value = []
  if (props.value === null || typeof props.value === "undefined" || props.value === "" || !Array.isArray(props.options) || props.options.length === 0) return false
  let hasUnmatch = false
  values.value.forEach((item) => {
    if (!props.options.some((v) => v.value === item)) {
      unmatchArray.value.push(item)
      hasUnmatch = true
    }
  })
  return hasUnmatch
})

function handleArray(array) {
  if (array.length === 0) return ""
  return array.reduce((pre, cur) => `${pre} ${cur}`)
}

function getTagType(item) {
  return item?.[tagTypeKey] || ""
}

function getTagClass(item) {
  return item?.[tagClassKey] || ""
}

function isPlainTag(item) {
  const type = getTagType(item)
  const className = getTagClass(item)
  return (type === "default" || type === "") && (className === "" || className == null)
}

function resolveTagColor(item) {
  if (getTagClass(item)) {
    return undefined
  }
  const colorMap = {
    success: "success",
    info: "default",
    warning: "warning",
    danger: "error",
    primary: "processing",
  }
  return colorMap[getTagType(item)] || undefined
}
</script>

<style scoped>
.ant-tag + .ant-tag {
  margin-left: 10px;
}
</style>
