package com.newland.boot.service.impl;

import com.newland.boot.dao.AutoDao;
import com.newland.boot.service.AutoService;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AutoServiceImpl implements AutoService {
   @Autowired
   private AutoDao autoDao;

   public void creatData(HashMap<String, Object> param) {
      this.autoDao.creatData(param);
   }

   public List<Integer> getFeeSource(String proCode) {
      return this.autoDao.getFeeSource(proCode);
   }

   public List<Integer> getFeeTarget(String proCode) {
      return this.autoDao.getFeeTarget(proCode);
   }

   public List<String> getBizDomain(String proCode) {
      return this.autoDao.getBizDomain(proCode);
   }

   public int insertWlan(String user, Long currentTime) {
      return this.autoDao.insertWlan(user, currentTime);
   }

   public List<String> getPreCodes() {
      return this.autoDao.getPreCodes();
   }

   @Override
   public void creatBizData(HashMap<String, Object> param) {
      this.autoDao.creatBizData(param);
   }

   @Override
   public void getBizData(HashMap<String, Object> proParam) {
      this.autoDao.getBizData(proParam);
   }

   @Override
   public List<String> preBzCodes() {
      return this.autoDao.getpreBzCodes();
   }
}
