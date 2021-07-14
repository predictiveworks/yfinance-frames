package de.kp.works.yfinance
/*
 * Copyright (c) 2019 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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

import org.apache.spark.sql.DataFrame
import com.google.gson._

import de.kp.works.http.HttpConnect
import de.kp.works.spark.Session

class YahooReader extends BaseReader with HttpConnect {
  
  private var interval:String = ""
  private var range:String = ""
  
  private var numSlices:Int = 1
  private val datatype:String = "json"
  
  private val session = Session.getSession
  
  def setInterval(value:String):YahooReader = {
    interval = value
    this
  }
  
  def setRange(value:String):YahooReader = {
    range = value
    this
  }

  def readSymbol(symbol:String):DataFrame = {
    
    try {
      
      /****************************************
       * 
       * Retrieve dataset from YFinance and transform 
       * into JSON
       * 
       ***************************************/
    
      val endpoint = s"${base}${symbol}?range=${range}&interval=${interval}"

      val bytes = get(endpoint)
      val json = extractJsonBody(bytes)
       
      /****************************************
       * 
       * Transform retrieved JSON into an Apache Spark 
       * DataFrame
       * 
       ***************************************/
  
      val chart = json.getAsJsonObject.get("chart").getAsJsonObject
      isError(chart)
  
      transform(chart)
      
    } catch {
      case t:Throwable =>
        log.error(s"Extracting Yahoo Finance response failed with: " + t.getLocalizedMessage)
        session.emptyDataFrame
    }
  }

  private def transform(chart:JsonObject):DataFrame = {

    val (rows, schema) = buildRowsAndSchema(chart)    
    session.createDataFrame(session.sparkContext.parallelize(rows, numSlices), schema)
    
  }
  
}