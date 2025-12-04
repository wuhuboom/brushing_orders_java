// src/lib/geoip.js
import { Reader } from "maxminddb-reader";

let readerPromise = null;

async function loadReader(dbUrl = "/geoip/GeoLite2-Country.mmdb") {
  if (!readerPromise) {
    readerPromise = (async () => {
      const resp = await fetch(dbUrl, { cache: "force-cache" });
      if (!resp.ok)
        throw new Error(
          `Failed to load mmdb: ${resp.status} ${resp.statusText}`
        );
      const buf = await resp.arrayBuffer();
      return Reader.fromBuffer(buf);
    })();
  }
  return readerPromise;
}

/**
 * 通用查询方法：传入 IP，返回 { country, city, raw }
 * 注意：使用 Country.mmdb 时，city 一定为 null。若换成 City.mmdb，会返回城市。
 */
export async function lookupGeo(ip) {
  if (!ip) throw new Error("ip is required");

  const reader = await loadReader(); // 默认读取 /geoip/GeoLite2-Country.mmdb
  const result = reader.get(ip);

  // Country.mmdb 可用字段：country.iso_code / country.names
  // City.mmdb 额外可用：city.names、subdivisions、location 等
  const country =
    result?.country?.names?.en ??
    result?.country?.names?.zh_CN ??
    result?.country?.iso_code ??
    null;

  const city = result?.city?.names?.en ?? result?.city?.names?.zh_CN ?? null; // 用 Country.mmdb 时这里会是 null

  return { country, city, raw: result };
}
