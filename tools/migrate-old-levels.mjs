import { createHash, randomUUID } from "node:crypto";
import { mkdir, readFile, stat, writeFile } from "node:fs/promises";
import { basename, dirname, extname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const sourceBaseUrl = "https://g9p7q0i2-20260513.top";
const account = process.env.OLD_ADMIN_ACCOUNT;
const password = process.env.OLD_ADMIN_PASSWORD;
const scriptDir = dirname(fileURLToPath(import.meta.url));
const projectDir = resolve(scriptDir, "..");
const migrationDir = resolve(
  projectDir,
  "sql",
  "migration_20260724",
  "levels",
);
const imageDir = resolve(
  process.env.LEVEL_IMAGE_DIR || "E:/order/uploadPath/level/img",
);
const imageUrlPrefix = "/profile/level/img";
const skipDownload = process.argv.includes("--skip-download");
const resumeSource = process.argv.includes("--resume-source");

if (!account || !password) {
  throw new Error("OLD_ADMIN_ACCOUNT and OLD_ADMIN_PASSWORD are required");
}

await mkdir(migrationDir, { recursive: true });
await mkdir(imageDir, { recursive: true });

const deviceId = randomUUID().replaceAll("-", "");
const baseHeaders = {
  Accept: "application/json, text/plain, */*",
  Origin: sourceBaseUrl,
  Referer: `${sourceBaseUrl}/marketing/levels`,
  "User-Agent":
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    + "AppleWebKit/537.36 Chrome/140.0 Safari/537.36",
  "X-App-Code": "app",
  "X-Channel-Code": "backend",
  "X-Device-Id": deviceId,
  "X-Locale": "zh-CN",
};

let authorization = "";

function log(message) {
  console.log(`[${new Date().toISOString()}] ${message}`);
}

async function requestJson(path, options = {}, retries = 3) {
  let lastError;
  for (let attempt = 1; attempt <= retries; attempt += 1) {
    try {
      const response = await fetch(`${sourceBaseUrl}${path}`, {
        ...options,
        headers: {
          ...baseHeaders,
          ...(authorization ? { Authorization: authorization } : {}),
          ...(options.headers || {}),
        },
        signal: AbortSignal.timeout(180_000),
      });
      const text = await response.text();
      let payload;
      try {
        payload = JSON.parse(text);
      } catch {
        throw new Error(
          `Invalid JSON from ${path}: HTTP ${response.status}, `
          + `body=${text.slice(0, 200)}`,
        );
      }
      if (!response.ok || (payload.status && Number(payload.status) !== 200)) {
        throw new Error(
          `${path} failed: HTTP ${response.status}, `
          + `status=${payload.status}, msg=${payload.msg || ""}`,
        );
      }
      return payload;
    } catch (error) {
      lastError = error;
      if (attempt < retries) {
        await new Promise((resolveDelay) => {
          setTimeout(resolveDelay, attempt * 1500);
        });
      }
    }
  }
  throw lastError;
}

async function authenticate() {
  const login = await requestJson("/backend/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json;charset=UTF-8" },
    body: JSON.stringify({ account, password }),
  });
  authorization = login?.data?.row?.token || "";
  if (!authorization) {
    throw new Error("Old server login succeeded without returning a token");
  }
  await requestJson("/backend/auth/users/info");
  log("Authenticated with the old server");
}

function dataRows(payload, endpoint) {
  const rows = payload?.data?.rows;
  if (!Array.isArray(rows)) {
    throw new Error(`${endpoint} did not return data.rows`);
  }
  return rows;
}

function dataRow(payload, endpoint) {
  const row = payload?.data?.row;
  if (!row || typeof row !== "object") {
    throw new Error(`${endpoint} did not return data.row`);
  }
  return row;
}

