package de.choffmeister.gittimeshift

import java.io._
import java.util.Date

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.specs2.mutable._

import scala.collection.JavaConversions._

class ApplicationSpec extends Specification {
  "Application" should {
    "run" in TempDirectory { dir ⇒
      // create test repository
      val repoDir = new File(dir, "jgit-cookbook")
      Unzipper.unzipResource("jgit-cookbook.zip", repoDir)

      try {
        // run application
        val app = new Application()
        app.run(Array(repoDir.toString))
      } catch {
        case _: Throwable ⇒ ()
      }

      // assert that afterwards all timestamps are ok
      val builder = new FileRepositoryBuilder()
      val repo = builder.setGitDir(repoDir)
        .readEnvironment()
        .findGitDir()
        .build()

      val timestamps = new Git(repo).log.all.call().toList.flatMap(c ⇒ List(c.getAuthorIdent.getWhen, c.getCommitterIdent.getWhen))
      val allowed = (t: Date) ⇒ !(t.getDay >= 1 && t.getDay <= 5 && t.getHours >= 8 && t.getHours < 18)
      timestamps.forall(ts ⇒ allowed(ts)) === true
    }
  }
}
