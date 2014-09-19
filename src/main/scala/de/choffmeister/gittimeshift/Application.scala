package de.choffmeister.gittimeshift

import java.io._

import com.madgag.git.bfg.cleaner._
import com.madgag.git.bfg.cleaner.protection.ProtectedObjectCensus
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class Application {
  def run(args: Array[String]) {
    val builder = new FileRepositoryBuilder()
    val repoDir = new File(args(0))
    val repo = builder.setGitDir(repoDir)
      .readEnvironment()
      .findGitDir()
      .build()

    val config = ObjectIdCleaner.Config(
      protectedObjectCensus = ProtectedObjectCensus(),
      objectIdSubstitutor = ObjectIdSubstitutor.OldIdsPublic,
      commitNodeCleaners = Seq.empty,
      treeEntryListCleaners = Seq.empty,
      treeBlobsCleaners = Seq.empty,
      treeSubtreesCleaners = Seq.empty,
      objectChecker = None)

    RepoRewriter.rewrite(repo, config)
  }
}

object Application {
  def main(args: Array[String]) {
    val app = new Application()
    app.run(args)
  }
}
