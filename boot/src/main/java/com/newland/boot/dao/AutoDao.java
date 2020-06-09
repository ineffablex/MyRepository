package com.newland.boot.dao;

import java.util.HashMap;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AutoDao {
   void creatData(HashMap<String, Object> param);

   List<Integer> getFeeSource(String proCode);

   List<Integer> getFeeTarget(String proCode);

   List<String> getBizDomain(String proCode);

   int insertWlan(@Param("userId") String user, @Param("InstanceId") Long currentTime);

   List<String> getPreCodes();

   List<String> getpreBzCodes();

   void creatBizData(HashMap<String, Object> param);

   void getBizData(HashMap<String, Object> proParam);

}
