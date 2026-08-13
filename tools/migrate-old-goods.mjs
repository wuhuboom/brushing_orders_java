import { createHash, randomUUID } from "node:crypto";
import { createWriteStream } from "node:fs";
import {
  access,
  mkdir,
  readFile,
  rename,
  rm,
  stat,
  writeFile,
} from "node:fs/promises";
import { basename, dirname, extname, resolve } from "node:path";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";
import { fileURLToPath } from "node:url";

const sourceBaseUrl = "https://g9p7q0i2-20260513.top";
const account = process.env.OLD_ADMIN_ACCOUNT;
const password = process.env.OLD_ADMIN_PASSWORD;
const pageSize = Number(process.env.OLD_GOODS_PAGE_SIZE || 5000);
const apiConcurrency = Number(process.env.OLD_GOODS_API_CONCURRENCY || 3);
const downloadConcurrency = Number(process.env.OLD_GOODS_DOWNLOAD_CONCURRENCY || 16);
const skipDownload = process.argv.includes("--skip-download");
const resumeSource = process.argv.includes("--resume-source");
const scriptDir = dirname(fileURLToPath(import.meta.url));
const projectDir = resolve(scriptDir, "..");
const migrationDir = resolve(projectDir, "sql", "migration_20260724");
const imageDir = resolve(process.env.GOODS_IMAGE_DIR || "E:/order/uploadPath/goods/img");
const imageReplacements = new Map([
  [
    "https://osswotlk.oss-ap-northeast-1.aliyuncs.com/df056f25b872b94f226570c0b180d333.jpg",
    {
      downloadUrl:
        "https://www.gosupps.com/media/catalog/product/cache/25/image/"
        + "9df78eab33525d08d6e5fb8d27136e95/7/1/71T9PuayW5L_1.jpg",
      filename: "79dbb15d8651326aa8619c3946da18d4.jpg",
      sourcePage:
        "https://www.gosupps.com/hawaiian-tropic-spf-50-broad-spectrum-"
        + "sunscreen-sheer-touch-moisturizing-protection-sunscreen-lotion-"
        + "coconut-16-0-fl-oz-pack-of-2-lotion-twin-pack.html",
    },
  ],
  [
    "https://osswotlk.oss-ap-northeast-1.aliyuncs.com/3aa051e55d854a05fcbe21928a32a439.jpg",
    {
      downloadUrl:
        "https://sony.scene7.com/is/image/sonyglobalsolutions/"
        + "wh-ch720_Primary_image_white?$primaryshotPreset$&fmt=png-alpha",
      filename: "15b4b3786a8554d041fd5c11a8936c65.png",
      sourcePage:
        "https://store.sony.co.nz/headphones-noisecancelling/WHCH720NW.html",
    },
  ],
  [
    "https://osswotlk.oss-ap-northeast-1.aliyuncs.com/8b39031f772fa0dec8f8a0463820334f.jpg",
    {
      downloadUrl:
        "https://www.sigma-global.com/lenses/c016_30_14_product_img01.png",
      filename: "9a321a750d2318d685b865fd245a36f5.png",
      sourcePage: "https://www.sigma-global.com/en/lenses/c016_30_14/",
    },
  ],
]);
const productTextCorrections = new Map([
  [
    "1000156",
    {
      title: "Lenspen NLP1 New Lenspen Original",
      subtitle: "Lenspen NLP1 New Lenspen Original",
      reason: "Removed a truncated mojibake suffix from the source title.",
    },
  ],
  [
    "1002762",
    {
      title:
        "Replacement ear pads and cup covers for Sony PS3/PS4 Gold Wireless "
        + "PlayStation 3/4 CECHYA-0083 Stereo 7.1 Virtual Surround Headphones",
      subtitle:
        "Replacement ear pads and cup covers for Sony PS3/PS4 Gold Wireless "
        + "PlayStation 3/4 CECHYA-0083 Stereo 7.1 Virtual Surround Headphones",
      reason: "Replaced a mojibake product-name fragment from the source title.",
    },
  ],
]);

if (!account || !password) {
  throw new Error("OLD_ADMIN_ACCOUNT and OLD_ADMIN_PASSWORD are required");
}

