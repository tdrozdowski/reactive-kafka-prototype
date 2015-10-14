name := "reactive-kafka-prototype"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.8.1",
  "org.reactivemongo" %% "reactivemongo" % "0.11.7",
  "com.typesafe.play" %% "play-json" % "2.3.7"
)
