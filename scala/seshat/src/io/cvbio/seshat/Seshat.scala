package io.cvbio.seshat

import com.fulcrumgenomics.bam.api.{SamSource, SamWriter}
import com.fulcrumgenomics.commons.CommonsDef._
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.commons.util.SystemUtil.IntelCompressionLibrarySupported
import com.intel.gkl.compression.{IntelDeflaterFactory, IntelInflaterFactory}
import htsjdk.samtools.ValidationStringency
import htsjdk.samtools.util.{BlockCompressedOutputStream, BlockGunzipper, IOUtil}
import io.cvbio.seshat.SeshatFindInGmail.{DefaultEmailNewerThan, DefaultWaitFor, DefaultWaitFor => DefaultWaitForEmail}
import io.cvbio.seshat.SeshatRoundTrip.SeshatAnnotationType
import io.cvbio.seshat.SeshatUploadVcf.{DefaultSeshatUploadUrl, SeshatAssembly, DefaultWaitFor => DefaultWaitForUpload}
import org.rogach.scallop.{ScallopOption => Opt, _}

import java.net.URL
import scala.util.Properties.lineSeparator


/** Namespace for Seshat related tooling. */
object Seshat {

  /** Command line configuration. */
  private[seshat] class SeshatConf(args: Seq[String]) extends ScallopConf(args) {
    printedName = "seshat"
    private val packageName: String = Option(this.getClass.getPackage.getImplementationTitle).getOrElse("seshat")
    private val version: String     = Option(this.getClass.getPackage.getImplementationVersion).getOrElse("UNKNOWN")
    version(s"$packageName $version\n")
    banner(
      s"""Scala tools for programmatically annotating VCFs with the Seshat TP53 database.
         |
         |Common Options:
      """.stripMargin.stripLineEnd.trim
    )

    val asyncIo: Opt[Boolean] = opt(descr = "Use asynchronous I/O for VCF files", default = Some(false))
    val compression: Opt[Int] = opt(descr = "Default GZIP VCF compression level", default = Some(5))
    val tmpDir: Opt[DirPath]  = opt(descr = "Directory to use for temp files", default = Some(PathUtil.pathTo(System.getProperty("java.io.tmpdir"))))

    implicit val seshatAnnotationTypeConverter: ValueConverter[SeshatAnnotationType] = singleArgConverter[SeshatAnnotationType](SeshatAnnotationType.withName)
    implicit val seshatAssemblyConverter: ValueConverter[SeshatAssembly]             = singleArgConverter[SeshatAssembly](SeshatAssembly.withName)

    private[seshat] object SeshatFindInGmailCommand extends Subcommand("find-in-gmail") {
      private val required = this.group("required")

      val input: Opt[PathToVcf]      = opt(descr = "The path to the input VCF file.", required = true, group = required)
      val output: Opt[PathPrefix]    = opt(descr = "The output path prefix for writing annotations.", required = true, group = required)

      private val optional = this.group("optional")
      val credentials: Opt[FilePath] = opt(descr = "The Google OAuth credentials JSON.", default = None, group = optional)
      val newerThan: Opt[Int]        = opt(descr = "Only consider emails that arrived <= hours.\n(default: 10).", default = Some(DefaultEmailNewerThan), group = optional)
      val waitFor: Opt[Int]          = opt(descr = "How long to wait for Seshat to email in seconds.\n(default: 200)", default = Some(DefaultWaitFor), group = optional)
    }

    private[seshat] object SeshatMergeCommand extends Subcommand("merge") {
      private val required = this.group("required")
      val input: Opt[PathToVcf]      = opt(descr = "The path to the input VCF file.", required = true, group = required)
      val annotations: Opt[FilePath] = opt(descr = "The annotation text file output from Seshat.", required = true, group = required)
      val output: Opt[PathToVcf]     = opt(descr = "The path to the output VCF file.", required = true, group = required)
    }

    private[seshat] object SeshatRoundTripCommand extends Subcommand("round-trip") {
      private val required = this.group("required")
      val input: Opt[PathToVcf] = opt(descr = "The path to the input VCF file.", group = required)
      val output: Opt[PathPrefix] = opt(descr = "The output path prefix for the annotations and annotated VCF.", group = required)
      val email: Opt[String] = opt(descr = "The email address for delivering Seshat annotations.", group = required)

      private val optional = this.group("optional")
      val assembly: Opt[SeshatAssembly]             = opt(descr = "The human genome assembly of the input VCF.\nAvailable options: (hg17, hg19, hg38).\n(default: hg38).", default = Some(SeshatAssembly.Hg38), group = optional)
      val annotationType: Opt[SeshatAnnotationType] = opt(descr = "The annotation text file output from Seshat.\nAvailable options: (short, long).\n(default: long).", default = Some(SeshatAnnotationType.Long), group = optional)
      val url: Opt[URL]                             = opt(descr = "The Seshat batch annotation URL.\n(default: http://vps338341.ovh.net/batch_analysis).", default = Some(DefaultSeshatUploadUrl), group = optional)
      val waitForUpload: Opt[Int]                   = opt(descr = "Seconds to wait before raising an exception.\n(default: 200).", default = Some(DefaultWaitForUpload), group = optional)
      val waitForEmail: Opt[Int]                    = opt(descr = "How long to wait for Seshat to email in seconds.\n(default: 200)", default = Some(DefaultWaitForEmail), group = optional)
      val newerThanEmail: Opt[Int]                  = opt(descr = "Only consider emails that arrived <= hours.\n(default: 10).", default = Some(DefaultEmailNewerThan), group = optional)
      val credentials: Opt[FilePath]                = opt(descr = "The Google OAuth credentials JSON.", default = None, group = optional)
    }

    private[seshat] object SeshatUploadVcfCommand extends Subcommand("upload-vcf") {
      private val required = this.group("required")
      val input: Opt[PathToVcf] = opt(descr = "The path to the input VCF file.", required = true, group = required)
      val email: Opt[String]    = opt(descr = "The email address for delivering Seshat annotations.", required = true, group = required)

      private val optional = this.group("optional")
      val assembly: Opt[SeshatAssembly] = opt(descr = "The human genome assembly of the input VCF.\nAvailable options: (hg17, hg19, hg38).\n(default: hg38).", default =  Some(SeshatAssembly.Hg38))
      val url: Opt[URL]                 = opt(descr = "The Seshat batch annotation URL.\n(default: http://vps338341.ovh.net/batch_analysis).", default = Some(DefaultSeshatUploadUrl), group = optional)
      val waitFor: Opt[Int]             = opt(descr = "Seconds to wait before raising an exception.\n(default: 200).", default = Some(DefaultWaitFor), group = optional)
    }

    addSubcommand(SeshatFindInGmailCommand)
    addSubcommand(SeshatMergeCommand)
    addSubcommand(SeshatRoundTripCommand)
    addSubcommand(SeshatUploadVcfCommand)

    footer(lineSeparator + "MIT License Copyright 2024 Clint Valentine")
    verify()
  }