async function fetchSource() {
  await authenticate();
  const pagePayload = await requestJson(
    "/backend/marketing/levels/page?pageNo=1&pageSize=100",
  );
  const pageRows = dataRows(pagePayload, "levels/page");
  const total = Number(pagePayload?.data?.total);
  if (!Number.isInteger(total) || total !== pageRows.length) {
    throw new Error(
      `Level count mismatch: expected ${pagePayload?.data?.total}, `
      + `got ${pageRows.length}`,
    );
  }

  const levels = [];
  for (const pageRow of pageRows) {
    const detailPayload = await requestJson(
      `/backend/marketing/levels/${encodeURIComponent(pageRow.id)}`,
    );
    const detail = dataRow(detailPayload, `levels/${pageRow.id}`);
    let locale = null;
    try {
      const localePayload = await requestJson(
        `/backend/marketing/levels/${encodeURIComponent(pageRow.id)}/locale`,
        {},
        1,
      );
      locale = localePayload?.data?.row ?? localePayload?.data ?? null;
    } catch (error) {
      log(`Locale snapshot skipped for level ${pageRow.id}: ${error.message}`);
    }
    levels.push({ ...pageRow, ...detail, locale });
  }

  return levels.sort((left, right) => {
    return Number(left.seq) - Number(right.seq) || Number(left.id) - Number(right.id);
  });
}

function requiredNumber(row, key) {
  const value = Number(row[key]);
  if (!Number.isFinite(value)) {
    throw new Error(`Level ${row.id} has invalid ${key}: ${row[key]}`);
  }
  return value;
}

function requiredString(row, key) {
  const value = row[key] === null || row[key] === undefined
    ? ""
    : String(row[key]);
  if (!value.trim()) {
    throw new Error(`Level ${row.id} has empty ${key}`);
  }
  return value;
}

function sourceImageUrl(value) {
  if (!value) return "";
  const text = String(value);
  return text.startsWith("http://") || text.startsWith("https://")
    ? text
    : new URL(text, sourceBaseUrl).toString();
}

function safeImageExtension(url) {
  const pathname = new URL(url).pathname;
  const extension = extname(pathname).toLowerCase();
  return [".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg"]
    .includes(extension)
    ? extension
    : ".png";
}

function localImageName(url) {
  const digest = createHash("sha256").update(url).digest("hex").slice(0, 32);
  return `${digest}${safeImageExtension(url)}`;
}

function productMatchStatus(value) {
  return [false, 0, "0", "false", "disabled"].includes(value) ? "0" : "1";
}

function normalizeDescription(row) {
  const replacements = new Map([
    ["鈼廠", "●S"],
    ["鈼廝", "●P"],
    ["鈼廌", "●D"],
    ["鈼廢", "●U"],
    ["鈼廋", "●C"],
    ["鈼廈", "●B"],
    ["鈼廎", "●F"],
    ["鈼廚", "●N"],
  ]);
  let description = requiredString(row, "description");
  for (const [broken, corrected] of replacements) {
    description = description.replaceAll(broken, corrected);
  }
  description = description.replace(
    /鈼[^<]*product data per set/,
    `●${requiredNumber(row, "orderLimit")} product data per set`,
  );
  if (description.includes("鈼") || description.includes("�")) {
    throw new Error(`Level ${row.id} description still contains mojibake`);
  }
  return description;
}

function mysqlDate(value) {
  if (!value) return "1970-01-01 00:00:00.000";
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d{1,3})?$/.test(text)) {
    return text.includes(".") ? text : `${text}.000`;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return text.replace("T", " ").replace("Z", "");
  }
  return date.toISOString().replace("T", " ").replace("Z", "");
}

