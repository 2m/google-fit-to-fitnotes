organization := "lt.dvim.fitnotes"
name := "google-fit-to-fitnotes"
description := "Imports weight measurements from Google Fit Takeout to Fitnotes DB"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.14.1")

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-hikari"
).map(_ % "0.13.4")

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.36.0.1"

scalafmtOnCompile := true
scalafixOnCompile := true

ThisBuild / scalafixDependencies ++= Seq(
  "com.nequissimus" %% "sort-imports" % "0.5.5"
)

enablePlugins(AutomateHeaderPlugin)

organizationName := "github.com/2m/google-git-to-fitnotes/contributors"
startYear := Some(2021)
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
