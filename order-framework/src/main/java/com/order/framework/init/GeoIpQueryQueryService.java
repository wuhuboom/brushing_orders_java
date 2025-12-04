package com.order.framework.init;


import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import com.order.common.config.OrderConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

@Service
public class GeoIpQueryQueryService {

    private DatabaseReader reader;

    private final String mmdbFileName = "GeoLite2-Country.mmdb";


    @PostConstruct
    public void init() {
        String localPath = OrderConfig.getProfile(); // 从 BrushingConfig 获取目录
        File externalFile = new File(localPath + "/" + mmdbFileName);
        if (externalFile.exists()) {
            try {
                reader = new DatabaseReader.Builder(externalFile).build();
                System.out.println("GeoIP 数据库加载成功（外部路径）: " + externalFile.getAbsolutePath());
                return;
            } catch (IOException e) {
                System.err.println("外部文件加载失败: " + e.getMessage());
            }
        } else {
            System.out.println("外部文件不存在，跳过加载: " + externalFile.getAbsolutePath());
        }

        // 备选：从 JAR 资源加载
        String currentDirectory = System.getProperty("user.dir");
        String filePath = currentDirectory + "/" + mmdbFileName;  // 拼接文件路径

        // 创建文件对象
        File externaFile = new File(filePath);
        // 检查文件是否存在
        if (externaFile.exists()) {
            try {
                reader = new DatabaseReader.Builder(externaFile).build();
                System.out.println("GeoIP 数据库加载成功（JAR 同级目录）: " + externaFile.getAbsolutePath());
                return;
            } catch (IOException e) {
                System.err.println("外部文件加载失败: " + e.getMessage());
            }
        } else {
            System.out.println("文件不存在，跳过加载: " + externaFile.getAbsolutePath());
        }

        // 如果没有找到文件，则设置 reader 为 null
        reader = null;
        System.out.println("GeoIP 数据库未加载（无可用文件）");
    }

    // 通用查询方法：使用 GeoIP2 API
    public String queryByIp(String ipAddress) {  // 方法示例，返回中文国家名
        if (reader == null) {
            return "未知";  // 未加载数据库
        }

        try {
            InetAddress ipAddr = InetAddress.getByName(ipAddress);
            CountryResponse response = reader.country(ipAddr);  // CountryResponse 只含国家

            // 解析国家
            Country country = response.getCountry();
            if (country == null || country.getIsoCode() == null) {
                return "未知";
            }

            // 提取中文国家名（优先 zh-CN）
            Map<String, String> names = country.getNames();
            String chineseName = names != null ? names.get("zh-CN") : null;

            // Fallback：如果无 zh-CN，用英文或 ISO
            if (chineseName == null) {
                chineseName = country.getName();  // 英文名作为备选
            }
            if (chineseName == null) {
                chineseName = country.getIsoCode();  // 最后 ISO 代码
            }

            return chineseName != null ? chineseName : "未知";

        } catch (IOException | GeoIp2Exception e) {
            System.err.println("查询失败: " + e.getMessage());
            return "未知";
        }
    }

}
