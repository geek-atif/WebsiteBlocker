package com.ironsdn.websiteblocker

import android.annotation.SuppressLint
import java.util.*


/**
 * Created by Atif Qamar on 29-07-2020.
 */
@SuppressLint("LongLogTag")
class Utility {
    companion object {

        fun getURlLookup( hashCode : Int) : URlLookup {
          var data =   getUrlLookUpDetail().get(hashCode)
           if( data ==null ){
               return URlLookup(0, "", "", false)
           }
            return data
        }

        fun getUrlLookUpDetail(): java.util.HashMap<Int, URlLookup> {
            var urlLookUp: HashMap<Int, URlLookup> = HashMap<Int, URlLookup>()
            urlLookUp.put(-364826023 , URlLookup(-364826023, "facebook.com", "Social Media", true))
            urlLookUp.put(-373274299 , URlLookup(-373274299, "instagram.com", "Social Media", true))
            urlLookUp.put(597105259 , URlLookup(597105259, "nowtv.com", "Video Site", true))
            return urlLookUp
        }
    }
}