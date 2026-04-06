package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 这个配置对应与application.yaml中的sky.alioss配置
 * 注意：application.yaml中的属性名使用 小写+连字符（kebab-case），Spring Boot 会自动映射到驼峰字段。
 */
@Component
@ConfigurationProperties(prefix = "sky.alioss")
@Data
public class AliOssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

}