  /** Run the tool seshat. */
  def main(args: Array[String]): Unit = {
    import com.fulcrumgenomics.util.Io

    System.setProperty("slf4j.internal.verbosity", "WARN")

    val conf = new SeshatConf(args.toIndexedSeq)

    SamSource.DefaultUseAsyncIo = conf.asyncIo()
    SamWriter.DefaultUseAsyncIo = conf.asyncIo()

    SamWriter.DefaultCompressionLevel = conf.compression()
    BlockCompressedOutputStream.setDefaultCompressionLevel(conf.compression())
    IOUtil.setCompressionLevel(conf.compression())
    Io.compressionLevel = conf.compression()

    // $COVERAGE-OFF$
    if (IntelCompressionLibrarySupported) {
      BlockCompressedOutputStream.setDefaultDeflaterFactory(new IntelDeflaterFactory)
      BlockGunzipper.setDefaultInflaterFactory(new IntelInflaterFactory)
    }
    // $COVERAGE-ON$

    Io.tmpDir = conf.tmpDir()
    System.setProperty("java.io.tmpdir", conf.tmpDir().toAbsolutePath.toString)

    SamSource.DefaultValidationStringency = ValidationStringency.LENIENT

    htsjdk.samtools.util.Log.setGlobalLogLevel(htsjdk.samtools.util.Log.LogLevel.WARNING)

    conf.subcommand match {
      case Some(conf.SeshatFindInGmailCommand) => new SeshatFindInGmail(
        input       = conf.SeshatFindInGmailCommand.input(),
        output      = conf.SeshatFindInGmailCommand.output(),
        credentials = conf.SeshatFindInGmailCommand.credentials.toOption,
        newerThan   = conf.SeshatFindInGmailCommand.newerThan(),
        waitFor     = conf.SeshatFindInGmailCommand.waitFor(),
      ).execute()
      case Some(conf.SeshatUploadVcfCommand) => new SeshatUploadVcf(
        input    = conf.SeshatUploadVcfCommand.input(),
        email    = conf.SeshatUploadVcfCommand.email(),
        assembly = conf.SeshatUploadVcfCommand.assembly(),
        url      = conf.SeshatUploadVcfCommand.url(),
        waitFor  = conf.SeshatUploadVcfCommand.waitFor(),
      ).execute()
      case Some(conf.SeshatMergeCommand) => new SeshatMerge(
        input       = conf.SeshatMergeCommand.input(),
        annotations = conf.SeshatMergeCommand.annotations(),
        output      = conf.SeshatMergeCommand.output(),
      ).execute()
      case Some(conf.SeshatRoundTripCommand) => new SeshatRoundTrip(
        input          = conf.SeshatRoundTripCommand.input(),
        output         = conf.SeshatRoundTripCommand.output(),
        email          = conf.SeshatRoundTripCommand.email(),
        assembly       = conf.SeshatRoundTripCommand.assembly(),
        annotationType = conf.SeshatRoundTripCommand.annotationType(),
        url            = conf.SeshatRoundTripCommand.url(),
        waitForUpload  = conf.SeshatRoundTripCommand.waitForUpload(),
        waitForEmail   = conf.SeshatRoundTripCommand.waitForEmail(),
        newerThanEmail = conf.SeshatRoundTripCommand.newerThanEmail(),
        credentials    = conf.SeshatRoundTripCommand.credentials.toOption,
      ).execute()
      case _ =>
        conf.printHelp()
        println()
        println(Util.format(s"No subcommand selected! Pick from find-in-gmail, merge, round-trip, upload-vcf"))
        sys.exit(1)
    }
  }
}
