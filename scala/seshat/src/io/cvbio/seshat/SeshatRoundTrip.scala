package io.cvbio.seshat

import com.fulcrumgenomics.commons.CommonsDef._
import com.fulcrumgenomics.commons.io.PathUtil
import enumeratum.EnumEntry.Lowercase
import enumeratum.{EnumEntry, _}
import io.cvbio.seshat.SeshatFindInGmail.{DefaultEmailNewerThan, DefaultWaitFor => DefaultWaitForEmail}
import io.cvbio.seshat.SeshatRoundTrip.{SeshatAnnotationType, SeshatInfix}
import io.cvbio.seshat.SeshatUploadVcf.{DefaultSeshatUploadUrl, SeshatAssembly, DefaultWaitFor => DefaultWaitForUpload}

import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Files
import scala.util.matching.Regex

class SeshatRoundTrip(
  val input: PathToVcf,
  val output: PathPrefix,
  val email: String,
  val assembly: SeshatAssembly             = SeshatAssembly.Hg38,
  val annotationType: SeshatAnnotationType = SeshatAnnotationType.Long,
  val url: URL                             = DefaultSeshatUploadUrl,
  val waitForUpload: Int                   = DefaultWaitForUpload,
  val waitForEmail: Int                    = DefaultWaitForEmail,
  val newerThanEmail: Int                  = DefaultEmailNewerThan,
  val credentials: Option[FilePath]        = None,
) {
  SeshatUploadVcf.validateEnvironment()
  SeshatFindInGmail.validateEnvironment()

  /** Run the tool [[SeshatRoundTrip]]. */
  def execute(): Unit = {
    new SeshatUploadVcf(
      input    = input,
      email    = email,
      assembly = assembly,
      url      = url,
      waitFor  = waitForUpload
    ).execute()

    new SeshatFindInGmail(
      input       = input,
      output      = output,
      newerThan   = newerThanEmail,
      waitFor     = waitForEmail,
      credentials = credentials
    ).execute()

    SeshatRoundTrip.findAnnotations(output.getParent, annotationType) match {
      case None => throw new FileNotFoundException(
        s"Could not find '${annotationType.entryName}' format annotations in the output directory: ${output.getParent}"
      )
      case Some(annotations) => new SeshatMerge(
        input       = input,
        annotations = annotations,
        output      = PathUtil.pathTo(output.toString + SeshatInfix + ".vcf" + ".gz"),
      ).execute()
    }
  }
}

/** Companion object for [[SeshatRoundTrip]]. */
object SeshatRoundTrip {

  /** A path part infix for Seshat-related files. */
  val SeshatInfix: String = ".seshat"

  /** Find a Seshat annotation file in a directory, by type. Annotation files have a date-time-stamp in their filename. */
  private[seshat] def findAnnotations(dir: DirPath, kind: SeshatAnnotationType): Option[FilePath] = {
    val pattern: Regex = s".*${kind.entryName}-\\d{8}_\\d{6}_\\d{6}\\.tsv".r
    Files.walk(dir, 1).iterator.find(path => pattern.matches(PathUtil.basename(path, trimExt = false)))
  }

  /** A trait that all enumerations of [[SeshatAnnotationType]] should extend. */
  sealed trait SeshatAnnotationType extends EnumEntry with Lowercase

  /** Companion object for [[SeshatAnnotationType]]. */
  object SeshatAnnotationType extends Enum[SeshatAnnotationType] {

    /** All values of this enumeration. */
    val values: IndexedSeq[SeshatAnnotationType] = findValues

    /** The short-form annotations. */
    case object Short extends SeshatAnnotationType

    /** The long-form annotations. */
    case object Long extends SeshatAnnotationType
  }
}
