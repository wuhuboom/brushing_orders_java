package com.brushing.api.controller;

import com.brushing.common.utils.DateUtils;
import com.brushing.member.domain.Goods;
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.service.IGoodsService;
import com.brushing.member.service.IOrderGoodsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/creategoodes")
public class CreateGoods {

    @Autowired
    private IGoodsService goodsService;

    @GetMapping("/create")
    public void create() throws Exception {
        String baseUrl = "https://dtdfor4kvg-20250807.com/backend/marketing/spus/page";
        String authToken = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI1MDM1NDM3MjlkMjM0OWE0OTk5ZGZlYWM5NDJlMGMwNyIsInN1YiI6ImJhY2tlbmQiLCJpc3MiOiIxMDIiLCJhdWQiOiIxMDQiLCJpYXQiOjE3NjQ4MzczMDYsImtleSI6IndvdGxrOmJhY2tlbmQ6dGs6MTA0OmJhY2tlbmQifQ.MqVEYFS2qciLazcoW_UeIjD7KuNSJMX8KH31pDD96e4"; // 从请求头中提取的Authorization值（非Bearer类型，直接用）

        CloseableHttpClient client = HttpClients.createDefault();
        ObjectMapper mapper = new ObjectMapper();

        int current = 686;
        int pageSize = 20;
        int total = 44544; // 从响应中获取total，但初始设为大值，实际循环中更新
        long processed = current * pageSize; // 已处理记录数

        // 修改本地保存目录为 /profile/upload/2025/hotel/ 的绝对路径
        String localImageDir = "D:/profile/upload/2025/goods"; // 假设D盘为根目录，根据实际调整
        File dir = new File(localImageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 图片路径前缀，用于设置Goods.image字段
        String imagePathPrefix = "/profile/upload/2025/goods/";

        try {
            while (processed < total) {
                // 构建URL参数：current作为页码，pageSize=20，排序固定00
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("?current=").append(current)
                        .append("&pageSize=").append(pageSize)
                        .append("&pageNo=").append(current) // 根据示例，pageNo与current相同
                        .append("&orders[0].field=seq")
                        .append("&orders[0].direction=ASC");

                String url = baseUrl + queryBuilder.toString();
                HttpGet request = new HttpGet(url);

                // 设置请求头
                request.setHeader("authority", "dtdfor4kvg-20250807.com");
                request.setHeader("accept", "*/*");
                request.setHeader("accept-encoding", "gzip, deflate, br, zstd");
                request.setHeader("accept-language", "zh-CN,zh;q=0.9");
                request.setHeader("authorization", authToken); // 直接设置JWT token
                request.setHeader("priority", "u=1, i");
                request.setHeader("referer", "https://dtdfor4kvg-20250807.com/marketing/spu/spus");
                request.setHeader("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"");
                request.setHeader("sec-ch-ua-mobile", "?0");
                request.setHeader("sec-ch-ua-platform", "\"Windows\"");
                request.setHeader("sec-fetch-dest", "empty");
                request.setHeader("sec-fetch-mode", "cors");
                request.setHeader("sec-fetch-site", "same-origin");
                request.setHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
                request.setHeader("x-app-code", "app");
                request.setHeader("x-channel-code", "backend");
                request.setHeader("x-device-id", "d1166b424357f418cc747c981882a973");
                request.setHeader("x-locale", "zh-CN");

                CloseableHttpResponse response = client.execute(request);
                if (response.getCode() != 200) {
                    System.out.println("请求失败，第" + current + "页: " + response.getCode());
                    break;
                }

                // 读取响应（假设是标准JSON，无需base64或URL解码，根据你的示例响应）
                String jsonStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JsonNode root = mapper.readTree(jsonStr);

                if (root.get("status").asInt() != 200) {
                    System.out.println("API返回非200状态: " + root.get("msg").asText());
                    break;
                }

                JsonNode dataNode = root.get("data");
                JsonNode rows = dataNode.get("rows");
                total = dataNode.get("total").asInt(); // 更新total

                if (rows == null || !rows.isArray() || rows.size() == 0) {
                    System.out.println("没有更多数据，结束。当前页: " + current);
                    break;
                }

                // 处理当前页数据
                for (JsonNode item : rows) {
                    Goods goods = new Goods();
                    goods.setTitle(item.get("title").asText());
                    goods.setSubTitle(item.get("subtitle").asText());

                    // 处理图片：提取文件名，设置本地路径到Goods.image，并下载
                    String picUrl = item.get("pic").asText();
                    String fileName = picUrl.substring(picUrl.lastIndexOf('/') + 1);
                    String localImagePath = imagePathPrefix + fileName;
                    goods.setImage(localImagePath);

                    goods.setPrice(new BigDecimal(item.get("price").asDouble()));
                    goods.setSerialNumber(item.get("seq").asLong());
                    goods.setIsEnabled(item.get("enabled").asText()); // "1" 表示启用
                    goods.setTypeId(Long.valueOf(item.get("categoryId").asText())); // "1" -> 1L

                    // 解析param
                    JsonNode param = item.get("param");
                    if (param != null && !param.isNull()) {
                        goods.setRating(new BigDecimal(param.get("rate").asDouble()));
                        goods.setStarRating(param.get("star").asInt());
                        goods.setQuantity(param.get("quantity").asInt());
                        goods.setUnitPrice(new BigDecimal(param.get("unitPrice").asDouble()));
                        if (param.has("description")) {
                            goods.setDescription(param.get("description").asText());
                        }
                        // points忽略或根据需要处理
                    }

                    // 插入数据库
                    goodsService.insertGoods(goods);
                    processed++;

                    // 下载图片到本地目录
                    if (picUrl != null && !picUrl.isEmpty()) {
                        File imageFile = new File(dir, fileName);
                        if (!imageFile.exists()) {
                            downloadImage(picUrl, imageFile);
                        }
                    }
                }

                System.out.println("已处理第" + current + "页，共" + rows.size() + "条记录。总进度: " + processed + "/" + total);
                current++;

                // 避免请求过快，可添加延时
                Thread.sleep(100); // 100ms延时
            }
        } finally {
            client.close();
        }

        System.out.println("所有数据拉取完成！总计: " + processed + "条记录。");
    }

    // 下载图片的辅助方法
    private void downloadImage(String imageUrl, File outputFile) {
        try (InputStream in = new URL(imageUrl).openStream();
             OutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("图片下载完成: " + outputFile.getName());
        } catch (Exception e) {
            System.err.println("下载图片失败: " + imageUrl + ", 错误: " + e.getMessage());
        }
    }

}
