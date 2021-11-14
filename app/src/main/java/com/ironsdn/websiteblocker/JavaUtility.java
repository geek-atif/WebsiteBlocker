package com.ironsdn.websiteblocker;

public class JavaUtility {

   public static int getHash(String url){
          if(!url.isEmpty())
            return url.hashCode();
          return 0;
    }
}
