import useDictStore from "@/store/modules/dict"
import { getDicts } from "@/api/system/dict/data"

const tagTypeKey = ["el", "TagType"].join("")
const tagClassKey = ["el", "TagClass"].join("")

export function useDict(...args) {
  const res = ref({})
  return (() => {
    args.forEach((dictType) => {
      res.value[dictType] = []
      const dicts = useDictStore().getDict(dictType)
      if (dicts) {
        res.value[dictType] = dicts
      } else {
        getDicts(dictType).then((resp) => {
          res.value[dictType] = resp.data.map((p) => ({
            label: p.dictLabel,
            value: p.dictValue,
            [tagTypeKey]: p.listClass,
            [tagClassKey]: p.cssClass,
          }))
          useDictStore().setDict(dictType, res.value[dictType])
        })
      }
    })
    return toRefs(res.value)
  })()
}
