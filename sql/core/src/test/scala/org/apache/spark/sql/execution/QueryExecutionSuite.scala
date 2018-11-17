/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.execution

import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, OneRowRelation}
import org.apache.spark.sql.test.SharedSQLContext

case class Simple(a: String, b: Int)

class QueryExecutionSuite extends SharedSQLContext {
  test("toString() exception/error handling") {
    spark.experimental.extraStrategies = Seq(
        new SparkStrategy {
          override def apply(plan: LogicalPlan): Seq[SparkPlan] = Nil
        })

    def qe: QueryExecution = new QueryExecution(spark, OneRowRelation())

    // Nothing!
    assert(qe.toString.contains("OneRowRelation"))

    // Throw an AnalysisException - this should be captured.
    spark.experimental.extraStrategies = Seq(
      new SparkStrategy {
        override def apply(plan: LogicalPlan): Seq[SparkPlan] =
          throw new AnalysisException("exception")
      })
    assert(qe.toString.contains("org.apache.spark.sql.AnalysisException"))

    // Throw an Error - this should not be captured.
    spark.experimental.extraStrategies = Seq(
      new SparkStrategy {
        override def apply(plan: LogicalPlan): Seq[SparkPlan] =
          throw new Error("error")
      })
    val error = intercept[Error](qe.toString)
    assert(error.getMessage.contains("error"))
  }

  test("toString() tree depth") {
    import testImplicits._

    val s = Seq(Simple("a", 1), Simple("b", 3), Simple("c", 4))
    val ds = (1 until 20).foldLeft(s.toDF()) { case (newDs, _) =>
      newDs.join(s.toDF(), "a")
    }
    // scalastyle:off println
    println(ds.queryExecution.toString)
    // scalastyle:on println
    val nLines = ds.queryExecution.optimizedPlan.toString.split("\n").length
    assert(nLines < 15)
  }
}
