package cn.jmi.openatom.sitepublish;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@MapperScan("cn.jmi.openatom.sitepublish.mapper")
@ConfigurationPropertiesScan
@SpringBootApplication
public class SitePublishApplication {

    public static void main(String[] args) {
        SpringApplication.run(SitePublishApplication.class, args);
    }
}
