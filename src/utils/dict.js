import useDictStore from "@/store/modules/dict";
import { getDicts } from "@/api/system/dict/data";

function attachDictType(list, dictType) {
  if (!Array.isArray(list)) {
    return;
  }
  if (Object.prototype.hasOwnProperty.call(list, "__dictType")) {
    list.__dictType = dictType;
    return;
  }
  Object.defineProperty(list, "__dictType", {
    value: dictType,
    writable: true,
    configurable: true,
  });
}

/**
 * 获取字典数据
 */
export function useDict(...args) {
  const res = ref({});
  return (() => {
    args.forEach((dictType, index) => {
      res.value[dictType] = [];
      const dicts = useDictStore().getDict(dictType);
      if (dicts) {
        attachDictType(dicts, dictType);
        res.value[dictType] = dicts;
      } else {
        getDicts(dictType).then((resp) => {
          const mapped = resp.data.map((p) => ({
            label: p.dictLabel,
            value: p.dictValue,
            elTagType: p.listClass,
            elTagClass: p.cssClass,
          }));
          attachDictType(mapped, dictType);
          res.value[dictType] = mapped;
          useDictStore().setDict(dictType, mapped);
        });
      }
    });
    return toRefs(res.value);
  })();
}
