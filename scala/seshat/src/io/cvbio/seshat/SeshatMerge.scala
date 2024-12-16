package io.cvbio.seshat

import com.fulcrumgenomics.commons.CommonsDef._
import com.fulcrumgenomics.commons.util.{DelimitedDataParser, Row}
import com.fulcrumgenomics.vcf.api.VcfCount.Fixed
import com.fulcrumgenomics.vcf.api._
import io.cvbio.seshat.SeshatMerge._
import org.slf4j.LoggerFactory
import io.cvbio.seshat.VcfUtil.{Utf8DescriptionPrefixSentinel, fieldEncode}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A class that will merge Seshat annotations with a VCF file.. */
class SeshatMerge(
  val input: PathToVcf,
  val annotations: FilePath,
  val output: PathToVcf,
) {

  /** A logger for this class. */
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  /** Run the tool [[SeshatMerge]]. */
  def execute(): Unit = {
    val source   = VcfSource(input)
    val variants = source.iterator
    val sample   = VcfSource.onlySample(source)
    val parser   = DelimitedDataParser(annotations, delimiter = Delimiter)
    val ids      = (parser.headers.toSet -- FieldsToIgnore - sample).toSeq.sorted
    val writer   = VcfWriter(output, header = seshatHeader(source.header, ids))

    logger.info("Starting to zip annotations and VCF variants.")
    (variants zip parser).foreach { case (variant: Variant, row: Row) =>
      validateVariantAndAnnotation(variant, row)
      val record = variant.copy(attrs = variant.attrs ++ seshatAttrs(row, ids))
      writer.write(record)
    }

    require(variants.isEmpty, "The input VCF source was not completely exhausted.")
    require(parser.isEmpty, "The input annotations file was not completely exhausted.")

    logger.info("Successfully annotated variant calls.")

    source.safelyClose()
    writer.close()
  }
}

/** Companion object for [[SeshatMerge]]. */
object SeshatMerge {

  /** The ISO8601 date formatter. */
  private val Iso8601Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /** Format a date as per ISO8601. */
  private def iso8601(date: LocalDate): String = Iso8601Formatter.format(date)

  /** This package's version, or current date if no version can be found. */
  private[seshat] lazy val PackageVersion: String = {
    Option(getClass.getPackage.getImplementationVersion).getOrElse(s"unknown-${iso8601(LocalDate.now())}")
  }

  /** The annotations file data delimiter. */
  val Delimiter: Char = '\t'

  /** Ignore these invariant fields in the annotation file, as they are copy-pasted from the source VCF. */
  private[seshat] val FieldsToIgnore: Set[String] = {
    Set("Sample", "CHROM", "POS", "ID", "REF", "ALT", "QUAL", "FILTER", "INFO", "FORMAT")
  }

  /** Build a new VCF header with additional IDs for Seshat annotations. IDs will be UTF-8 encoded. */
  private[seshat] def seshatHeader(header: VcfHeader, ids: Seq[String]): VcfHeader = {
    def info(id: String): VcfInfoHeader = {
      val encodedId = fieldEncode(id)
      VcfInfoHeader(
        id          = s"Seshat_$encodedId",
        count       = Fixed(1),
        kind        = VcfFieldType.String,
        description = Utf8DescriptionPrefixSentinel,
        source      = Some(getClass.getSimpleName.dropRight(1)),
        version     = Some(PackageVersion)
      )
    }
    header.copy(infos = header.infos ++ ids.map(info))
  }

  /** Build a key-value map of Seshat annotations. IDs and values will be UTF-8 encoded. */
  private[seshat] def seshatAttrs(row: Row, ids: Seq[String]): Seq[(String, String)] = {
    ids.map { id =>
      val encodedId    = fieldEncode(id)
      val encodedValue = fieldEncode(row[String](id))
      s"Seshat_$encodedId" -> encodedValue
    }
  }

  /** Perform validation on a VCF variant and its associated Seshat key-value annotation. */
  private[seshat] def validateVariantAndAnnotation(variant: Variant, row: Row): Unit = {
    require(
      variant.genotypes.keySet.contains(row[String]("Sample")),
      s"Seshat annotation file record does not match VCF sample: $variant, $row"
    )
    require(
      row[String]("CHROM") == variant.chrom,
      s"Seshat annotation file record CHROM does not match VCF record contig: $variant, $row"
    )
    require(
      row[Int]("POS") == variant.pos,
      s"Seshat annotation file record POS does not match VCF record position: $variant, $row"
    )
    require(
      row[String]("REF") == variant.alleles.ref.toString,
      s"Seshat annotation file record REF does not match VCF reference allele: $variant, $row"
    )
  }
}
