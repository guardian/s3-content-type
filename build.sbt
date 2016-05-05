name := "s3-content-type"

scalaVersion in ThisBuild := "2.11.8"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.10.75"
)