if (!Number.isInteger(pageSize) || pageSize < 1 || pageSize > 5000) {
  throw new Error(`Invalid OLD_GOODS_PAGE_SIZE: ${pageSize}`);
}

if (!Number.isInteger(apiConcurrency) || apiConcurrency < 1 || apiConcurrency > 8) {
  throw new Error(`Invalid OLD_GOODS_API_CONCURRENCY: ${apiConcurrency}`);
}

if (
  !Number.isInteger(downloadConcurrency)
  || downloadConcurrency < 1
  || downloadConcurrency > 256
) {
  throw new Error(
    `Invalid OLD_GOODS_DOWNLOAD_CONCURRENCY: ${downloadConcurrency}`,
  );
}

await mkdir(migrationDir, { recursive: true });
await mkdir(imageDir, { recursive: true });

const deviceId = randomUUID().replaceAll("-", "");
const baseHeaders = {
  Accept: "application/json, text/plain, */*",
  Origin: sourceBaseUrl,
  Referer: `${sourceBaseUrl}/marketing/spu/spus`,
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
        await new Promise((resolveDelay) => setTimeout(resolveDelay, attempt * 1500));
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

  // The old backend initializes the role permission context on this request.
  await requestJson("/backend/auth/users/info");
  log("Authenticated with the old server");
}

function rowArray(payload, endpoint) {
  const rows = payload?.data?.rows;
  if (!Array.isArray(rows)) {
    throw new Error(`${endpoint} did not return data.rows`);
  }
  return rows;
}

async function fetchSourceData() {
  const categoriesPayload = await requestJson(
    "/backend/marketing/categories/list",
  );
  const categories = rowArray(categoriesPayload, "categories/list");

  const firstPayload = await requestJson(
    `/backend/marketing/spus/page?pageNo=1&pageSize=${pageSize}`,
  );
  const firstRows = rowArray(firstPayload, "spus/page");
  const total = Number(firstPayload?.data?.total);

  if (!Number.isInteger(total) || total < 1) {
    throw new Error(`Invalid source total: ${firstPayload?.data?.total}`);
  }

  const pageCount = Math.ceil(total / pageSize);
  const pages = Array.from({ length: pageCount });
  pages[0] = firstRows;
  let nextPageNo = 2;
  let fetchedCount = firstRows.length;
  log(`Fetched page 1/${pageCount}: ${fetchedCount}/${total}`);

  async function pageWorker() {
    while (true) {
      const pageNo = nextPageNo;
      nextPageNo += 1;
      if (pageNo > pageCount) return;

      const payload = await requestJson(
        `/backend/marketing/spus/page?pageNo=${pageNo}&pageSize=${pageSize}`,
      );
      const rows = rowArray(payload, `spus/page?pageNo=${pageNo}`);
      pages[pageNo - 1] = rows;
      fetchedCount += rows.length;
      log(`Fetched page ${pageNo}/${pageCount}: ${fetchedCount}/${total}`);
    }
  }

  await Promise.all(
    Array.from(
      { length: Math.min(apiConcurrency, Math.max(pageCount - 1, 0)) },
      () => pageWorker(),
    ),
  );

  const goods = pages.flat();

  if (goods.length !== total) {
    throw new Error(`Source count mismatch: expected ${total}, got ${goods.length}`);
  }

  return { categories, goods, total };
}

function stringValue(value) {
  return value === null || value === undefined ? "" : String(value);
}

function mysqlTsvValue(value) {
  if (value === null || value === undefined) {
    return "\\N";
  }

  return String(value)
    .replaceAll("\\", "\\\\")
    .replaceAll("\0", "\\0")
    .replaceAll("\t", "\\t")
    .replaceAll("\r", "\\r")
    .replaceAll("\n", "\\n");
}

function decodeMysqlTsvValue(value) {
  if (value === "\\N") return null;

  let decoded = "";
  for (let index = 0; index < value.length; index += 1) {
    const character = value[index];
    if (character !== "\\" || index === value.length - 1) {
      decoded += character;
      continue;
    }

    index += 1;
    const escaped = value[index];
    decoded += {
      0: "\0",
      n: "\n",
      r: "\r",
      t: "\t",
      "\\": "\\",
    }[escaped] ?? escaped;
  }
  return decoded;
}

function tsvLine(values) {
  return `${values.map(mysqlTsvValue).join("\t")}\n`;
}

