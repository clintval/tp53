package io.cvbio.seshat

import com.fulcrumgenomics.commons.CommonsDef._
import com.fulcrumgenomics.commons.io.PathUtil.{basename, extensionOf}
import com.fulcrumgenomics.commons.io.{Io, PathUtil}
import enumeratum.EnumEntry.Lowercase
import enumeratum.{EnumEntry, _}
import htsjdk.samtools.util.IOUtil
import io.cvbio.io.{CliTool, Python3}
import io.cvbio.seshat.SeshatUploadVcf._
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

import java.net.{URI, URL}
import java.nio.file.{Files, Path}
import scala.collection.mutable.ListBuffer
import scala.util.Properties.isMac

/** A class that will execute the Python command `vcf_upload`. */
class SeshatUploadVcf(
  val input: PathToVcf,
  val email: String,
  val assembly: SeshatAssembly = SeshatAssembly.Hg38,
  val url: URL = DefaultSeshatUploadUrl,
  val waitFor: Int = DefaultWaitFor
) {
  SeshatUploadVcf.validateEnvironment()

  /** A logger for this class. */
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  /** Run the tool [[SeshatUploadVcf]]. */
  def execute(): Unit = {
    val plainTextVcf = if (!IOUtil.hasBlockCompressedExtension(input)) input else {
      logger.info("Discovered a GZIP-compressed VCF. Writing VCF to temporary plain-text before upload.")
      val name = stripBlockCompressedExtension(input.getFileName)
      val temp = Io.makeTempDir(getClass.getSimpleName).resolve(name)
      val in   = Io.toInputStream(input) // Elegantly handles optionally BGZIP/GZIP compressed input.
      val out  = Io.toOutputStream(temp)
      IOUtils.copy(in, out)
      temp.toFile.deleteOnExit()
      in.close()
      out.close()
      temp
    }
    CliTool.execCommand(
      command        = Seq(Python3.executable, "-m", "tp53.seshat.upload_vcf") ++ scriptArgs(plainTextVcf),
      logger         = Some(logger),
      stdoutRedirect = s => logger.info(s),
      stderrRedirect = s => logger.info(s),
    )
  }

  /** The argument list for the [[SeshatUploadVcf]] script. */
  private[seshat] def scriptArgs(input: PathToVcf): Seq[String] = {
    val buffer = new ListBuffer[String]()
    buffer.addAll(Seq("--input", input.toString))
    buffer.addAll(Seq("--assembly", assembly.entryName))
    buffer.addAll(Seq("--email", email))
    buffer.addAll(Seq("--url", url.toString))
    buffer.addAll(Seq("--wait-for", waitFor.toString))
    buffer.toList
  }
}

/** Companion object for [[SeshatUploadVcf]]. */
object SeshatUploadVcf {

  /** The default Seshat Batch upload URL. */
  val DefaultSeshatUploadUrl: URL = new URI("http://vps338341.ovh.net/batch_analysis").toURL

  /** The total number of seconds we will wait for the VCF file to upload, in seconds. */
  val DefaultWaitFor: Int = 5

  /** The root of this Unix system. */
  private val UnixRoot: Path = PathUtil.pathTo("/")

  /** The location on Mac operating systems where applications are installed. */
  private val MacApplicationRoot: DirPath = UnixRoot.resolve("Applications")

  /** Searches the MacOS application directory for an App that starts with <prefix> and returns the full path of the
   * first application discovered. The file path returned is not tested for its ability to actually be executed.
   */
  private[seshat] def findMacApplication(prefix: String): Option[FilePath] = {
    if (!isMac) { None }
    else {
      Files.walk(MacApplicationRoot, 1)
        .iterator
        .find(path => basename(path).startsWith(prefix) && extensionOf(path).contains(".app"))
    }
  }

  /** Strip a block compressed extension from a file path if it exists. */
  private[seshat] def stripBlockCompressedExtension(path: FilePath): FilePath = {
    if (IOUtil.hasBlockCompressedExtension(path)) PathUtil.removeExtension(path)
    else path
  }

  /** Raise an exception if the environment is not configured to run this tool. */
  def validateEnvironment(): Unit = {
    if (isMac) require(
      findMacApplication(prefix = "Google Chrome").nonEmpty,
      message = "Detected a MacOS platform but no installation of Google Chrome."
    )
    require(Python3.available, "Python 3 is not available on the system path.")
  }

  /** A trait that all enumerations of [[SeshatAssembly]] should extend. */
  sealed trait SeshatAssembly extends EnumEntry with Lowercase

  /** Companion object for [[SeshatAssembly]]. */
  object SeshatAssembly extends Enum[SeshatAssembly] {

    /** All values of this enumeration. */
    val values: IndexedSeq[SeshatAssembly] = findValues

    /** The assembly hg17. */
    case object Hg17 extends SeshatAssembly

    /** The assembly hg18. */
    case object Hg18 extends SeshatAssembly

    /** The assembly hg38. */
    case object Hg38 extends SeshatAssembly
  }
}
