package de.choffmeister.gittimeshift

import java.io._

import org.specs2.mutable._

class ApplicationSpec extends Specification {
  "Application" should {
    "run" in TempDirectory { dir ⇒
      val repoDir = new File(dir, "jgit-cookbook")
      Unzipper.unzipResource("jgit-cookbook.zip", repoDir)

      val app = new Application()
      app.run(Array(repoDir.toString))

      ok
    }
  }
}