function prepareLevels(source) {
  const seenLevels = new Set();
  const imageByUrl = new Map();
  const levels = source.map((row) => {
    const level = requiredNumber(row, "seq");
    if (seenLevels.has(level)) {
      throw new Error(`Duplicate source level value: ${level}`);
    }
    seenLevels.add(level);

    const iconSource = sourceImageUrl(requiredString(row, "icon"));
    const imageName = localImageName(iconSource);
    imageByUrl.set(iconSource, imageName);

    return {
      sourceId: requiredNumber(row, "id"),
      name: requiredString(row, "name"),
      level,
      iconSource,
      icon: `${imageUrlPrefix}/${imageName}`,
      price: requiredNumber(row, "price"),
      minBalance: requiredNumber(row, "balanceMin"),
      inviteCount: requiredNumber(row, "inviteQuantityMin"),
      orderCountPerDay: requiredNumber(row, "orderLimit"),
      minCommissionRate: requiredNumber(row, "minCommissionPercentage"),
      maxCommissionRate: requiredNumber(row, "maxCommissionPercentage"),
      minContinuousCommissionRate: requiredNumber(
        row,
        "minEvenCommissionPercentage",
      ),
      maxContinuousCommissionRate: requiredNumber(
        row,
        "maxEvenCommissionPercentage",
      ),
      taskCountPerDay: requiredNumber(row, "completeLimit"),
      withdrawCountPerDay: requiredNumber(row, "withdrawalLimit"),
      withdrawFeeRate: requiredNumber(row, "withdrawalFeeRate"),
      minWithdrawAmount: requiredNumber(row, "withdrawalOrderSeqLimit"),
      withdrawLimitPerDay: requiredNumber(row, "withdrawalAmountLimit"),
      minWithdraw: requiredNumber(row, "withdrawalAmountMin"),
      maxWithdraw: requiredNumber(row, "withdrawalAmountMax"),
      description: normalizeDescription(row),
      createTime: mysqlDate(row.createdDate || row.createdTime),
      productMatchEnabled: productMatchStatus(row.productMatchEnabled),
      productMatchMin: Number(row.productMatchMin || 0),
      productMatchMax: Number(row.productMatchMax || 0),
      sourceData: row,
    };
  });

  return { levels, imageByUrl };
}

async function fileExists(path) {
  try {
    const details = await stat(path);
    return details.isFile() && details.size > 0;
  } catch {
    return false;
  }
}

