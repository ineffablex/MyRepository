package com.newland.boot.service;

import java.util.HashMap;
import java.util.List;

public interface AutoService {
   void creatData(HashMap<String, Object> param);

   List<Integer> getFeeSource(String proCode);

   List<Integer> getFeeTarget(String proCode);

   List<String> getBizDomain(String proCode);

   int insertWlan(String user, Long currentTime);

   List<String> getPreCodes();

   void creatBizData(HashMap<String, Object> param);

   void getBizData(HashMap<String, Object> proParam);

    List<String> preBzCodes();
}
