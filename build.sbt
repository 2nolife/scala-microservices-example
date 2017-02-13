name := "favorites-ms"

Common.settings

libraryDependencies ++= Dependencies.Favorites

mainClass in (Compile, run) := Some("com.coldcore.favorites.ms.start")

lazy val root = (project in file(".")).
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*)

parallelExecution in Test := false
parallelExecution in IntegrationTest := false