function safeImageName(url, sourceId, collisionOwners) {
  if (!url) return "";

  let pathname;
  try {
    pathname = new URL(url).pathname;
  } catch {
    return "";
  }

  const rawName = decodeURIComponent(pathname.split("/").pop() || "");
  const sanitized = rawName.replace(/[<>:"/\\|?*\u0000-\u001f]/g, "_");
  const fallbackExtension = extname(sanitized) || ".jpg";
  const fallbackName = `${sourceId}${fallbackExtension}`;
  const candidate = sanitized || fallbackName;
  const owner = collisionOwners.get(candidate.toLowerCase());

  if (!owner || owner === url) {
    collisionOwners.set(candidate.toLowerCase(), url);
    return candidate;
  }

  const extension = extname(candidate);
  const stem = candidate.slice(0, candidate.length - extension.length);
  const uniqueName = `${stem}-${sourceId}${extension || ".jpg"}`;
  collisionOwners.set(uniqueName.toLowerCase(), url);
  return uniqueName;
}

function prepareData(source) {
  const idSet = new Set();
  const collisionOwners = new Map();
  const imageByUrl = new Map();
  const categoryIds = new Set(source.categories.map((row) => stringValue(row.id)));
  const missingCategoryIds = new Set();
  const duplicateIds = [];
  const missingImages = [];
  const normalizedGoods = [];
  let maxTitleLength = 0;
  let maxSubtitleLength = 0;

  for (const row of source.goods) {
    const sourceId = stringValue(row.id);
    const categoryId = stringValue(row.categoryId);

    if (!sourceId) {
      throw new Error("A source product has no id");
    }
    if (idSet.has(sourceId)) {
      duplicateIds.push(sourceId);
    }
    idSet.add(sourceId);

    if (!categoryIds.has(categoryId)) {
      missingCategoryIds.add(categoryId);
    }

    const textCorrection = productTextCorrections.get(sourceId);
    const title = textCorrection?.title || stringValue(row.title);
    const subtitle = textCorrection?.subtitle || stringValue(row.subtitle);
    const sourcePic = stringValue(row.pic);
    const replacement = imageReplacements.get(sourcePic);
    const downloadUrl = replacement?.downloadUrl || sourcePic;
    const existingImageName = imageByUrl.get(downloadUrl);
    const imageName = replacement?.filename || existingImageName || safeImageName(
      sourcePic,
      sourceId,
      collisionOwners,
    );

    if (!sourcePic || !imageName) {
      missingImages.push(sourceId);
    } else if (!existingImageName) {
      imageByUrl.set(downloadUrl, imageName);
    }

    maxTitleLength = Math.max(maxTitleLength, [...title].length);
    maxSubtitleLength = Math.max(maxSubtitleLength, [...subtitle].length);

    normalizedGoods.push({
      sourceId,
      categoryId,
      title,
      subtitle,
      sourcePic,
      localImage: imageName ? `/profile/goods/img/${imageName}` : "",
      price: row.price ?? 0,
      seq: row.seq ?? 0,
      enabled: stringValue(row.enabled),
      unitPrice: row.param?.unitPrice ?? 0,
      quantity: row.param?.quantity ?? 0,
      star: row.param?.star ?? 0,
      rate: row.param?.rate ?? 0,
      description: stringValue(row.param?.description),
      createdBy: stringValue(row.createdBy),
      createdDate: stringValue(row.createdDate),
      updatedDate: stringValue(row.lastModifiedDate),
      sourceJson: JSON.stringify(row),
    });
  }

  if (duplicateIds.length) {
    throw new Error(
      `Duplicate source product ids: ${duplicateIds.slice(0, 20).join(", ")}`,
    );
  }
  if (missingCategoryIds.size) {
    throw new Error(
      `Unknown category ids: ${[...missingCategoryIds].join(", ")}`,
    );
  }

  const normalizedCategories = source.categories.map((row) => ({
    sourceId: stringValue(row.id),
    title: stringValue(row.title),
    enabled: stringValue(row.enabled),
    seq: row.seq ?? 0,
    image: stringValue(row.pic),
    subtitle: stringValue(row.subtitle),
    remark: stringValue(row.remark),
    createdDate: stringValue(row.createdDate),
    sourceJson: JSON.stringify(row),
  }));

  return {
    imageByUrl,
    missingImages,
    normalizedCategories,
    normalizedGoods,
    stats: {
      categoryCount: normalizedCategories.length,
      productCount: normalizedGoods.length,
      uniqueImageCount: imageByUrl.size,
      missingImageCount: missingImages.length,
      maxTitleLength,
      maxSubtitleLength,
    },
  };
}

async function writeMigrationFiles(source, prepared) {
  const sourceJsonlPath = resolve(migrationDir, "old-goods-source.jsonl");
  const goodsTsvPath = resolve(migrationDir, "old-goods.tsv");
  const categoriesTsvPath = resolve(migrationDir, "old-goods-types.tsv");
  const manifestPath = resolve(migrationDir, "old-goods-manifest.json");

  const sourceJsonl = source.goods
    .map((row) => JSON.stringify(row))
    .join("\n") + "\n";
  const goodsTsv = prepared.normalizedGoods.map((row) => tsvLine([
    row.sourceId,
    row.categoryId,
    row.title,
    row.subtitle,
    row.sourcePic,
    row.localImage,
    row.price,
    row.seq,
    row.enabled,
    row.unitPrice,
    row.quantity,
    row.star,
    row.rate,
    row.description,
    row.createdBy,
    row.createdDate || null,
    row.updatedDate || null,
    row.sourceJson,
  ])).join("");
  const categoriesTsv = prepared.normalizedCategories.map((row) => tsvLine([
    row.sourceId,
    row.title,
    row.enabled,
    row.seq,
    row.image,
    row.subtitle,
    row.remark,
    row.createdDate || null,
    row.sourceJson,
  ])).join("");

  const sourceSha256 = createHash("sha256").update(sourceJsonl).digest("hex");
  const manifest = {
    generatedAt: new Date().toISOString(),
    sourceBaseUrl,
    expectedTotal: source.total,
    ...prepared.stats,
    missingImageProductIds: prepared.missingImages,
    imageReplacements: [...imageReplacements].map(([sourceUrl, replacement]) => ({
      sourceUrl,
      ...replacement,
    })),
    productTextCorrections: [...productTextCorrections].map(
      ([sourceId, correction]) => ({ sourceId, ...correction }),
    ),
    sourceSha256,
  };

  await writeFile(sourceJsonlPath, sourceJsonl, "utf8");
  await writeFile(goodsTsvPath, goodsTsv, "utf8");
  await writeFile(categoriesTsvPath, categoriesTsv, "utf8");
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");

  return {
    categoriesTsvPath,
    goodsTsvPath,
    manifest,
    manifestPath,
    sourceJsonlPath,
  };
}

async function fileExistsWithContent(path) {
  try {
    return (await stat(path)).size > 0;
  } catch {
    return false;
  }
}

async function downloadImageChunks(url, headers, contentLength) {
  const chunkSize = 256 * 1024;
  const ranges = [];

  for (let start = 0; start < contentLength; start += chunkSize) {
    ranges.push({
      start,
      end: Math.min(start + chunkSize - 1, contentLength - 1),
    });
  }

  const chunks = await Promise.all(ranges.map(async ({ start, end }) => {
    const response = await fetch(url, {
      headers: {
        ...headers,
        Range: `bytes=${start}-${end}`,
      },
      signal: AbortSignal.timeout(120_000),
    });
    if (response.status !== 206) {
      throw new Error(
        `Range ${start}-${end} returned HTTP ${response.status}`,
      );
    }

    const chunk = Buffer.from(await response.arrayBuffer());
    const expectedLength = end - start + 1;
    if (chunk.length !== expectedLength) {
      throw new Error(
        `Range ${start}-${end} returned ${chunk.length} bytes, `
        + `expected ${expectedLength}`,
      );
    }
    return chunk;
  }));

  return Buffer.concat(chunks, contentLength);
}

async function downloadImage(url, filename, retries = 4) {
  const targetPath = resolve(imageDir, filename);
  const partialPath = `${targetPath}.part`;
  const downloadUrl = new URL(url);
  const expectedMd5 = basename(filename, extname(filename)).toLowerCase();

  if (
    downloadUrl.protocol === "https:"
    && downloadUrl.hostname.endsWith(".aliyuncs.com")
  ) {
    downloadUrl.protocol = "http:";
  }

  if (await fileExistsWithContent(targetPath)) {
    if (/^[a-f0-9]{32}$/.test(expectedMd5)) {
      const actualMd5 = createHash("md5")
        .update(await readFile(targetPath))
        .digest("hex");
      if (actualMd5 === expectedMd5) {
        return { skipped: true, targetPath };
      }
      await rm(targetPath, { force: true });
    } else {
      return { skipped: true, targetPath };
    }
  }

  let lastError;
  for (let attempt = 1; attempt <= retries; attempt += 1) {
    try {
      const downloadHeaders = {
        Referer: sourceBaseUrl,
        "User-Agent": baseHeaders["User-Agent"],
      };
      const headResponse = await fetch(downloadUrl, {
        method: "HEAD",
        headers: downloadHeaders,
        signal: AbortSignal.timeout(20_000),
      });
      if (!headResponse.ok) {
        throw new Error(`HTTP ${headResponse.status}`);
      }

      const contentLength = Number(headResponse.headers.get("content-length"));
      if (Number.isInteger(contentLength) && contentLength > 512 * 1024) {
        const image = await downloadImageChunks(
          downloadUrl,
          downloadHeaders,
          contentLength,
        );
        await writeFile(partialPath, image);
      } else {
        const response = await fetch(downloadUrl, {
          headers: downloadHeaders,
          signal: AbortSignal.timeout(120_000),
        });
        if (!response.ok || !response.body) {
          throw new Error(`HTTP ${response.status}`);
        }
        await pipeline(
          Readable.fromWeb(response.body),
          createWriteStream(partialPath),
        );
      }

      const fileStat = await stat(partialPath);
      if (fileStat.size < 1) {
        throw new Error("Downloaded file is empty");
      }

      if (/^[a-f0-9]{32}$/.test(expectedMd5)) {
        const actualMd5 = createHash("md5")
          .update(await readFile(partialPath))
          .digest("hex");
        if (actualMd5 !== expectedMd5) {
          throw new Error(
            `MD5 mismatch: expected ${expectedMd5}, got ${actualMd5}`,
          );
        }
      }

      await rename(partialPath, targetPath);
      return { skipped: false, targetPath };
    } catch (error) {
      lastError = error;
      await rm(partialPath, { force: true });
      if (attempt < retries) {
        await new Promise((resolveDelay) => setTimeout(resolveDelay, attempt * 1000));
      }
    }
  }

  throw new Error(`${url}: ${lastError?.message || lastError}`);
}

async function downloadImages(imageByUrl) {
  const entries = [...imageByUrl.entries()];
  const failures = [];
  let cursor = 0;
  let downloaded = 0;
  let skipped = 0;

  async function worker() {
    while (true) {
      const index = cursor;
      cursor += 1;
      if (index >= entries.length) return;

      const [url, filename] = entries[index];
      try {
        const result = await downloadImage(url, filename);
        if (result.skipped) skipped += 1;
        else downloaded += 1;
      } catch (error) {
        failures.push({ url, filename, error: error.message });
      }

      const completed = downloaded + skipped + failures.length;
      if (completed % 250 === 0 || completed === entries.length) {
        log(
          `Images ${completed}/${entries.length} `
          + `(downloaded=${downloaded}, skipped=${skipped}, failed=${failures.length})`,
        );
      }
    }
  }

  await Promise.all(
    Array.from(
      { length: Math.min(downloadConcurrency, entries.length) },
      () => worker(),
    ),
  );

  const result = {
    total: entries.length,
    downloaded,
    skipped,
    failed: failures.length,
    failures,
  };
  await writeFile(
    resolve(migrationDir, "old-goods-image-download.json"),
    `${JSON.stringify(result, null, 2)}\n`,
    "utf8",
  );
  return result;
}

function sqlPath(path) {
  return path.replaceAll("\\", "/").replaceAll("'", "''");
}

async function writeApplySql(files, prepared) {
  const sql = `-- Generated by tools/migrate-old-goods.mjs.
-- Source: ${sourceBaseUrl}/marketing/spu/spus
SET NAMES utf8mb4;

DROP TABLE IF EXISTS goods_import_stage_20260724;
CREATE TABLE goods_import_stage_20260724 (
    source_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    subtitle TEXT NULL,
    source_pic VARCHAR(1000) NULL,
    local_image VARCHAR(500) NOT NULL,
    price DECIMAL(20, 2) NOT NULL,
    seq BIGINT NOT NULL,
    enabled CHAR(1) NOT NULL,
    unit_price DECIMAL(20, 2) NULL,
    quantity INT NULL,
    star DECIMAL(10, 2) NULL,
    rate DECIMAL(10, 2) NULL,
    description LONGTEXT NULL,
    created_by VARCHAR(64) NULL,
    created_date DATETIME NULL,
    updated_date DATETIME NULL,
    source_data JSON NOT NULL,
    PRIMARY KEY (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOAD DATA LOCAL INFILE '${sqlPath(files.goodsTsvPath)}'
INTO TABLE goods_import_stage_20260724
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\\t' ESCAPED BY '\\\\'
LINES TERMINATED BY '\\n'
(source_id, category_id, title, subtitle, source_pic, local_image, price, seq,
 enabled, unit_price, quantity, star, rate, description, created_by,
 created_date, updated_date, source_data);

DROP TABLE IF EXISTS goods_type_import_stage_20260724;
CREATE TABLE goods_type_import_stage_20260724 (
    source_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    enabled CHAR(1) NOT NULL,
    seq BIGINT NOT NULL,
    image VARCHAR(500) NULL,
    subtitle VARCHAR(500) NULL,
    remark VARCHAR(500) NULL,
    created_date DATETIME NULL,
    source_data JSON NOT NULL,
    PRIMARY KEY (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOAD DATA LOCAL INFILE '${sqlPath(files.categoriesTsvPath)}'
INTO TABLE goods_type_import_stage_20260724
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\\t' ESCAPED BY '\\\\'
LINES TERMINATED BY '\\n'
(source_id, title, enabled, seq, image, subtitle, remark, created_date, source_data);

DROP PROCEDURE IF EXISTS assert_old_goods_import_20260724;
DELIMITER $$
CREATE PROCEDURE assert_old_goods_import_20260724()
BEGIN
    IF (SELECT COUNT(*) FROM goods_import_stage_20260724) <> ${prepared.stats.productCount} THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Old goods staging row count mismatch';
    END IF;
    IF (SELECT COUNT(*) FROM goods_type_import_stage_20260724) <> ${prepared.stats.categoryCount} THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Old goods category row count mismatch';
    END IF;
    IF EXISTS (
        SELECT 1 FROM goods_import_stage_20260724
        WHERE local_image IS NULL OR local_image = ''
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Old goods staging contains missing images';
    END IF;
END$$
DELIMITER ;
CALL assert_old_goods_import_20260724();
DROP PROCEDURE assert_old_goods_import_20260724;

SET @add_source_data_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'goods'
          AND column_name = 'source_data'
    ),
    'SELECT 1',
    'ALTER TABLE goods ADD COLUMN source_data JSON NULL COMMENT ''原服务器完整商品数据'' AFTER description'
);
PREPARE add_source_data_stmt FROM @add_source_data_sql;
EXECUTE add_source_data_stmt;
DEALLOCATE PREPARE add_source_data_stmt;

ALTER TABLE goods
    MODIFY COLUMN title VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
    MODIFY COLUMN serial_number BIGINT NOT NULL COMMENT '原服务器序号',
    MODIFY COLUMN image VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片',
    MODIFY COLUMN sub_title VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '二级标题',
    MODIFY COLUMN star_rating DECIMAL(10, 2) NULL COMMENT '星级';

START TRANSACTION;

DELETE FROM goods;
DELETE FROM goods_type;

INSERT INTO goods_type (
    id, title, is_enabled, serial_number, image, sub_title, remarks, create_time
)
SELECT
    source_id,
    title,
    CASE enabled WHEN '1' THEN '0' ELSE '1' END,
    seq,
    COALESCE(image, ''),
    NULLIF(subtitle, ''),
    NULLIF(remark, ''),
    created_date
FROM goods_type_import_stage_20260724
ORDER BY source_id;

INSERT INTO goods (
    id, title, type_id, is_enabled, price, serial_number, image, sub_title,
    unit_price, quantity, star_rating, rating, description, source_data,
    create_by, create_time
)
SELECT
    source_id,
    title,
    category_id,
    CASE enabled WHEN '1' THEN '0' ELSE '1' END,
    price,
    seq,
    local_image,
    NULLIF(subtitle, ''),
    unit_price,
    quantity,
    star,
    rate,
    NULLIF(description, ''),
    source_data,
    created_by,
    created_date
FROM goods_import_stage_20260724
ORDER BY source_id;

COMMIT;

DROP TABLE goods_import_stage_20260724;
DROP TABLE goods_type_import_stage_20260724;

-- Explicit backup tables requested for removal.
DROP TABLE IF EXISTS goods_withdrawal_account_bak_20260723_232555;
DROP TABLE IF EXISTS order_withdrawal_bak_20260723_232555;
DROP TABLE IF EXISTS sys_menu_backup_before_remove_monitor_tool;
DROP TABLE IF EXISTS sys_menu_bak_20260723_232555;
DROP TABLE IF EXISTS sys_role_menu_backup_before_remove_monitor_tool;

-- Repair known schema comments that were previously imported with mojibake.
ALTER TABLE order_user
    MODIFY COLUMN sign_days INT NOT NULL DEFAULT 0 COMMENT '签到天数',
    MODIFY COLUMN total_sign_days INT NOT NULL DEFAULT 0 COMMENT '累计签到天数',
    MODIFY COLUMN today_sign_count INT NOT NULL DEFAULT 0 COMMENT '今日签到次数';
ALTER TABLE activity_account COMMENT = '活动账户';
ALTER TABLE activity_account_prize COMMENT = '活动账户奖品';
ALTER TABLE activity_info COMMENT = '活动信息';
ALTER TABLE activity_partner COMMENT = '活动合作方';
ALTER TABLE activity_prize
    MODIFY COLUMN kind CHAR(1) NOT NULL DEFAULT '1' COMMENT '1礼品 2现金 3抽奖次数',
    COMMENT = '活动奖品';
ALTER TABLE points_account COMMENT = '积分账户';
ALTER TABLE points_flow COMMENT = '积分流水';
ALTER TABLE points_gift
    MODIFY COLUMN kind CHAR(1) NOT NULL DEFAULT '1' COMMENT '1礼品 2现金',
    COMMENT = '积分礼品';
ALTER TABLE points_gift_order
    MODIFY COLUMN status CHAR(1) NOT NULL DEFAULT '1' COMMENT '1待发货 2已发货 3已收货 4已取消',
    COMMENT = '积分礼品订单';
ALTER TABLE website_customer COMMENT = '网站客户';
ALTER TABLE translations
    MODIFY COLUMN zh_CN TEXT COMMENT '中文(简体)',
    MODIFY COLUMN zh_TW TEXT COMMENT '中文(繁体)',
    MODIFY COLUMN ko_KR TEXT COMMENT '韩文',
    MODIFY COLUMN th_TH TEXT COMMENT '泰文',
    MODIFY COLUMN ja_JP TEXT COMMENT '日文',
    MODIFY COLUMN pt_PT TEXT COMMENT '葡萄牙语',
    MODIFY COLUMN en_US TEXT COMMENT '英文',
    MODIFY COLUMN ar_SA TEXT COMMENT '阿拉伯语',
    MODIFY COLUMN es_ES TEXT COMMENT '西班牙语',
    MODIFY COLUMN sv_SE TEXT COMMENT '瑞典语',
    MODIFY COLUMN it_IT TEXT COMMENT '意大利语',
    MODIFY COLUMN de_DE TEXT COMMENT '德语',
    MODIFY COLUMN no_NO TEXT COMMENT '挪威语',
    MODIFY COLUMN ru_RU TEXT COMMENT '俄语',
    MODIFY COLUMN hu_HU TEXT COMMENT '匈牙利语',
    MODIFY COLUMN pl_PL TEXT COMMENT '波兰语',
    MODIFY COLUMN sk_SK TEXT COMMENT '斯洛伐克语',
    MODIFY COLUMN fr_FR TEXT COMMENT '法语',
    MODIFY COLUMN cs_CZ TEXT COMMENT '捷克语',
    MODIFY COLUMN pt_BR TEXT COMMENT '巴西葡萄牙语',
    MODIFY COLUMN hi_IN TEXT COMMENT '印地语',
    COMMENT = '多语言翻译';
UPDATE order_config
SET name = CASE type
    WHEN 'error' THEN '错误代码'
    WHEN 'backendRateLimit' THEN '后端限流设置'
    WHEN 'frontendRateLimit' THEN '前端限流设置'
    ELSE name
END
WHERE type IN ('error', 'backendRateLimit', 'frontendRateLimit');
UPDATE sys_menu
SET remark = CASE menu_id
    WHEN 4000 THEN '积分商城目录'
    WHEN 4001 THEN '礼品管理目录'
    WHEN 4002 THEN '礼品管理菜单'
    WHEN 4003 THEN '积分账户目录'
    WHEN 4004 THEN '积分账户菜单'
    WHEN 4005 THEN '积分流水菜单'
    WHEN 4006 THEN '积分订单目录'
    WHEN 4007 THEN '积分订单菜单'
    WHEN 4100 THEN '活动管理目录'
    WHEN 4101 THEN '活动管理目录'
    WHEN 4102 THEN '活动管理菜单'
    WHEN 4103 THEN '奖品管理菜单'
    WHEN 4104 THEN '活动账户目录'
    WHEN 4105 THEN '活动账户菜单'
    WHEN 4106 THEN '参与记录菜单'
    WHEN 4200 THEN '官网管理目录'
    WHEN 4201 THEN '官网管理目录'
    WHEN 4202 THEN '客户管理菜单'
    ELSE remark
END
WHERE menu_id IN (
    4000, 4001, 4002, 4003, 4004, 4005, 4006, 4007,
    4100, 4101, 4102, 4103, 4104, 4105, 4106,
    4200, 4201, 4202
);

SELECT COUNT(*) AS goods_count FROM goods;
SELECT COUNT(*) AS goods_type_count FROM goods_type;
SELECT COUNT(*) AS missing_image_url_count
FROM goods
WHERE image IS NULL OR image = '';
`;

  const applySqlPath = resolve(migrationDir, "apply-old-goods.sql");
  await writeFile(applySqlPath, sql, "utf8");
  return applySqlPath;
}

let source;

if (resumeSource) {
  const snapshotPath = resolve(migrationDir, "old-goods-source.jsonl");
  const categorySnapshotPath = resolve(
    migrationDir,
    "old-goods-types.tsv",
  );
  const snapshot = await readFile(snapshotPath, "utf8");
  const categorySnapshot = await readFile(categorySnapshotPath, "utf8");
  const goods = snapshot
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => JSON.parse(line));
  const categories = categorySnapshot
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => {
      const fields = line.split("\t");
      if (fields.length !== 9) {
        throw new Error(
          `Invalid category snapshot row with ${fields.length} fields`,
        );
      }
      return JSON.parse(decodeMysqlTsvValue(fields[8]));
    });
  source = { categories, goods, total: goods.length };
  log(`Loaded ${goods.length} products from the local source snapshot`);
} else {
  await authenticate();
  source = await fetchSourceData();
}

const prepared = prepareData(source);
const files = await writeMigrationFiles(source, prepared);
log(`Source validation: ${JSON.stringify(prepared.stats)}`);

if (prepared.missingImages.length) {
  throw new Error(
    `${prepared.missingImages.length} products have no usable image URL; `
    + "database migration was not generated",
  );
}

let imageResult = {
  total: prepared.imageByUrl.size,
  downloaded: 0,
  skipped: 0,
  failed: 0,
};

if (!skipDownload) {
  imageResult = await downloadImages(prepared.imageByUrl);
  if (imageResult.failed) {
    throw new Error(
      `${imageResult.failed} images failed to download; `
      + "database migration was not generated",
    );
  }
} else {
  log("Image download skipped by --skip-download");
}

const applySqlPath = await writeApplySql(files, prepared);
const finalManifest = {
  ...files.manifest,
  imageDownload: imageResult,
  imageDirectory: imageDir,
  applySqlPath,
};
await writeFile(
  files.manifestPath,
  `${JSON.stringify(finalManifest, null, 2)}\n`,
  "utf8",
);

log(`Migration files are ready: ${migrationDir}`);
log(`Apply SQL: ${applySqlPath}`);
