import Dependencies._

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.3.0-SNAPSHOT"
ThisBuild / organization := "com.github"
ThisBuild / organizationName := "verazza"

lazy val root = (project in file("."))
  .settings(
    name := "fx", // プロジェクト名
    Compile / run / mainClass := Some("fx.Main"), // 通常実行時のメインクラス
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "16.0.0-R24", // ScalaFXのバージョン (プロジェクトに合わせてください)
      // JavaFXモジュール (プロジェクトに合わせてください)
      "org.openjfx" % "javafx-controls" % "16",
      "org.openjfx" % "javafx-graphics" % "16",
      "org.openjfx" % "javafx-fxml" % "16", // もしFXMLを使っていれば
      "org.openjfx" % "javafx-media" % "16", // もしMediaを使っていれば
      // ... 他のJavaFXモジュール ...
      munit % Test
    ),
    // JavaFXのネイティブライブラリを含めるための設定 (既存の設定を活かす)
    libraryDependencies ++= {
      lazy val osName = System.getProperty("os.name") match {
        case n if n.startsWith("Linux")   => "linux"
        case n if n.startsWith("Mac")     => "mac"
        case n if n.startsWith("Windows") => "win"
        case _ => throw new Exception("Unknown platform!")
      }
      Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
        .map(m =>
          "org.openjfx" % s"javafx-$m" % "16" classifier osName
        ) // JavaFXのバージョンをプロジェクトに合わせる
    },

    // --- sbt-assembly の設定 ---
    assembly / mainClass := Some("fx.Main"), // JAR実行時のメインクラス (スラッシュ構文)
    assembly / assemblyJarName := s"${name.value}-${organizationName.value}-${version.value}.jar", // 生成されるJARファイル名

    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        MergeStrategy.discard // META-INF以下の重複は破棄 (一般的な戦略)
      case "module-info.class" => // ★★★ 修正箇所: module-info.class を直接指定して破棄 ★★★
        MergeStrategy.discard // Java 9+ のモジュール情報
      // JavaFX関連の競合は最初のものを採用 (状況により調整)
      // より具体的なパスを指定するか、必要に応じて MergeStrategy.rename や他の戦略を検討
      case x if x.startsWith("javafx/")         => MergeStrategy.first
      case x if x.startsWith("com/sun/javafx/") => MergeStrategy.first
      case x if x.startsWith("com/sun/prism/")  => MergeStrategy.first
      case x if x.startsWith("com/sun/glass/")  => MergeStrategy.first
      // META-INF内の特定のファイルを保持したい場合は、上記のPathList("META-INF",...)より前に記述する
      // 例: case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
      case x => MergeStrategy.defaultMergeStrategy(x) // それ以外はデフォルト戦略
    }
    // --- sbt-assembly の設定ここまで ---
  )
