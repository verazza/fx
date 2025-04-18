import Dependencies._

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github"
ThisBuild / organizationName := "verazza"

lazy val root = (project in file("."))
  .settings(
    name := "fx",
    libraryDependencies += munit % Test
  )
