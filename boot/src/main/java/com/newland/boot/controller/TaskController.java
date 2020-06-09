package com.newland.boot.controller;

import com.alibaba.fastjson.JSONArray;
import com.newland.boot.BootdoApplication;
import com.newland.boot.domain.ProDetails;
import com.newland.boot.service.AutoService;
import com.newland.boot.utils.PageUtils;
import com.newland.boot.utils.TemplateUtils;
import com.newland.boot.utils.zipcompress;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Component
@Transactional(
   rollbackFor = {Exception.class}
)
@ConfigurationProperties(
   prefix = "bootdo"
)
@PropertySource({"application-pro.yml"})
@Controller
public class TaskController {
   @Value("${bootdo.uploadPath}")
   String uploadPath;
   @Value("${bootdo.templatePath}")
   String templatePath;
   @Value("${bootdo.generatePath}")
   String generatePath;
   ThreadLocal<String> timeDir = new ThreadLocal();
   //结果展示信息
   ThreadLocal<Map<String, ProDetails>> proDetailList = new ThreadLocal();
   //缓存产品列表
   ThreadLocal<List<String>> preProCodeList = new ThreadLocal();
   @Autowired
   private AutoService autoService;

   @RequestMapping({"/"})
   public String homePage() {
      return "index";
   }

   @RequestMapping({"/toGenerate"})
   public String autoPage() {
      return "autoTest/normal/generate";
   }

   @RequestMapping({"/toGeneratePage"})
   public String toGeneratePage() {
      return "autoTest/normal/generateDetail";
   }

   @RequestMapping({"/getFile"})
   @ResponseBody
   public JSONObject getFile(@RequestParam("file") MultipartFile file) throws JSONException {
      this.preProCodeList.set(BootdoApplication.preCodes);
      Map<String, ProDetails> detailMap = new HashMap();
      this.proDetailList.set(detailMap);
      JSONObject res = new JSONObject();
      String fileName = file.getOriginalFilename();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
      this.timeDir.set(sdf.format(Calendar.getInstance().getTime()));
      new File(this.uploadPath + fileName);
      res.put("code", 0);
      if (file.isEmpty()) {
         res.put("msg", "文件内容为空");
         return res;
      } else {
         try {
            System.out.println("开始读取文件-------");
            long startT = System.currentTimeMillis();
            boolean dealResult = this.getProCode(file);
            if (dealResult) {
               File proFile = new File(this.generatePath + (String)this.timeDir.get() + "/");
               File[] listFiles = proFile.listFiles();
               if (listFiles != null && listFiles.length > 0) {
                  zipcompress.zipCompress(this.generatePath + (String)this.timeDir.get() + ".zip", proFile);
                  res.put("data", this.generatePath + (String)this.timeDir.get() + ".zip");
               } else {
                  res.put("data", "产品编码问题、未生成文件！");
               }

               res.put("msg", "生成成功");
            } else {
               res.put("msg", "生成失败");
               res.put("data", "生成失败");
            }

            List<ProDetails> detailList = new ArrayList();
            Iterator var15 = ((Map)this.proDetailList.get()).keySet().iterator();

            while(var15.hasNext()) {
               String key = (String)var15.next();
               detailList.add((this.proDetailList.get()).get(key));
            }

            System.out.println("转换成json字符串:" + JSONArray.toJSON(detailList).toString());
            res.put("detailList", JSONArray.toJSON(detailList).toString());
            long endT = System.currentTimeMillis();
            System.out.println("生成结束------消耗时间是:" + (endT - startT) + "ms");
            return res;
         } catch (IOException var13) {
            System.out.println(var13);
            return res;
         }
      }
   }

   @RequestMapping({"/download"})
   public void download(HttpServletRequest request, HttpServletResponse response, @RequestParam String path) {
      File file = new File(path);
      if (file.exists()) {
         ByteArrayOutputStream bos = new ByteArrayOutputStream((int)file.length());
         BufferedInputStream in = null;
         Object var7 = null;

         try {
            in = new BufferedInputStream(new FileInputStream(file));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            boolean var10 = false;

            int len;
            while(-1 != (len = in.read(buffer, 0, buf_size))) {
               bos.write(buffer, 0, len);
            }

            byte[] data = bos.toByteArray();
            response.reset();
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            response.addHeader("Content-Length", "" + data.length);
            response.setContentType("application/octet-stream; charset=UTF-8");
            IOUtils.write(data, response.getOutputStream());
         } catch (IOException var19) {
            var19.printStackTrace();
         } finally {
            try {
               in.close();
               bos.close();
            } catch (IOException var18) {
               var18.printStackTrace();
            }

         }

      }
   }

