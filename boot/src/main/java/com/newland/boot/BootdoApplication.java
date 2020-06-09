package com.newland.boot;

import com.newland.boot.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAutoConfiguration(
   exclude = {SecurityAutoConfiguration.class}
)
@EnableTransactionManagement
@ServletComponentScan
@MapperScan({"com.newland.*.dao"})
@SpringBootApplication
public class BootdoApplication implements ApplicationRunner {
   public static List<String> preCodes = new ArrayList();
   public static List<String> preBzCodes = new ArrayList();
   @Autowired
   private AutoService autoService;

   public static void main(String[] args) {
      SpringApplication.run(BootdoApplication.class, args);
      System.out.println("ヾ(◍°∇°◍)ﾉﾞ    bootdo启动成功      ヾ(◍°∇°◍)ﾉﾞ\n ______                    _   ______            \n|_   _ \\                  / |_|_   _ `.          \n  | |_) |   .--.    .--. `| |-' | | `. \\  .--.   \n  |  __'. / .'`\\ \\/ .'`\\ \\| |   | |  | |/ .'`\\ \\ \n _| |__) || \\__. || \\__. || |, _| |_.' /| \\__. | \n|_______/  '.__.'  '.__.' \\__/|______.'  '.__.'  ");
   }

   public void run(ApplicationArguments arg0) throws Exception {
      System.out.println("接入初始化线程,获取数据库列表");
      preCodes = this.autoService.getPreCodes();
      preBzCodes = this.autoService.preBzCodes();
      System.out.println("获取数据库结束");
   }
}
