// 你原来的 utils/ruoyi.js 里，替换 parseTime
import { getActiveTimeZone, isValidIanaTZ } from "@/utils/timezone-helper";

export function parseTime(time, pattern, tzOverride) {
  if (arguments.length === 0 || time == null || time === "") return null;

  const fmt = pattern || "{y}-{m}-{d} {h}:{i}:{s}";

  // —— 关键：确定要用的时区 ——
  let tz = getActiveTimeZone();
  if (isValidIanaTZ(tzOverride)) tz = tzOverride; // 第三个参数优先生效

  const date = toDateUTC(time); // 把输入当“UTC瞬间”

  // 用 Intl 按指定时区取各个部分
  const dtf = new Intl.DateTimeFormat("zh-CN", {
    timeZone: tz,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });
  const parts = Object.fromEntries(
    dtf.formatToParts(date).map((p) => [p.type, p.value])
  );

  const dowEn = new Intl.DateTimeFormat("en-US", {
    timeZone: tz,
    weekday: "short",
  }).format(date);
  const dowMap = {
    Sun: "日",
    Mon: "一",
    Tue: "二",
    Wed: "三",
    Thu: "四",
    Fri: "五",
    Sat: "六",
  };

  const map = {
    y: parts.year,
    m: parts.month,
    d: parts.day,
    h: parts.hour,
    i: parts.minute,
    s: parts.second,
    a: dowMap[dowEn],
  };

  return fmt.replace(/{(y|m|d|h|i|s|a)+}/g, (all, key) => {
    let val = map[key];
    if (val == null) return "";
    if (all.length > 0 && key !== "a" && String(val).length < 2)
      val = "0" + val;
    return val;
  });
}

// 把各种输入转成“对应 UTC 瞬间”的 Date
function toDateUTC(input) {
  if (input instanceof Date) return input;

  if (typeof input === "number") {
    const n = String(input).length === 10 ? input * 1000 : input;
    return new Date(n); // 当作 UTC epoch 毫秒
  }

  if (typeof input === "string") {
    const s = input.trim();
    if (/^\d+$/.test(s)) {
      const n = parseInt(s, 10);
      return new Date(s.length === 10 ? n * 1000 : n);
    }
    // 若无时区信息，按 UTC 解释（补 Z）
    const hasTZ = /[zZ]|[+-]\d{2}:?\d{2}$/.test(s);
    const iso = s.includes("T") ? s : s.replace(" ", "T");
    return new Date(hasTZ ? iso : iso + "Z");
  }

  return new Date(input);
}

// 表单重置
export function resetForm(refName) {
  if (this.$refs[refName]) {
    this.$refs[refName].resetFields();
  }
}

// 添加日期范围
export function addDateRange(params, dateRange, propName) {
  let search = params;
  search.params =
    typeof search.params === "object" &&
    search.params !== null &&
    !Array.isArray(search.params)
      ? search.params
      : {};
  dateRange = Array.isArray(dateRange) ? dateRange : [];
  if (typeof propName === "undefined") {
    search.params["beginTime"] = dateRange[0];
    search.params["endTime"] = dateRange[1];
  } else {
    search.params["begin" + propName] = dateRange[0];
    search.params["end" + propName] = dateRange[1];
  }
  return search;
}

// 回显数据字典
export function selectDictLabel(datas, value) {
  if (value === undefined) {
    return "";
  }
  var actions = [];
  Object.keys(datas).some((key) => {
    if (datas[key].value == "" + value) {
      actions.push(datas[key].label);
      return true;
    }
  });
  if (actions.length === 0) {
    actions.push(value);
  }
  return actions.join("");
}

// 回显数据字典（字符串、数组）
export function selectDictLabels(datas, value, separator) {
  if (value === undefined || value.length === 0) {
    return "";
  }
  if (Array.isArray(value)) {
    value = value.join(",");
  }
  var actions = [];
  var currentSeparator = undefined === separator ? "," : separator;
  var temp = value.split(currentSeparator);
  Object.keys(value.split(currentSeparator)).some((val) => {
    var match = false;
    Object.keys(datas).some((key) => {
      if (datas[key].value == "" + temp[val]) {
        actions.push(datas[key].label + currentSeparator);
        match = true;
      }
    });
    if (!match) {
      actions.push(temp[val] + currentSeparator);
    }
  });
  return actions.join("").substring(0, actions.join("").length - 1);
}

// 字符串格式化(%s )
export function sprintf(str) {
  var args = arguments,
    flag = true,
    i = 1;
  str = str.replace(/%s/g, function () {
    var arg = args[i++];
    if (typeof arg === "undefined") {
      flag = false;
      return "";
    }
    return arg;
  });
  return flag ? str : "";
}

// 转换字符串，undefined,null等转化为""
export function parseStrEmpty(str) {
  if (!str || str == "undefined" || str == "null") {
    return "";
  }
  return str;
}

// 数据合并
export function mergeRecursive(source, target) {
  for (var p in target) {
    try {
      if (target[p].constructor == Object) {
        source[p] = mergeRecursive(source[p], target[p]);
      } else {
        source[p] = target[p];
      }
    } catch (e) {
      source[p] = target[p];
    }
  }
  return source;
}

/**
 * 构造树型结构数据
 * @param {*} data 数据源
 * @param {*} id id字段 默认 'id'
 * @param {*} parentId 父节点字段 默认 'parentId'
 * @param {*} children 孩子节点字段 默认 'children'
 */
export function handleTree(data, id, parentId, children) {
  let config = {
    id: id || "id",
    parentId: parentId || "parentId",
    childrenList: children || "children",
  };

  var childrenListMap = {};
  var tree = [];
  for (let d of data) {
    let id = d[config.id];
    childrenListMap[id] = d;
    if (!d[config.childrenList]) {
      d[config.childrenList] = [];
    }
  }

  for (let d of data) {
    let parentId = d[config.parentId];
    let parentObj = childrenListMap[parentId];
    if (!parentObj) {
      tree.push(d);
    } else {
      parentObj[config.childrenList].push(d);
    }
  }
  return tree;
}

/**
 * 参数处理
 * @param {*} params  参数
 */
export function tansParams(params) {
  let result = "";
  for (const propName of Object.keys(params)) {
    const value = params[propName];
    var part = encodeURIComponent(propName) + "=";
    if (value !== null && value !== "" && typeof value !== "undefined") {
      if (typeof value === "object") {
        for (const key of Object.keys(value)) {
          if (
            value[key] !== null &&
            value[key] !== "" &&
            typeof value[key] !== "undefined"
          ) {
            let params = propName + "[" + key + "]";
            var subPart = encodeURIComponent(params) + "=";
            result += subPart + encodeURIComponent(value[key]) + "&";
          }
        }
      } else {
        result += part + encodeURIComponent(value) + "&";
      }
    }
  }
  return result;
}

// 返回项目路径
export function getNormalPath(p) {
  if (p.length === 0 || !p || p == "undefined") {
    return p;
  }
  let res = p.replace("//", "/");
  if (res[res.length - 1] === "/") {
    return res.slice(0, res.length - 1);
  }
  return res;
}

// 验证是否为blob格式
export function blobValidate(data) {
  return data.type !== "application/json";
}