   @ResponseBody
   @GetMapping({"/getDetailList"})
   public PageUtils getDetailList(@RequestParam Map<String, Object> params) {
      List<ProDetails> detailList = new ArrayList();
      List<ProDetails> pageList = new ArrayList();
      Iterator var4 = ((Map)this.proDetailList.get()).keySet().iterator();

      while(var4.hasNext()) {
         String key = (String)var4.next();
         detailList.add(this.proDetailList.get().get(key));
      }

      for(int i = 0; i < Integer.parseInt(params.get("limit").toString()); ++i) {
         if (Integer.parseInt(params.get("offset").toString() + i) <= detailList.size() - 1) {
            pageList.add(detailList.get(Integer.parseInt(params.get("offset").toString() + i)));
         }
      }

      System.out.println("limit:" + params.get("limit").toString());
      System.out.println("offset:" + params.get("offset").toString());
      System.out.println("pageList.size():" + pageList.size());
      System.out.println("pageList.get(0).toString():" + ((ProDetails)pageList.get(0)).toString());
      PageUtils pageUtils = new PageUtils(pageList, pageList.size());
      return pageUtils;
   }

   private boolean getProCode(MultipartFile file) throws IOException {
      Reader reader = null;
      reader = new InputStreamReader(file.getInputStream(), "utf-8");
      BufferedReader br = new BufferedReader(reader);

      String line;
      ArrayList proCodes;
      ProDetails prodetail;
      for(proCodes = new ArrayList(); (line = br.readLine()) != null; ((Map)this.proDetailList.get()).put(line, prodetail)) {
         prodetail = new ProDetails();
         prodetail.setStatus(1);
         prodetail.setDealMsg("生成成功！");
         line = line.trim();
         if (line != null && line != "" && line.matches("\\d{10}")) {
            if (!proCodes.contains(line)) {
               proCodes.add(line);
               prodetail.setProcode(line);
            }
         } else {
            prodetail.setStatus(0);
            prodetail.setDealMsg("产品编码格式错误！请输入0-9的10位数字！");
         }
      }

      System.out.println("开始读取数据库内的产品列表------");
      long startT = System.currentTimeMillis();
      long endT = System.currentTimeMillis();
      System.out.println("读取数据库内的产品列表结束------消耗时间是:" + (endT - startT) + "ms");
      reader.close();
      System.out.println("proCodes:" + proCodes.size());
      if (!proCodes.isEmpty()) {
         Iterator var10 = proCodes.iterator();

         while(var10.hasNext()) {
            String code = (String)var10.next();
            if (!((List)this.preProCodeList.get()).contains(code)) {
               System.out.println("产品编码:" + code);
               this.preUserData(code);
            } else {
               ProDetails detail = (ProDetails)((Map)this.proDetailList.get()).get(code);
               detail.setStatus(0);
               detail.setDealMsg("产品编码在数据内重复，暂不生成用例！");
               ((Map)this.proDetailList.get()).put(code, detail);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   /**
    *@autor: litao
    *@Description: 准备用户资料
    *@date: 2020/6/4
    *@time: 16:18
    *@methodName: preUserData
    *@Param: [code]
    *@returnType: void
   */
   private void preUserData(String code) {
      System.out.println("开始查询" + code + "用户关系类型");
      List<Integer> feeSources = this.autoService.getFeeSource(code + "0");
      List<String> bizDomain = this.autoService.getBizDomain(code + "0");
      Integer feeSource;
      ProDetails detail;
      if (feeSources.size() > 1) {
         feeSource = 1;
      } else {
         if (feeSources.size() != 1) {
            detail = (ProDetails)((Map)this.proDetailList.get()).get(code);
            detail.setStatus(0);
            detail.setDealMsg("未查到用户关系类型，暂不生成用例！");
            ((Map)this.proDetailList.get()).put(code, detail);
            return;
         }

         feeSource = (Integer)feeSources.get(0);
      }

      if (this.dealBizDomain(bizDomain)) {
         detail = (ProDetails)((Map)this.proDetailList.get()).get(code);
         detail.setStatus(0);
         detail.setDealMsg("只有固定费套餐，暂不生成用例！");
         ((Map)this.proDetailList.get()).put(code, detail);
      } else {
         System.out.println("查询" + code + "用户关系类型是" + feeSource);
         System.out.println("查询" + code + "用户关系类型结束");
         HashMap<String, Object> param = new HashMap();
         System.out.println("查询" + code + "bizDomain是" + bizDomain);
         BootdoApplication.preCodes.add(code);
         param.put("proCode", code);
         param.put("feeSource", feeSource);
         param.put("userNum", bizDomain.size() * 3);
         param.put("outPut", (Object)null);
         this.autoService.creatData(param);
         //事务回滚
         TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
         String[] userData = param.get("outPut").toString().replaceAll("\r|\n", "").trim().substring(3).split("\\===");
         int index = 0;
         Iterator var8;
         String bz;
         String[] userId;
         String[] msisdn;
         String[] imsi;
         int i;
         long endT;
         long startT;
         if (feeSource == 1) {
            var8 = bizDomain.iterator();

            while(var8.hasNext()) {
               bz = (String)var8.next();
               userId = new String[3];
               msisdn = new String[3];
               imsi = new String[3];

               for(i = 0; i < 3; ++i) {
                  userId[i] = userData[index].substring(userData[index].indexOf("user_id:") + 9, userData[index].indexOf("msisdn :")).trim();
                  msisdn[i] = userData[index].substring(userData[index].indexOf("msisdn :") + 9, userData[index].indexOf("imsi:")).trim();
                  imsi[i] = userData[index].substring(userData[index].indexOf("imsi:") + 6, userData[index].indexOf("account_id:")).trim();
                  ++index;
                  if (index == userData.length) {
                     break;
                  }
               }

               System.out.println("开始生成文本文件------");
               startT = System.currentTimeMillis();
               this.genTxt(userId, msisdn, imsi, code, bz);
               endT = System.currentTimeMillis();
               System.out.println("生成文本文件结束------消耗时间是:" + (endT - startT) + "ms");
            }
         } else {
            var8 = bizDomain.iterator();

            while(var8.hasNext()) {
               bz = (String)var8.next();
               userId = new String[3];
               msisdn = new String[3];
               imsi = new String[3];

               for(i = 0; i < 3; ++i) {
                  userData[index] = userData[index].substring(userData[index].indexOf("个人用户"));
                  userId[i] = userData[index].substring(userData[index].indexOf("user_id:") + 9, userData[index].indexOf("msisdn :"));
                  msisdn[i] = userData[index].substring(userData[index].indexOf("msisdn :") + 9, userData[index].indexOf("imsi:"));
                  imsi[i] = userData[index].substring(userData[index].indexOf("imsi:") + 6, userData[index].indexOf("account_id:"));
                  if (index == userData.length) {
                     break;
                  }
               }

               System.out.println("开始生成文本文件------");
               startT = System.currentTimeMillis();
               this.genTxt(userId, msisdn, imsi, code, bz);
               endT = System.currentTimeMillis();
               System.out.println("生成文本文件结束------消耗时间是:" + (endT - startT) + "ms");
            }
         }

      }
   }

   private boolean dealBizDomain(List<String> bizDomain) {
      return bizDomain.size() == 1 && ((String)bizDomain.get(0)).equals("88,66");
   }

   /**
    *@autor: litao
    *@Description: 替换模板内容，生成文件内容
    *@date: 2020/5/19
    *@time: 16:11
    *@methodName: genTxt
    *@Param: [userId, msisdn, imsi, code, bz]
    *@returnType: void
   */
   private void genTxt(String[] userId, String[] msisdn, String[] imsi, String code, String bz) {
      if (bz.equals("3")) {
         System.out.println("开始写入文本文件------");
         long startT = System.currentTimeMillis();
         StringBuilder content = new StringBuilder(TemplateUtils.gprs);
         StringBuilder proCode = new StringBuilder(code);
         proCode.insert(8, ".");
         content = replaceAll(content, "#code#", proCode.toString());
         content = replaceAll(content, "#procode#", code);
         for(int i = 0; i < 3; ++i) {
            int flag = i + 1;
            content = replaceAll(content, "#imsi" + flag + "#", imsi[i]);
            content = replaceAll(content, "#msisdn" + flag + "#", msisdn[i]);
         }

         this.writeFile(content, code, "gprs");
         long endT = System.currentTimeMillis();
         System.out.println("写入文本文件结束------消耗时间是:" + (endT - startT) + "ms");
      }

      if (bz.equals("1")) {
         StringBuilder content = new StringBuilder(TemplateUtils.gsm);
         StringBuilder proCode = new StringBuilder(code);
         proCode.insert(8, ".");
         content = replaceAll(content, "#code#", proCode.toString());
         content = replaceAll(content, "#procode#", code);
         for(int i = 0; i < 3; ++i) {
            int flag = i + 1;
            content = replaceAll(content, "#imsi" + flag + "#", imsi[i]);
            content = replaceAll(content, "#msisdn" + flag + "#", msisdn[i]);
         }

         this.writeFile(content, code, "gsm");
      }

      if (bz.equals("2")) {
         StringBuilder content = new StringBuilder(TemplateUtils.sms);
         StringBuilder proCode = new StringBuilder(code);
         proCode.insert(8, ".");
         content = replaceAll(content, "#code#", proCode.toString());
         content = replaceAll(content, "#procode#", code);

         for(int i = 0; i < 3; ++i) {
            int flag = i + 1;
            content = replaceAll(content, "#msisdn" + flag + "#", msisdn[i]);
         }

         this.writeFile(content, code, "sms");
      }

      if (bz.equals("4")) {
         StringBuilder content = new StringBuilder(TemplateUtils.wlan);
         StringBuilder proCode = new StringBuilder(code);
         proCode.insert(8, ".");
         content = replaceAll(content, "#code#", proCode.toString());
         content = replaceAll(content, "#procode#", code);
         for(int i = 0; i < 3; ++i) {
            int flag = i + 1;
            content = replaceAll(content, "#msisdn" + flag + "#", msisdn[i]);
         }

         this.writeFile(content, code, "wlan");
      }

      if (bz.equals("H")) {
         StringBuilder content = new StringBuilder(TemplateUtils.net);
         StringBuilder proCode = new StringBuilder(code);
         proCode.insert(8, ".");
         content = replaceAll(content, "#code#", proCode.toString());
         content = replaceAll(content, "#procode#", code);
         for(int i = 0; i < 3; ++i) {
            int flag = i + 1;
            content = replaceAll(content, "#imsi" + flag + "#", imsi[i]);
            content = replaceAll(content, "#msisdn" + flag + "#", msisdn[i]);
         }

         this.writeFile(content, code, "net");
      }

   }

   /**
    *@autor: litao
    *@Description: //TODO 写入文件
    *@date: 2020/5/19
    *@time: 16:12
    *@methodName: writeFile
    *@Param: [content, code, bz]
    *@returnType: void
   */
   public void writeFile(StringBuilder content, String code, String bz) {
      try {
         File file = new File(this.generatePath + (String)this.timeDir.get() + "/" + code + "-" + bz + ".txt");
         File parentFile = file.getParentFile();
         if (!parentFile.exists()) {
            parentFile.mkdirs();
         }

         if (!file.exists()) {
            file.createNewFile();
         }

         FileWriter fw = new FileWriter(this.generatePath + (String)this.timeDir.get() + "/" + code + "-" + bz + ".txt");
         fw.write(content.toString());
         fw.flush();
         fw.close();
      } catch (IOException var7) {
         var7.printStackTrace();
      }

   }

   /**
    *@autor: litao
    *@Description: //TODO 替换方法
    *@date: 2020/5/19
    *@time: 16:13
    *@methodName: replaceAll
    *@Param: [stb, oldStr, newStr]
    *@returnType: java.lang.StringBuilder
   */
   public static StringBuilder replaceAll(StringBuilder stb, String oldStr, String newStr) {
      if (stb != null && oldStr != null && newStr != null && stb.length() != 0 && oldStr.length() != 0) {
         int index = stb.indexOf(oldStr);
         int lastIndex;
         if (index > -1 && !oldStr.equals(newStr)) {
            for(boolean var4 = false; index > -1; index = stb.indexOf(oldStr, lastIndex)) {
               stb.replace(index, index + oldStr.length(), newStr);
               lastIndex = index + newStr.length();
            }
         }
         return stb;
      } else {
         return stb;
      }
   }
}
