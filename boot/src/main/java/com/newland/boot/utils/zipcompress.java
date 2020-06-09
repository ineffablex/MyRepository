package com.newland.boot.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class zipcompress {
   public static void zipCompress(String zipSavePath, File sourceFile) {
      try {
         ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipSavePath));
         compress(zos, sourceFile, sourceFile.getName());
         zos.close();
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   private static void compress(ZipOutputStream zos, File sourceFile, String fileName) throws Exception {
      int len;
      if (sourceFile.isDirectory()) {
         File[] fileList = sourceFile.listFiles();
         if (fileList.length == 0) {
            zos.putNextEntry(new ZipEntry(fileName + "/"));
         } else {
            File[] var4 = fileList;
            len = fileList.length;

            for(int var6 = 0; var6 < len; ++var6) {
               File file = var4[var6];
               compress(zos, file, fileName + "/" + file.getName());
            }
         }
      } else if (!sourceFile.exists()) {
         zos.putNextEntry(new ZipEntry("/"));
         zos.closeEntry();
      } else {
         zos.putNextEntry(new ZipEntry(fileName));
         FileInputStream fis = new FileInputStream(sourceFile);
         byte[] buf = new byte[1024];

         while((len = fis.read(buf)) != -1) {
            zos.write(buf, 0, len);
         }

         zos.closeEntry();
         fis.close();
      }

   }
}
