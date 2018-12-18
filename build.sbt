/* Copyright (c) 2016, Nokia Solutions and Networks Oy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Nokia Solutions and Networks Oy nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NOKIA SOLUTIONS AND NETWORKS OY BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import de.heikoseeberger.sbtheader.HeaderKey._
import de.heikoseeberger.sbtheader.HeaderPattern
import scalariform.formatter.preferences._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(noPublishSettings: _*).
  aggregate(
    `mesos-scala-api`,
    `mesos-scala-interface`,
    `mesos-scala-framework`,
    `mesos-scala-java-bridge`,
    `mesos-scala-example`
  )

lazy val `mesos-scala-api` = (project in file("mesos-scala-api")).
  settings(commonSettings: _*).
  settings(publishSettings: _*).
  settings(fork in Test := true).
  dependsOn(
    `mesos-scala-framework`,
    `mesos-scala-java-bridge`
  )

lazy val `mesos-scala-interface` = (project in file("mesos-scala-interface")).
  settings(commonSettings: _*).
  settings(publishSettings: _*).
  settings(
    libraryDependencies += "org.apache.mesos" % "mesos" % "1.4.0",
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    PB.targets in Compile := Seq(
      PB.gens.java -> (sourceManaged in Compile).value,
      scalapb.gen(javaConversions=true, flatPackage=false, grpc=false) -> (sourceManaged in Compile).value
    )
  ).
  settings(
    excludeFilter in (Compile, managedSources) := "*.java",
    managedSources in Compile <<= (managedSources in Compile, excludeFilter in (Compile, managedSources)) map { (files, filter) =>
      files.filter(f => !filter.accept(f))
    }
  )
//  settings(
//    mappings in (Compile,packageBin) ~= { ms: Seq[(File, String)] =>
//      ms filterNot { case (file, toPath) =>
//        toPath == "org/apache/mesos/Protos.class" || (toPath.startsWith("org/apache/mesos/Protos$") && toPath.endsWith(".class"))
//      }
//    }
//  )

lazy val `mesos-scala-framework` = (project in file("mesos-scala-framework")).
  settings(commonSettings: _*).
  settings(publishSettings: _*).
  dependsOn(`mesos-scala-interface`).
  settings(
    libraryDependencies ++= Seq(
      "io.reactivex" %% "rxscala" % "0.26.1"
    )
  )

lazy val `mesos-scala-java-bridge` = (project in file("mesos-scala-java-bridge")).
  settings(commonSettings: _*).
  settings(publishSettings: _*).
  dependsOn(`mesos-scala-interface`).
  settings(connectInput in Test := true)

lazy val `mesos-scala-example` = (project in file("mesos-scala-example")).
  settings(commonSettings: _*).
  settings(noPublishSettings: _*).
  dependsOn(`mesos-scala-api`).
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*).
  settings(
    fork in IntegrationTest := true,
    fork in run := true,
    libraryDependencies ++= Seq(
      logback,
      scalatest % "it"
    ),
    EclipseKeys.configurations += IntegrationTest
  )

lazy val commonSettings = Seq(

  organization := "com.nokia",
  organizationName := "Nokia",
  version := "0.5.0-SNAPSHOT",
  description := "A Scala API for Mesos",
  licenses ++= Seq(
    "BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"),
    "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
  ),
  homepage := Some(url("https://github.com/nokia/mesos-scala-api")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/nokia/mesos-scala-api"),
    "scm:git:github.com/nokia/mesos-scala-api.git"
  )),

  // Scala:
  scalaVersion := "2.11.8",
  compileOrder := CompileOrder.JavaThenScala, // needed because of protobuf
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Xlint:_",
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-dead-code"
  ),

  // Code formatter:
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(PreserveSpaceBeforeArguments, true),

  // Scalastyle:
  scalastyleFailOnError := true,

  // Code coverage:
  scoverage.ScoverageKeys.coverageExcludedFiles := ".*target\\/.*",

  // Dependencies:
  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    scalatest % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.5" % "test", // can't upgrade to 1.13 due to scalatest
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
    logback % "test"
  ),

  // sbt:
  updateOptions := updateOptions.value.withCachedResolution(true),

  headers := Map(
    "scala" -> (HeaderPattern.cStyleBlockComment, License.Nokia.cStyle)
  )
)

lazy val publishSettings = Seq(
  pomExtra := {
    <developers>
      <developer>
        <id>aszarvas</id>
        <name>aszarvas</name>
        <url>https://github.com/aszarvas</url>
      </developer>
      <developer>
        <id>dilation</id>
        <name>dilation</name>
        <url>https://github.com/dilation</url>
      </developer>
    </developers>
  }
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val scalatest =
  "org.scalatest" %% "scalatest" % "2.2.6"

lazy val logback =
  "ch.qos.logback" % "logback-classic" % "1.1.7"

addCommandAlias("validate", ";test:compile;it:compile;test;it:test")
addCommandAlias("measureCoverage", ";clean;coverage;test")
