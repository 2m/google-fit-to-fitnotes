/*
 * Copyright 2021 github.com/2m/google-fit-to-fitnotes/contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lt.dvim.fitnotes

import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.math.BigDecimal

import cats.effect.IO
import cats.implicits._
import doobie._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse

object Main {
  case class FpValue(fpVal: Float)
  case class FitValue(value: FpValue)
  case class DataPoint(fitValue: List[FitValue], startTimeNanos: Long)

  val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

  def main(args: Array[String]): Unit = {
    val (fitData, dbData) = args.toList match {
      case file :: dbFile :: Nil => {
        if (args.toList.forall(f => Files.exists(Paths.get(f)))) (Source.fromFile(file).mkString, dbFile)
        else throw new Error(s"Not all files in [${args.mkString(", ")}] exist.")
      }
      case _ => throw new Error("Please provide path to the Google Fit weight data.")
    }

    val fitJson    = parse(fitData).getOrElse(Json.Null)
    val dataPoints = fitJson.hcursor.downField("Data Points").as[List[DataPoint]]
    val parsed = for {
      dataPoint <- dataPoints.toSeq.flatten
    } yield {
      val dateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(dataPoint.startTimeNanos / 1000000000), ZoneId.systemDefault())
      val date  = dateFormat.format(dateTime)
      val time  = timeFormat.format(dateTime)
      val value = BigDecimal.decimal(dataPoint.fitValue.head.value.fpVal).setScale(1, BigDecimal.RoundingMode.HALF_UP)
      (date, time, value)
    }

    implicit val cs = IO.contextShift(global)

    val xa = Transactor.fromDriverManager[IO](
      "org.sqlite.JDBC",
      s"jdbc:sqlite:$dbData",
      "",
      ""
    )

    val y = xa.yolo
    import y._

    val sql = "insert into MeasurementRecord (measurement_id, date, time, value, comment) values (1, ?, ?, ?, '')"
    Update[(String, String, BigDecimal)](sql).updateMany(parsed).quick.unsafeRunSync()

    println("Done.")
  }
}
