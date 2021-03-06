package de.kp.works.spark
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
import org.apache.spark._
import org.apache.spark.sql._

object Session {
  
  private var session:SparkSession = null
  /**
   * The SparkSession is built without using the respective
   * builder as this approach can be used e.g. to integrate
   * Analytics-Zoo additional context with ease.
   */
  def initialize:Unit = {
    
    val conf = new SparkConf()
      .setAppName("VantageFrames")
      .setMaster("local[4]")
      /*
       * Driver & executor configuration
       */
      .set("spark.driver.maxResultSize", "4g")
      .set("spark.driver.memory"       , "12g")
      .set("spark.executor.memory"     , "12g")
    

    val sparkContext = new SparkContext(conf)
    session = new SQLContext(sparkContext).sparkSession
    
    session.sparkContext.setLogLevel("ERROR")
    
  }
  
  def getSession = {
    if (session == null) initialize
    session
    
  }
  
}