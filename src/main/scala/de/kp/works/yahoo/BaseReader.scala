package de.kp.works.yfinance
/*
 * Copyright (c) 20129 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @author Stefan Krusche, Dr. Krusche & Partner PartG
 * 
 */

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import com.google.gson._
import scala.collection.mutable

trait BaseReader {
    
  protected val base = "https://query1.finance.yahoo.com/v8/finance/chart/"
    
  protected val YF_INTERVALS = 
    List("1m","2m","5m","15m","30m","60m","90m","1h","1d","5d","1wk","1mo","3mo")
  
  protected val YF_RANGES = 
    List("1d","5d","1mo","3mo","6mo","1y","2y","5y","10y","ytd","max")
  
  protected val baseFields = Array(
    StructField("timestamp", LongType,   true),
    StructField("open",      DoubleType, true),
    StructField("high",      DoubleType, true),
    StructField("low",       DoubleType, true),
    StructField("close",     DoubleType, true))
    
  protected def buildRowsAndSchema(chart:JsonObject):(Seq[Row], StructType) = {
    
    val result = chart.get("result").getAsJsonArray.get(0).getAsJsonObject
    
    val timestamp = getLongList(result.get("timestamp").getAsJsonArray)
    val indicators = result.get("indicators").getAsJsonObject
    
    /*
     * Extract OHLCV from `quote`
     */
    val quote = indicators.get("quote").getAsJsonArray.get(0).getAsJsonObject
    
    val open = getDoubleList(quote.get("open").getAsJsonArray)
    val high = getDoubleList(quote.get("high").getAsJsonArray)
    
    val low = getDoubleList(quote.get("low").getAsJsonArray)
    val close = getDoubleList(quote.get("close").getAsJsonArray)
    
    val volume = getIntList(quote.get("volume").getAsJsonArray)

    /*
     * Extract ADJCLOSE
    *
     * We distinguish between dataset with and without
     * `adjclose` field 
     *
     */
    val adjclose = indicators.get("adjclose")
    if (adjclose == null || adjclose.isJsonNull) {
      
      val rows = timestamp.zip(open).zip(high).zip(low).zip(close).zip(volume)
        /* 
         * val rows: List[((((((Long, Double), Double), Double), Double), Double), Int)] 
         */
        .map{case(((((timestamp, open), high), low), close), volume) => {
           Row(timestamp, open, high, low, close, volume) 
        }}
      
      val schema = StructType(baseFields ++ Array(StructField("volume", IntegerType, true)))
      (rows, schema)      
      
    } else {

      val data = adjclose.getAsJsonArray.get(0).getAsJsonObject
      val adj_close = getDoubleList(data.get("adjclose").getAsJsonArray)

      val rows = timestamp.zip(open).zip(high).zip(low).zip(close).zip(adj_close).zip(volume)
        /* 
         * val rows: List[((((((Long, Double), Double), Double), Double), Double), Int)] 
         */
        .map{case((((((timestamp, open), high), low), close), adjclose), volume) => {
           Row(timestamp, open, high, low, close, adjclose, volume) 
        }}

      val schema = StructType(baseFields ++ 
          Array(StructField("adjclose",  DoubleType, true), StructField("volume", IntegerType, true)))
      (rows, schema)
      
    }
    
  }
  
  protected def isError(chart:JsonObject):Unit = {
    
    val isResultNull = chart.get("result").isJsonNull
    val isErrorNull = chart.get("error").isJsonNull
    
    if (isErrorNull && isResultNull)
      throw new Exception("The Yahoo endpoint did not return a valid answer.")

    if (isErrorNull == false) {

      val error = chart.get("error").getAsJsonObject
      val description = error.get("description").getAsString

      throw new Exception(s"The Yahaoo endpoint returned: ${description}")
      
    }

  }    
  
  protected def getDoubleList(jArray:JsonArray):List[Any] = {
    
    val values = mutable.ArrayBuffer.empty[Any]

    (0 until jArray.size).foreach(i => {
      
      val item = jArray.get(i)
      val valu = if (item.isJsonNull) null else item.getAsDouble
      
      values += valu
      
    })
    
    values.toList
   
  }
  
  protected def getIntList(jArray:JsonArray):List[Any] = {
    
    val values = mutable.ArrayBuffer.empty[Any]

    (0 until jArray.size).foreach(i => {
      
      val item = jArray.get(i)
      val valu = if (item.isJsonNull) null else item.getAsInt
      
      values += valu
      
    })
    
    values.toList
   
  }
  
  protected def getLongList(jArray:JsonArray):List[Long] = {
    
    val values = mutable.ArrayBuffer.empty[Long]

    (0 until jArray.size).foreach(i => {
      values += jArray.get(i).getAsLong
    })
    
    values.toList
   
  }
}