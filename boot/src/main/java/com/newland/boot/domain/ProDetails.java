package com.newland.boot.domain;

public class ProDetails {
   public String procode;
   public Integer status;
   public String dealMsg;

   public String getProcode() {
      return this.procode;
   }

   public void setProcode(String procode) {
      this.procode = procode;
   }

   public Integer getStatus() {
      return this.status;
   }

   public void setStatus(Integer status) {
      this.status = status;
   }

   public String getDealMsg() {
      return this.dealMsg;
   }

   public void setDealMsg(String dealMsg) {
      this.dealMsg = dealMsg;
   }

   public String toString() {
      return "ProDetails [procode=" + this.procode + ", status=" + this.status + ", dealMsg=" + this.dealMsg + "]";
   }
}
