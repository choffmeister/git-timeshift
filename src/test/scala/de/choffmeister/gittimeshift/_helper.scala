package de.choffmeister.gittimeshift

import java.io._
import java.util._
import java.util.zip._

import org.specs2.execute._
import org.specs2.specification._

object TempDirectory extends Outside[File] {
  override def apply[R: AsResult](a: File ⇒ R) = {
    val temp = createTemporaryDirectory("")
    try {
      AsResult.effectively(a(temp))
    } finally {
      removeDirectory(temp)
    }
  }

  override def outside: File = ???

  private def createTemporaryDirectory(suffix: String): File = {
    val base = new File(new File(System.getProperty("java.io.tmpdir")), "gittimeshift")
    val dir = new File(base, UUID.randomUUID().toString + suffix)
    dir.mkdirs()
    dir
  }

  private def removeDirectory(dir: File): Unit = {
    def recursion(f: File): Unit = {
      if (f.isDirectory) {
        f.listFiles().foreach(child ⇒ recursion(child))
      }
      f.delete()
    }
    recursion(dir)
  }
}

object Unzipper {
  def unzipResource(resourceName: String, targetDir: File): Unit = {
    using(getClass.getClassLoader.getResourceAsStream(resourceName))(s ⇒ unzipStream(s, targetDir))
  }

  def unzipFile(zipFile: File, targetDir: File): Unit = {
    using(new FileInputStream(zipFile))(s ⇒ unzipStream(s, targetDir))
  }

  def unzipStream(zipStream: InputStream, targetDir: File): Unit = {
    val buffer = new Array[Byte](1024)
    val zip = new ZipInputStream(zipStream)
    var pos = Option(zip.getNextEntry)

    while (pos.isDefined) {
      val entry = pos.get
      val fileName = entry.getName
      val file = new File(targetDir, fileName)
      file.getParentFile.mkdirs()

      if (!entry.isDirectory) {
        val outStream = new FileOutputStream(file)
        var done = false
        while (!done) {
          val read = zip.read(buffer)
          if (read > 0) outStream.write(buffer, 0, read)
          else done = true
        }
        outStream.close()
      }

      pos = Option(zip.getNextEntry)
    }

    zip.closeEntry()
    zip.close()
  }

  def using[C <: { def close(): Unit }, T](closable: C)(inner: C ⇒ T): T = {
    try {
      inner(closable)
    } finally {
      closable.close()
    }
  }
}
