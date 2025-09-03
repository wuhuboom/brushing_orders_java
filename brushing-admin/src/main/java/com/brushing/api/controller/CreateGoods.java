package com.brushing.api.controller;

import com.brushing.common.utils.DateUtils;
import com.brushing.member.domain.OrderGoods;
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

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

@RestController
@RequestMapping("/api/creategoodes")
public class CreateGoods {

    @Autowired
    private IOrderGoodsService orderGoodsService;

    @GetMapping("/create")
    public void create() throws Exception{
        String baseUrl = "https://hotel-test.xs1jkhau.com/api/admin/goods";
        String token = "Bearer eyJ0eXAiOiJqd3QifQ.eyJzdWIiOiIxIiwiaXNzIjoiaHR0cDpcL1wvOiIsImV4cCI6MTc1NjkxODMyNCwiaWF0IjoxNzU2MzEzNTI0LCJuYmYiOjE3NTYzMTM1MjQsInVpZCI6MSwicyI6IlNHekw0VSIsImp0aSI6IjU1Mjk5M2RjNjQ3M2Y5NTEzMzRhZjM4MmNiYTMzNTA5In0.OTZkM2YyZDA4YzE4ZGFhYWE5NzU2ZDY1YWMyN2RlYThhZmZhOTMxYg";

        HttpClient client = HttpClients.createDefault();
        ObjectMapper mapper = new ObjectMapper();

        int page = 1;
        int limit = 30;

        File dir = new File("D:/file/img/");
        if (!dir.exists()) dir.mkdirs();

        while (true) {
            String url = baseUrl + "?page=" + page + "&limit=" + limit;
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", token);

            ClassicHttpResponse response = (ClassicHttpResponse) client.execute(request);
            if (response.getCode() != 200) {
                System.out.println("请求失败: " + response.getCode());
                break;
            }

            // Step 1: 读取返回的原始字符串
            String raw = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

            String urlDecoded = URLDecoder.decode(raw, StandardCharsets.UTF_8.name());

            byte[] base64Decoded = Base64.getDecoder().decode(urlDecoded);
            String jsonStr = new String(base64Decoded, StandardCharsets.UTF_8);
            String jsonString = URLDecoder.decode(jsonStr, StandardCharsets.UTF_8.name());

            ObjectMapper mapperd= new ObjectMapper();
            // Step 3: 用 Jackson 解析
            JsonNode root = mapperd.readTree(jsonString);



            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.size() == 0) {
                System.out.println("没有更多数据，结束。");
                break;
            }

            // 4. 下载图片
            for (JsonNode item : data) {
                String imageUrl = item.get("cover").asText(); // 注意字段名可能是 cover/coverImage
                        //图片地址
                        String fileName = "/profile/upload/2025/hotel/"+imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                        OrderGoods goods=new OrderGoods();
                        goods.setCoverUrl(fileName);
                        goods.setDescription(item.get("describe").asText());
                        goods.setName(item.get("name").asText());
                        goods.setStatus("0");
                        goods.setTypeId(1L);
                        goods.setPrice(new BigDecimal(item.get("price").asText()));
                        goods.setCreateTime(DateUtils.getNowDate());
                        orderGoodsService.insertOrderGoods(goods);
            }

            page++; // 下一页
        }

        System.out.println("所有图片下载完成！");
    }

}
