package de.choffmeister.gittimeshift

import java.util.Date

import com.madgag.git.bfg.cleaner.{ Cleaner, CommitNodeCleaner }
import com.madgag.git.bfg.cleaner.CommitNodeCleaner.Kit
import com.madgag.git.bfg.model.CommitNode
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.{ ObjectId, Repository, PersonIdent }
import org.eclipse.jgit.revwalk.RevCommit

import scala.collection.JavaConversions._

class TimeshiftCommitNodeCleaner(allowed: Date ⇒ Boolean, repo: Repository, commitsToChange: Either[String, List[RevCommit]]) extends CommitNodeCleaner {
  import de.choffmeister.gittimeshift.TimeshiftCommitNodeCleaner._

  val commits = commitsToChange match {
    case Right(c) ⇒ c
    case Left(expr) ⇒
      val regex = """^([a-zA-Z0-9\-/_~]+)(\.\.([a-zA-Z0-9\-/_~]+))?$""".r
      val range = regex.findFirstIn(expr) match {
        case Some(regex(a, _, b)) if Option(b).isDefined ⇒ (repo.resolve(a), repo.resolve(b))
        case Some(regex(a, _, b)) if Option(b).isEmpty ⇒ (repo.resolve(a), repo.resolve(a))
        case _ ⇒ throw new Exception(expr)
      }
      new Git(repo).log().addRange(range._1, range._2).call().toList
  }

  val timestampMap: Map[Date, Date] = {
    // get all timestamps of all commits
    val timestamps = commits.flatMap(c ⇒ List((c.getId, c.getAuthorIdent.getWhen), (c.getId, c.getCommitterIdent.getWhen))).toSet
    // find blocks the timestamps are in and the need mapping
    val mapping = timestamps.map { ts ⇒
      val (out, in) = (findOuterBlock(allowed, ts._2), findInnerBlock(allowed, ts._2))
      val ts2 = rescale(ts._2, out, in)
      (ts._1, ts._2, ts2)
    }
    val grouped = mapping.groupBy(_._3)
    // filter out mappings in blocks, that have no violating timestamps anyway (this makes this cleaning idempotent!!!)
    val filtered = grouped.filter(a ⇒ a._2.exists(b ⇒ !allowed(b._2))).toList
    filtered.flatMap(_._2).map(x ⇒ x._2 -> x._3).toMap
  }

  override def fixer(kit: Kit): Cleaner[CommitNode] = { commit ⇒
    val at = commit.author.getWhen
    val ct = commit.committer.getWhen
    changeTimestamps(commit, timestampMap.getOrElse(at, at), timestampMap.getOrElse(ct, ct))
  }

  private def changeTimestamps(commit: CommitNode, authorWhen: Date, committerWhen: Date): CommitNode = commit.copy(
    author = new PersonIdent(commit.author, authorWhen),
    committer = new PersonIdent(commit.committer, committerWhen)
  )
}

object TimeshiftCommitNodeCleaner {
  type Curfew = Date ⇒ Boolean
  type Block = (Date, Date)

  def addHours(t: Date, hours: Int) = new Date(t.getTime + hours * 1000 * 60 * 60)
  def floorToHours(t: Date): Date = new Date(t.getTime / (1000 * 60 * 60) * (1000 * 60 * 60))

  def min(t: Block): Date = if (t._1.before(t._2)) t._1 else t._2
  def max(t: Block): Date = if (t._1.after(t._2)) t._1 else t._2
  def median(t: Block): Date = new Date((t._1.getTime + t._2.getTime) / 2)

  def rescale(t: Date, from: Block, to: Block): Date = {
    val lambda = (t.getTime - from._1.getTime).toDouble / (from._2.getTime - from._1.getTime).toDouble
    new Date((to._1.getTime + (to._2.getTime - to._1.getTime) * lambda).toLong)
  }

  def findAllowed(c: Curfew, t: Date, step: Int): Date = c(t) match {
    case true ⇒ floorToHours(t)
    case false ⇒ findAllowed(c, new Date(t.getTime + step * 60 * 60 * 1000), step)
  }

  def findDisallowed(c: Curfew, t: Date, step: Int): Date = c(t) match {
    case false ⇒ floorToHours(t)
    case true ⇒ findDisallowed(c, new Date(t.getTime + step * 60 * 60 * 1000), step)
  }

  def findBlock(c: Curfew, t: Date, steps: Int): Block = steps match {
    case 0 ⇒ c(t) match {
      case true ⇒ (addHours(findDisallowed(c, t, -1), +1), findDisallowed(c, t, +1))
      case false ⇒ (addHours(findAllowed(c, t, -1), +1), findAllowed(c, t, +1))
    }
    case s if s < 0 ⇒ findBlock(c, addHours(findBlock(c, t, 0)._1, -1), steps + 1)
    case s if s > 0 ⇒ findBlock(c, findBlock(c, t, 0)._2, steps - 1)
  }

  def findOuterBlock(c: Curfew, t: Date): Block = c(t) match {
    case true ⇒ (median(findBlock(c, t, -1)), median(findBlock(c, t, +1)))
    case false ⇒ t.before(median(findBlock(c, t, 0))) match {
      case true ⇒ (median(findBlock(c, t, -2)), median(findBlock(c, t, 0)))
      case false ⇒ (median(findBlock(c, t, 0)), median(findBlock(c, t, +2)))
    }
  }

  def findInnerBlock(c: Curfew, t: Date): Block = c(t) match {
    case true ⇒ (max(findBlock(c, t, -1)), min(findBlock(c, t, +1)))
    case false ⇒ t.before(median(findBlock(c, t, 0))) match {
      case true ⇒ (max(findBlock(c, t, -2)), min(findBlock(c, t, 0)))
      case false ⇒ (max(findBlock(c, t, 0)), min(findBlock(c, t, +2)))
    }
  }
}
