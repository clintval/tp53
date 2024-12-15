package io.cvbio.seshat

import com.fulcrumgenomics.FgBioDef.PathPrefix
import com.fulcrumgenomics.commons.CommonsDef._
import io.cvbio.io.{CliTool, Python3}
import io.cvbio.seshat.SeshatFindInGmail._
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

/** A class that will execute the Python command `find_in_gmail`. */
class SeshatFindInGmail(
  val input: PathToVcf,
  val output: PathPrefix,
  val newerThan: Int = DefaultEmailNewerThan,
  val waitFor: Int = DefaultWaitFor,
  val credentials: Option[FilePath] = None
) {
  SeshatFindInGmail.validateEnvironment()

  /** A logger for this class. */
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  /** Run the tool [[SeshatFindInGmail]]. */
  def execute(): Unit = {
    CliTool.execCommand(
      command        = Seq(Python3.executable, "-m", "tp53.seshat.find_in_gmail") ++ scriptArgs,
      logger         = Some(logger),
      stdoutRedirect = s => logger.info(s),
      stderrRedirect = s => logger.info(s),
    )
  }

  /** The argument list for the [[SeshatFindInGmail]] script. */
  private[seshat] def scriptArgs: Seq[String] = {
    val buffer = new ListBuffer[String]()
    buffer.addAll(Seq("--input", input.toString))
    buffer.addAll(Seq("--output", output.toString))
    buffer.addAll(Seq("--newer-than", newerThan.toString))
    buffer.addAll(Seq("--wait-for", waitFor.toString))
    credentials.map(_cred => buffer.addAll(Seq("--credentials", _cred.toString)))
    buffer.toList
  }
}

/** Companion object for [[SeshatFindInGmail]]. */
object SeshatFindInGmail {

  /** The total number of seconds we will wait for an email to arrive. */
  val DefaultWaitFor: Int = 200

  /** Default to only considering emails that are newer by this many hours. */
  val DefaultEmailNewerThan: Int = 10

  /** Raise an exception if the environment is not configured to run this tool. */
  def validateEnvironment(): Unit = {
    require(
      Python3.available,
      message = "Python 3 is not available on the system path."
    )
  }
}
