import { getZoneActive } from "@/api/system/zone";

let _ACTIVE_TZ = null;
const LS_KEY = "ACTIVE_TZ_NAME";

// 模块加载就打一条，确认文件被引入了

export async function refreshActiveTimeZone() {
  try {
    const res = await getZoneActive();

    const row = res?.data || res; // 兼容不同返回结构
    const tz =
      row?.tzName || row?.tz_name || row?.tz || row?.timeZone || row?.timezone;

    if (isValidIanaTZ(tz)) {
      _ACTIVE_TZ = tz;
      localStorage.setItem(LS_KEY, tz);
      return tz;
    } else {
      console.warn("[TZ] invalid tz from API:", tz);
    }
  } catch (err) {
    console.error("[TZ] getZoneActive() error:", err);
  }
  const fallback = getActiveTimeZone();
  return fallback;
}

export function getActiveTimeZone() {
  const ls = localStorage.getItem(LS_KEY);
  if (isValidIanaTZ(_ACTIVE_TZ)) return _ACTIVE_TZ;
  if (isValidIanaTZ(ls)) return ls;
  const browser = Intl.DateTimeFormat().resolvedOptions().timeZone;
  return isValidIanaTZ(browser) ? browser : "UTC";
}

export function setActiveTimeZone(tz) {
  if (isValidIanaTZ(tz)) {
    _ACTIVE_TZ = tz;
    localStorage.setItem(LS_KEY, tz);
    return true;
  }
  console.warn("[TZ] setActiveTimeZone(): invalid tz", tz);
  return false;
}

export function isValidIanaTZ(tz) {
  if (!tz || typeof tz !== "string") return false;
  try {
    new Intl.DateTimeFormat("en-US", { timeZone: tz });
    return true;
  } catch {
    return false;
  }
}
