package com.newland.boot.controller;

import com.alibaba.fastjson.JSONArray;
import com.newland.boot.BootdoApplication;
import com.newland.boot.domain.ProDetails;
import com.newland.boot.service.AutoService;
import com.newland.boot.utils.PageUtils;
import com.newland.boot.utils.TemplateUtils;
import com.newland.boot.utils.zipcompress;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@ConfigurationProperties(
   prefix = "bootdo"
)
@PropertySource({"application-pro.yml"})
@Controller
public class TaskDomainController {
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

   @RequestMapping({"/toDomainGenerate"})
   public String autoPage() {
      return "autoTest/bizdom/generateBiz";
   }

   @RequestMapping({"/toDomainGeneratePage"})
   public String toGeneratePage() {
      return "autoTest/bizdom/generateBizDetail";
   }

   @RequestMapping({"/getDomFile"})
   @ResponseBody
   public JSONObject getDomFile(@RequestParam("file") MultipartFile file) throws JSONException {
      preProCodeList.set(BootdoApplication.preBzCodes);
      Map<String, ProDetails> detailMap = new HashMap();
      proDetailList.set(detailMap);
      JSONObject res = new JSONObject();
      String fileName = file.getOriginalFilename();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
      timeDir.set(sdf.format(Calendar.getInstance().getTime()));
      new File(uploadPath + fileName);
      res.put("code", 0);
      if (file.isEmpty()) {
         res.put("msg", "文件内容为空");
         return res;
      } else {
         try {
            System.out.println("开始读取文件-------");
            long startT = System.currentTimeMillis();
            boolean dealResult = getProCode(file);
            if (dealResult) {
               File proFile = new File(generatePath + (String)timeDir.get() + "/");
               File[] listFiles = proFile.listFiles();
               if (listFiles != null && listFiles.length > 0) {
                  zipcompress.zipCompress(generatePath + (String)timeDir.get() + ".zip", proFile);
                  res.put("data", generatePath + (String)timeDir.get() + ".zip");
               } else {
                  res.put("data", "产品编码问题、未生成文件！");
               }

               res.put("msg", "生成成功");
            } else {
               res.put("msg", "生成失败");
               res.put("data", "生成失败");
            }

            List<ProDetails> detailList = new ArrayList();
            Iterator var15 = ((Map)proDetailList.get()).keySet().iterator();

            while(var15.hasNext()) {
               String key = (String)var15.next();
               detailList.add((proDetailList.get()).get(key));
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

   @ResponseBody
   @GetMapping({"/getDomainDetailList"})
   public PageUtils getDetailList(@RequestParam Map<String, Object> params) {
      List<ProDetails> detailList = new ArrayList();
      List<ProDetails> pageList = new ArrayList();
      Iterator var4 = ((Map)proDetailList.get()).keySet().iterator();

      while(var4.hasNext()) {
         String key = (String)var4.next();
         detailList.add(proDetailList.get().get(key));
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
      for(proCodes = new ArrayList(); (line = br.readLine()) != null; ((Map)proDetailList.get()).put(line, prodetail)) {
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

      //保存生成减免数据
      StringBuilder decprdStr = new StringBuilder();
      //保存生成月末非减免数据
      StringBuilder normalStr = new StringBuilder();
      //保存生成月中非减免数据
      StringBuilder normalEndStr = new StringBuilder();

      decprdStr.append("\t");
      normalStr.append("\t");
      normalEndStr.append("\t");

      if (!proCodes.isEmpty()) {
         Iterator var10 = proCodes.iterator();
         while(var10.hasNext()) {
            String code = (String)var10.next();
            if (!((List)preProCodeList.get()).contains(code)) {
               System.out.println("产品编码:" + code);
               preUserData(code,decprdStr,normalStr,normalEndStr);
               BootdoApplication.preBzCodes.add(code);
            } else {
               ProDetails detail = (ProDetails)((Map)proDetailList.get()).get(code);
               detail.setStatus(0);
               detail.setDealMsg("产品编码在数据内重复，暂不生成用例！");
               ((Map)proDetailList.get()).put(code, detail);
            }
         }
         genTxt(decprdStr,normalStr,normalEndStr);
         return true;
      } else {
         return false;
      }
   }


   /**
    *@autor: litao
    *@Description: 准备用户资料数据
    *@date: 2020/6/3
    *@time: 15:55
    *@methodName: preUserData
    *@Param: [code]
    *@returnType: void
   */
   private void preUserData(String code, StringBuilder decprdStr, StringBuilder normalStr, StringBuilder normalEndStr) {
      System.out.println("开始查询" + code + "用户关系类型");
      List<Integer> feeSources = autoService.getFeeSource(code + "0");
      List<Integer> feeTargets = autoService.getFeeTarget(code + "0");
      List<String> bizDomain = autoService.getBizDomain(code + "0");
      //判断月中月末标记
      int flag = 0;
      Integer feeSource = 1;
      Integer feeTarget = 1;
      Integer relationType = 1;
      ProDetails detail;
      if (feeSources.size() > 1) {
         feeSource = 1;
      } else {
         if (feeSources.size() != 1) {
            detail = (ProDetails) ((Map) proDetailList.get()).get(code);
            detail.setStatus(0);
            detail.setDealMsg("未查到用户关系类型，暂不生成用例！");
            ((Map) proDetailList.get()).put(code, detail);
            return;
         }
         feeSource = (Integer) feeSources.get(0);
         feeTarget = (Integer) feeTargets.get(0);
      }

      if(feeSource == 1 && feeTarget == 1){
         relationType = 1;
      }
      else if(feeSource == 2 && feeTarget == 2){
         relationType = 2;
      }
      else if(feeSource == 1 && feeTarget == 2){
         relationType = 5;
      }
      else if(feeSource == 1 && feeTarget == 7){
         relationType = 5;
      }
      else if(feeSource == 2 && feeTarget == 7){
         relationType = 2;
      }
      else{
         detail = (ProDetails) ((Map) proDetailList.get()).get(code);
         detail.setStatus(0);
         detail.setDealMsg("用户关系类型不支持，暂不生成用例！");
         ((Map) proDetailList.get()).put(code, detail);
         return;
      }

      if (dealBizDomain(bizDomain)) {
         detail = (ProDetails) ((Map) proDetailList.get()).get(code);
         detail.setStatus(0);
         detail.setDealMsg("不存在固定费套餐，暂不生成用例！");
         ((Map) proDetailList.get()).put(code, detail);
      } else {
         System.out.println("查询" + code + "用户关系类型是" + feeSource);
         System.out.println("查询" + code + "用户关系类型结束");
         System.out.println("查询" + code + "bizDomain是" + bizDomain);
         //存储过程参数（创建资料）
         HashMap<String, Object> dataParam = new HashMap();
         dataParam.put("proCode", code);
         dataParam.put("feeSource", relationType);
         //dataParam.put("userNum", bizDomain.size() * 3);
         dataParam.put("outPut", (Object) null);
         //创建固定费用户资料
         autoService.creatBizData(dataParam);
         //事务回滚
         //TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
         String userdata = dataParam.get("outPut").toString();
         System.out.println("userdata:" + userdata);
         Long userId = Long.valueOf(userdata.substring(userdata.indexOf("用户user_id: ") + 11, userdata.indexOf("用户user_id:") + 27));
         //存储过程参数（获取数据）
         HashMap<String, Object> proParam = new HashMap();
         proParam.put("proCode", code);
         proParam.put("userId", 0);
         proParam.put("i_out_decprd_info", (Object) null);
         proParam.put("i_out_nomal_info", (Object) null);
         proParam.put("out_errsm", (Object) null);
         //获取固定费用户资料
         autoService.getBizData(proParam);
         System.out.println("i_out_decprd_info是" + proParam.get("i_out_decprd_info"));
         System.out.println("i_out_nomal_info" + proParam.get("i_out_nomal_info"));
         System.out.println("out_errsm" + proParam.get("out_errsm"));
         if(!proParam.get("out_errsm").equals("0")){
            detail = (ProDetails) ((Map) proDetailList.get()).get(code);
            detail.setStatus(0);
            detail.setDealMsg(proParam.get("out_errsm").toString());
            return ;
         }
         //减免数据
         if (proParam.get("i_out_decprd_info") != null) {
            String decprdData = proParam.get("i_out_decprd_info").toString().trim();
            //根据冒号分割每一条数据
            String[] decprdDataSp = decprdData.split("\\:");
            for (int i = 0; i < decprdDataSp.length; i++){
               //根据逗号分割信息
               String [] decprdmess = decprdDataSp[i].split("\\,");
               if(!decprdmess[2].equals(0)){
                  for(int j = 0; j < decprdmess.length; j++){
                     decprdStr.append(decprdmess[j]).append("\t");
                  }
                  decprdStr.append("${P_monthfee_eday}    #zwt").append("\n");
               }
            }
         }
         //非减免数据
         if (proParam.get("i_out_nomal_info") != null) {
            String nomalData = proParam.get("i_out_nomal_info").toString().trim();
            //根据冒号分割每一条数据
            String[] normalDataSp = nomalData.split("\\:");
            for (int i = 0; i < normalDataSp.length; i++){
               //根据逗号分割信息
               String [] normalMess = normalDataSp[i].split("\\,");
               //费用不为0时生成用例
               if(!normalMess[6].equals("0")||!normalMess[7].equals("0")) {
                  //费用类型为5或6时同时生成月中月末数据
                  if (normalMess[4].equals("5") || normalMess[4].equals("6")) {
                     //月中数据
                     for (int j = 0; j < normalMess.length-2; j++) {
                        normalStr.append(normalMess[j]).append("\t");
                     }
                     normalStr.append(normalMess[7]).append("\n");
                     normalStr.append("...    1    ${P_monthfee_bday}    #zwt").append("\n");
                     //月末数据
                     for (int j = 0; j < normalMess.length-2; j++) {
                        normalEndStr.append(normalMess[j]).append("\t");
                     }
                     normalEndStr.append(normalMess[6]).append("\n");
                     normalEndStr.append("...    1    ${P_monthfee_eday}    #zwt").append("\n");
                  }else{
                     //月末数据
                     for (int j = 0; j < normalMess.length-2; j++) {
                        normalEndStr.append(normalMess[j]).append("\t");
                     }
                     normalEndStr.append(normalMess[6]).append("\n");
                     normalEndStr.append("...    1    ${P_monthfee_eday}    #zwt").append("\n");

                  }
               }
            }
         }
      }
   }

  /**
   *@autor: litao
   *@Description: 判断是否有固定费套餐
   *@date: 2020/6/3
   *@time: 15:53
   *@methodName: dealBizDomain
   *@Param: [bizDomain]
   *@returnType: boolean
  */
   private boolean dealBizDomain(List<String> bizDomain) {
      return !bizDomain.contains("88,66");
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
   private void genTxt(StringBuilder decprdStr, StringBuilder normalStr, StringBuilder normalEndStr) throws IOException {
      StringBuilder content = new StringBuilder();
      StringBuilder contentEnd = new StringBuilder();
      FileReader freader = null;
      FileReader freaderEnd = null;
      BufferedReader reader = null;
      BufferedReader readerEnd = null;
      try {
         freader = new FileReader(templatePath+"/月中-固定费执行场景.txt");
         freaderEnd = new FileReader(templatePath+"/月末-固定费执行场景.txt");
         reader = new BufferedReader(freader);
         readerEnd = new BufferedReader(freaderEnd);
         String tempString = null;
         String tempStringEnd = null;
         while((tempString = reader.readLine()) != null){
            content.append(tempString).append("\n");
         }
         while((tempStringEnd = readerEnd.readLine()) != null){
            contentEnd.append(tempStringEnd).append("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }finally {
         freader.close();
         freader.close();
         reader.close();
         readerEnd.close();
      }
      replaceAll(content,"#decprd#"," ");
      replaceAll(content,"#normal#",normalStr.toString());
      replaceAll(contentEnd,"#decprd#",decprdStr.toString());
      replaceAll(contentEnd,"#normal#",normalEndStr.toString());
      System.out.println("content" +content);
      System.out.println("contentEnd" +contentEnd);
      writeFile(content,"/月中-固定费执行场景.txt");
      writeFile(contentEnd,"/月末-固定费执行场景.txt");
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
   private void writeFile(StringBuilder content, String fileName) {
      try {
         File file = new File(generatePath + (String)timeDir.get() + fileName);
         File parentFile = file.getParentFile();
         if (!parentFile.exists()) {
            parentFile.mkdirs();
         }

         if (!file.exists()) {
            file.createNewFile();
         }

         FileWriter fw = new FileWriter(file);
         fw.write(content.toString());
         fw.flush();
         fw.close();
      } catch (IOException e) {
         e.printStackTrace();
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
