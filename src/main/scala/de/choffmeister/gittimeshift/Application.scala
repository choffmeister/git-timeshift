package de.choffmeister.gittimeshift

import java.io._
import java.util._

import com.madgag.git.bfg.cleaner._
import com.madgag.git.bfg.cleaner.protection.ProtectedObjectCensus
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class Application {
  def run(args: Array[String]) = {
    val builder = new FileRepositoryBuilder()
    val repo = builder.findGitDir(new File(System.getProperty("user.dir"))).readEnvironment().build()

    args.toList match {
      case expr :: Nil ⇒
        val allowed = (t: Date) ⇒ !(t.getDay >= 1 && t.getDay <= 5 && t.getHours >= 8 && t.getHours < 18)
        val timeshiftCleaner = new TimeshiftCommitNodeCleaner(allowed, repo, Left(expr))
        val config = ObjectIdCleaner.Config(
          protectedObjectCensus = ProtectedObjectCensus(),
          objectIdSubstitutor = ObjectIdSubstitutor.OldIdsPublic,
          commitNodeCleaners = timeshiftCleaner :: Nil,
          treeEntryListCleaners = Nil,
          treeBlobsCleaners = Nil,
          treeSubtreesCleaners = Nil,
          objectChecker = None)

        RepoRewriter.rewrite(repo, config)
      case head :: tail ⇒
        throw new Exception("To many arguments. Please provide an expression that selects commits to adjust")
      case Nil ⇒
        throw new Exception("Please provide an expression that selects commits to adjust")
    }
  }
}

object Application {
  def main(args: Array[String]) {
    val app = new Application()
    app.run(args)
  }
}