async function downloadImages(imageByUrl) {
  let downloaded = 0;
  let skipped = 0;
  const failures = [];

  for (const [url, filename] of imageByUrl) {
    const targetPath = resolve(imageDir, filename);
    if (await fileExists(targetPath)) {
      skipped += 1;
      continue;
    }
    try {
      const response = await fetch(url, {
        headers: {
          ...baseHeaders,
          ...(url.startsWith(sourceBaseUrl) && authorization
            ? { Authorization: authorization }
            : {}),
        },
        signal: AbortSignal.timeout(180_000),
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const bytes = Buffer.from(await response.arrayBuffer());
      if (!bytes.length) {
        throw new Error("empty response");
      }
      await writeFile(targetPath, bytes);
      downloaded += 1;
      log(`Downloaded ${basename(targetPath)}`);
    } catch (error) {
      failures.push({ url, filename, error: error.message });
    }
  }

  return {
    total: imageByUrl.size,
    downloaded,
    skipped,
    failed: failures.length,
    failures,
  };
}

function sqlString(value) {
  if (value === null || value === undefined) return "NULL";
  return `'${String(value)
    .replaceAll("\\", "\\\\")
    .replaceAll("'", "''")
    .replaceAll("\0", "")}'`;
}

function sqlNumber(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    throw new Error(`Invalid SQL number: ${value}`);
  }
  return String(number);
}

function levelSqlTuple(level) {
  const values = [
    level.sourceId,
    level.name,
    level.level,
    level.icon,
    level.price,
    level.minBalance,
    level.inviteCount,
    level.orderCountPerDay,
    level.minCommissionRate,
    level.maxCommissionRate,
    level.minContinuousCommissionRate,
    level.maxContinuousCommissionRate,
    level.taskCountPerDay,
    level.withdrawCountPerDay,
    level.withdrawFeeRate,
    level.minWithdrawAmount,
    level.withdrawLimitPerDay,
    level.minWithdraw,
    level.maxWithdraw,
    level.description,
    level.createTime,
    level.productMatchEnabled,
    level.productMatchMin,
    level.productMatchMax,
  ];
  const stringIndexes = new Set([1, 3, 19, 20, 21]);
  return `(${values.map((value, index) => {
    return stringIndexes.has(index) ? sqlString(value) : sqlNumber(value);
  }).join(", ")})`;
}

function buildApplySql(levels) {
  const tuples = levels.map(levelSqlTuple).join(",\n");
  return `-- Generated by tools/migrate-old-levels.mjs
-- Source: ${sourceBaseUrl}/backend/marketing/levels/page
SET NAMES utf8mb4;
START TRANSACTION;

ALTER TABLE goods_member_level
    MODIFY COLUMN icon VARCHAR(500) CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图标';

DROP TEMPORARY TABLE IF EXISTS tmp_old_member_level;
CREATE TEMPORARY TABLE tmp_old_member_level (
    source_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    level INT NOT NULL,
    icon VARCHAR(500) NOT NULL,
    price DECIMAL(20,2) NOT NULL,
    min_balance DECIMAL(20,2) NOT NULL,
    invite_count INT NOT NULL,
    order_count_per_day INT NOT NULL,
    min_commission_rate DECIMAL(5,2) NOT NULL,
    max_commission_rate DECIMAL(5,2) NOT NULL,
    min_continuous_commission_rate DECIMAL(5,2) NOT NULL,
    max_continuous_commission_rate DECIMAL(5,2) NOT NULL,
    task_count_per_day INT NOT NULL,
    withdraw_count_per_day INT NOT NULL,
    withdraw_fee_rate DECIMAL(5,2) NOT NULL,
    min_withdraw_amount DECIMAL(20,2) NOT NULL,
    withdraw_limit_per_day DECIMAL(20,2) NOT NULL,
    min_withdraw DECIMAL(20,2) NOT NULL,
    max_withdraw DECIMAL(20,2) NOT NULL,
    description TEXT NOT NULL,
    create_time DATETIME(3) NOT NULL,
    product_match_enabled CHAR(1) NOT NULL,
    product_match_min DECIMAL(5,2) NOT NULL,
    product_match_max DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (level),
    UNIQUE KEY uk_tmp_old_member_level_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO tmp_old_member_level (
    source_id, name, level, icon, price, min_balance, invite_count,
    order_count_per_day, min_commission_rate, max_commission_rate,
    min_continuous_commission_rate, max_continuous_commission_rate,
    task_count_per_day, withdraw_count_per_day, withdraw_fee_rate,
    min_withdraw_amount, withdraw_limit_per_day, min_withdraw, max_withdraw,
    description, create_time, product_match_enabled, product_match_min,
    product_match_max
) VALUES
${tuples};

UPDATE goods_member_level target
JOIN tmp_old_member_level source ON source.level = target.level
SET target.name = source.name,
    target.icon = source.icon,
    target.price = source.price,
    target.min_balance = source.min_balance,
    target.invite_count = source.invite_count,
    target.order_count_per_day = source.order_count_per_day,
    target.min_commission_rate = source.min_commission_rate,
    target.max_commission_rate = source.max_commission_rate,
    target.min_continuous_commission_rate =
        source.min_continuous_commission_rate,
    target.max_continuous_commission_rate =
        source.max_continuous_commission_rate,
    target.task_count_per_day = source.task_count_per_day,
    target.withdraw_count_per_day = source.withdraw_count_per_day,
    target.withdraw_fee_rate = source.withdraw_fee_rate,
    target.min_withdraw_amount = source.min_withdraw_amount,
    target.withdraw_limit_per_day = source.withdraw_limit_per_day,
    target.min_withdraw = source.min_withdraw,
    target.max_withdraw = source.max_withdraw,
    target.description = source.description,
    target.create_time = source.create_time,
    target.product_match_enabled = source.product_match_enabled,
    target.product_match_min = source.product_match_min,
    target.product_match_max = source.product_match_max;

INSERT INTO goods_member_level (
    name, level, icon, price, min_balance, invite_count, order_count_per_day,
    min_commission_rate, max_commission_rate, min_continuous_commission_rate,
    max_continuous_commission_rate, task_count_per_day, withdraw_count_per_day,
    withdraw_fee_rate, min_withdraw_amount, withdraw_limit_per_day,
    min_withdraw, max_withdraw, description, create_time,
    product_match_enabled, product_match_min, product_match_max
)
SELECT source.name, source.level, source.icon, source.price, source.min_balance,
       source.invite_count, source.order_count_per_day,
       source.min_commission_rate, source.max_commission_rate,
       source.min_continuous_commission_rate,
       source.max_continuous_commission_rate, source.task_count_per_day,
       source.withdraw_count_per_day, source.withdraw_fee_rate,
       source.min_withdraw_amount, source.withdraw_limit_per_day,
       source.min_withdraw, source.max_withdraw, source.description,
       source.create_time, source.product_match_enabled,
       source.product_match_min, source.product_match_max
FROM tmp_old_member_level source
LEFT JOIN goods_member_level target ON target.level = source.level
WHERE target.id IS NULL;

DELETE target
FROM goods_member_level target
LEFT JOIN tmp_old_member_level source ON source.level = target.level
LEFT JOIN order_user user_ref ON user_ref.vip_id = target.id
WHERE source.level IS NULL
  AND user_ref.id IS NULL;

COMMIT;

SELECT id, name, level, icon, price, min_balance, invite_count,
       order_count_per_day, min_commission_rate, max_commission_rate,
       min_continuous_commission_rate, max_continuous_commission_rate,
       task_count_per_day, withdraw_count_per_day, withdraw_fee_rate,
       min_withdraw_amount, withdraw_limit_per_day, min_withdraw, max_withdraw,
       product_match_enabled, product_match_min, product_match_max
FROM goods_member_level
ORDER BY level, id;
`;
}

let source;
const sourcePath = resolve(migrationDir, "old-levels-source.json");
if (resumeSource) {
  source = JSON.parse(await readFile(sourcePath, "utf8"));
  log(`Loaded ${source.length} levels from the local source snapshot`);
} else {
  source = await fetchSource();
  await writeFile(sourcePath, `${JSON.stringify(source, null, 2)}\n`, "utf8");
  log(`Fetched ${source.length} levels from the old server`);
}

const prepared = prepareLevels(source);
const preparedPath = resolve(migrationDir, "old-levels-prepared.json");
await writeFile(
  preparedPath,
  `${JSON.stringify(prepared.levels, null, 2)}\n`,
  "utf8",
);

let imageResult = {
  total: prepared.imageByUrl.size,
  downloaded: 0,
  skipped: 0,
  failed: 0,
  failures: [],
};
if (!skipDownload) {
  imageResult = await downloadImages(prepared.imageByUrl);
  if (imageResult.failed) {
    throw new Error(
      `${imageResult.failed} level images failed to download: `
      + JSON.stringify(imageResult.failures),
    );
  }
}

const applySqlPath = resolve(migrationDir, "apply-old-levels.sql");
await writeFile(
  applySqlPath,
  buildApplySql(prepared.levels),
  "utf8",
);

const manifest = {
  generatedAt: new Date().toISOString(),
  sourceBaseUrl,
  levelCount: prepared.levels.length,
  imageCount: prepared.imageByUrl.size,
  imageDirectory: imageDir,
  imageUrlPrefix,
  imageDownload: imageResult,
  sourcePath,
  preparedPath,
  applySqlPath,
};
await writeFile(
  resolve(migrationDir, "old-levels-manifest.json"),
  `${JSON.stringify(manifest, null, 2)}\n`,
  "utf8",
);

log(`Prepared ${prepared.levels.length} levels`);
log(`Downloaded level images: ${JSON.stringify(imageResult)}`);
log(`Apply SQL: ${applySqlPath}`);
