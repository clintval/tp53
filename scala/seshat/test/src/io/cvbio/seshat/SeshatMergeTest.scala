package io.cvbio.seshat

import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.commons.util.{DelimitedDataParser, Row}
import com.fulcrumgenomics.testing.VcfBuilder
import com.fulcrumgenomics.testing.VcfBuilder.Gt
import com.fulcrumgenomics.util.Io.makeTempFile
import com.fulcrumgenomics.vcf.api.{AlleleSet, Genotype, Variant, VcfSource}
import io.cvbio.UnitSpec
import io.cvbio.seshat.SeshatMerge._
import io.cvbio.seshat.VcfUtil.{TextExtension, VcfExtension}

import scala.util.Properties.lineSeparator

/** Unit tests for [[SeshatMerge]]. */
class SeshatMergeTest extends UnitSpec {

  /** The name of the first reference sequence. */
  val Chr1: String = "chr1"

  /** A helper method to make a Row since Row is private. */
  private def buildRow(mapping: Map[String, String], delimiter: Char = Delimiter): Row = {
    val temp   = makeTempFile(getClass.getSimpleName, ".txt")
    val writer = Io.toWriter(temp)
    val keys   = mapping.keySet.toSeq.sorted.toList
    val values = keys.map(mapping.apply)

    writer.write(keys.mkString(delimiter.toString) + lineSeparator)
    writer.write(values.mkString(delimiter.toString) + lineSeparator)

    writer.close()

    val parser = DelimitedDataParser(temp, delimiter = delimiter)
    parser.next()
  }

  "SeshatMerge.seshatHeader" should "inject new IDs as new INFO fields" in {
    val ids    = Seq("new_field_1", "new_field_2")
    val header = VcfBuilder.DefaultHeader
    val actual = SeshatMerge.seshatHeader(header, ids = ids)
    val field1 = actual.infos.find(_.id == "Seshat_new_field_1").value
    field1.description shouldBe VcfUtil.Utf8DescriptionPrefixSentinel
    field1.source.value shouldBe "SeshatMerge"
    field1.version.value shouldBe PackageVersion
    val field2 = actual.infos.find(_.id == "Seshat_new_field_2").value
    field2.description shouldBe VcfUtil.Utf8DescriptionPrefixSentinel
    field2.source.value shouldBe "SeshatMerge"
    field2.version.value shouldBe PackageVersion
  }

  it should "UTF-8 encode the ID if there are characters that need encoding" in {
    val ids    = Seq("new field=1", "new\tfield%2")
    val header = VcfBuilder.DefaultHeader
    val actual = SeshatMerge.seshatHeader(header, ids = ids)
    actual.infos.map(_.id) should contain ("Seshat_new%20field%3D1")
    val field1 = actual.infos.find(_.id == "Seshat_new%20field%3D1").value
    field1.description shouldBe VcfUtil.Utf8DescriptionPrefixSentinel
    field1.source.value shouldBe "SeshatMerge"
    field1.version.value shouldBe PackageVersion
    actual.infos.map(_.id) should contain ("Seshat_new%09field%252")
    val field2 = actual.infos.find(_.id == "Seshat_new%09field%252").value
    field2.description shouldBe VcfUtil.Utf8DescriptionPrefixSentinel
    field2.source.value shouldBe "SeshatMerge"
    field2.version.value shouldBe PackageVersion
  }

  "SeshatMerge.seshatAttrs" should "build a new list of attrs from IDs and an annotation row" in {
    val row = buildRow(
      Map(
        "POS"    -> "1",
        "CHROM"  -> Chr1,
        "REF"    -> "A",
        "Sample" -> "sample_1",
        "extra"  -> "extra_val"
      )
    )
    SeshatMerge.seshatAttrs(row, Seq("extra")) shouldBe Seq(("Seshat_extra", "extra_val"))
  }

  it should "UTF-8 encode the ID and value if there are characters that need encoding" in {
    val row = buildRow(
      Map(
        "POS"    -> "1",
        "CHROM"  -> Chr1,
        "REF"    -> "A",
        "Sample" -> "sample_1",
        "e==ra"  -> "extra val"
      )
    )
    SeshatMerge.seshatAttrs(row, Seq("e==ra")) shouldBe Seq(("Seshat_e%3D%3Dra", "extra%20val"))
  }

  "Seshat.validateVariantAndAnnotation" should "validate a Variant against a given delimited-data Row" in {
    val alleles  = AlleleSet("A", "T")
    val genotype = Genotype(alleles, sample = "sample_1", calls = alleles.alts)
    val variant  = Variant(Chr1, pos = 1, id = None, alleles = alleles, genotypes = Map("sample_1" -> genotype))
    val row      = buildRow(
      Map(
        "POS"    -> "1",
        "CHROM"  -> Chr1,
        "REF"    -> alleles.ref.toString(),
        "Sample" -> "sample_1",
        "extra"  -> "extra"
      )
    )
    noException shouldBe thrownBy { SeshatMerge.validateVariantAndAnnotation(variant, row) }
  }

  it should "raise an exception if the sample cannot be found in the row" in {
    val alleles  = AlleleSet("A", "T")
    val genotype = Genotype(alleles, sample = "sample_1", calls = alleles.alts)
    val variant  = Variant(Chr1, pos = 1, id = None, alleles = alleles, genotypes = Map("sample_1" -> genotype))
    val row      = buildRow(
      Map(
        "POS"    -> "1",
        "CHROM"  -> Chr1,
        "REF"    -> alleles.ref.toString(),
        "Sample" -> "sample_XXX",
        "extra"  -> "extra"
      )
    )
    an[IllegalArgumentException] shouldBe thrownBy { SeshatMerge.validateVariantAndAnnotation(variant, row) }
  }

  it should "raise an exception if the chromosome does not match the row" in {
    val alleles  = AlleleSet("A", "T")
    val genotype = Genotype(alleles, sample = "sample_1", calls = alleles.alts)
    val variant  = Variant(Chr1, pos = 1, id = None, alleles = alleles, genotypes = Map("sample_1" -> genotype))
    val row      = buildRow(
      Map(
        "POS"    -> "1",
        "CHROM"  -> "CHR2",
        "REF"    -> alleles.ref.toString(),
        "Sample" -> "sample_1",
        "extra"  -> "extra"
      )
    )
    an[IllegalArgumentException] shouldBe thrownBy { SeshatMerge.validateVariantAndAnnotation(variant, row) }
  }

  it should "raise an exception if the position does not match the row" in {
    val alleles  = AlleleSet("A", "T")
    val genotype = Genotype(alleles, sample = "sample_1", calls = alleles.alts)
    val variant  = Variant(Chr1, pos = 1, id = None, alleles = alleles, genotypes = Map("sample_1" -> genotype))
    val row      = buildRow(
      Map(
        "POS"    -> "2",
        "CHROM"  -> Chr1,
        "REF"    -> alleles.ref.toString(),
        "Sample" -> "sample_1",
        "extra"  -> "extra"
      )
    )
    an[IllegalArgumentException] shouldBe thrownBy { SeshatMerge.validateVariantAndAnnotation(variant, row) }
  }

  it should "raise an exception if the reference allele does not match the row" in {
    val alleles  = AlleleSet("A", "T")
    val genotype = Genotype(alleles, sample = "sample_1", calls = alleles.alts)
    val variant  = Variant(Chr1, pos = 1, id = None, alleles = alleles, genotypes = Map("sample_1" -> genotype))
    val row      = buildRow(
      Map(
        "POS"    -> "1",
        "CHROM"  -> Chr1,
        "REF"    -> "T",
        "Sample" -> "sample_1",
        "extra"  -> "extra"
      )
    )
    an[IllegalArgumentException] shouldBe thrownBy { SeshatMerge.validateVariantAndAnnotation(variant, row) }
  }

  "SeshatMerge" should "run end-to-end annotating two variants with all complex cases covered" in {
    val builder = VcfBuilder(Seq("sample_1"))
    builder.add(
      chrom   = Chr1,
      pos     = 1,
      alleles = Seq("A", "T"),
      gts     = Seq(Gt("sample_1", "A/T"))
    )
    val annotation1 = Map(
      "POS"    -> "1",
      "CHROM"  -> Chr1,
      "REF"    -> "A",
      "Sample" -> "sample_1",
      "NEW=K"  -> "N/A;N/A;N/A=hi,mom"
    )
    builder.add(
      chrom   = Chr1,
      pos     = 2,
      alleles = Seq("C", "G"),
      gts     = Seq(Gt("sample_1", "C/G"))
    )
    val annotation2 = Map(
      "POS"    -> "2",
      "CHROM"  -> Chr1,
      "REF"    -> "C",
      "Sample" -> "sample_1",
      "NEW=K"  -> "0.5"
    )
    val input       = builder.toTempFile()
    val output      = makeTempFile(getClass.getSimpleName, VcfExtension)
    val annotations = makeTempFile(getClass.getSimpleName, TextExtension)
    val writer      = Io.toWriter(annotations)
    val keys        = annotation1.keySet.toSeq.sorted.toList
    val values1     = keys.map(annotation1.apply)
    val values2     = keys.map(annotation2.apply)

    writer.write(keys.mkString(Delimiter.toString) + lineSeparator)
    writer.write(values1.mkString(Delimiter.toString) + lineSeparator)
    writer.write(values2.mkString(Delimiter.toString) + lineSeparator)
    writer.close()

    new SeshatMerge(
      input       = input,
      annotations = annotations,
      output      = output
    ).execute()

    val source = VcfSource(output)
    val List(outputVariant1, outputVariant2) = source.toList
    val List(inputVariant1,  inputVariant2)  = builder.toList
    source.header.infos.map(_.id) should contain ("Seshat_NEW%3DK")

    inputVariant1.chrom shouldBe outputVariant1.chrom
    inputVariant1.alleles.ref shouldBe outputVariant1.alleles.ref
    inputVariant1.alleles.alts should contain theSameElementsInOrderAs outputVariant1.alleles.alts
    inputVariant1.genotypes.keySet should contain theSameElementsAs outputVariant1.genotypes.keySet

    inputVariant2.chrom shouldBe outputVariant2.chrom
    inputVariant2.alleles.ref shouldBe outputVariant2.alleles.ref
    inputVariant2.alleles.alts should contain theSameElementsInOrderAs outputVariant2.alleles.alts
    inputVariant2.genotypes.keySet should contain theSameElementsAs outputVariant2.genotypes.keySet

    outputVariant1.attrs.keySet should contain ("Seshat_NEW%3DK")
    outputVariant1.attrs("Seshat_NEW%3DK") shouldBe "N/A%3BN/A%3BN/A%3Dhi%2Cmom"
    outputVariant2.attrs.keySet should contain ("Seshat_NEW%3DK")
    outputVariant2.attrs("Seshat_NEW%3DK") shouldBe "0.5"
  }

  it should "raise an exception if there are more variants in the input than annotations" in {
    val builder = VcfBuilder(Seq("sample_1"))
    builder.add(
      chrom   = Chr1,
      pos     = 1,
      alleles = Seq("A", "T"),
      gts     = Seq(Gt("sample_1", "A/T"))
    )
    val annotation1 = Map(
      "POS"    -> "1",
      "CHROM"  -> Chr1,
      "REF"    -> "A",
      "Sample" -> "sample_1",
      "NEW=K"  -> "N/A;N/A;N/A=hi,mom"
    )
    builder.add(
      chrom   = Chr1,
      pos     = 2,
      alleles = Seq("C", "G"),
      gts     = Seq(Gt("sample_1", "C/G"))
    )

    val input       = builder.toTempFile()
    val output      = makeTempFile(getClass.getSimpleName, VcfExtension)
    val annotations = makeTempFile(getClass.getSimpleName, TextExtension)
    val writer      = Io.toWriter(annotations)
    val keys        = annotation1.keySet.toSeq.sorted.toList
    val values1     = keys.map(annotation1.apply)

    writer.write(keys.mkString(Delimiter.toString) + lineSeparator)
    writer.write(values1.mkString(Delimiter.toString) + lineSeparator)
    writer.close()

    val _ = an[IllegalArgumentException] shouldBe thrownBy {
      new SeshatMerge(
        input       = input,
        annotations = annotations,
        output      = output
      ).execute()
    }
  }

  it should "raise an exception if there are more annotations in the input than variants" in {
    val builder = VcfBuilder(Seq("sample_1"))
    builder.add(
      chrom   = Chr1,
      pos     = 1,
      alleles = Seq("A", "T"),
      gts     = Seq(Gt("sample_1", "A/T"))
    )
    val annotation1 = Map(
      "POS"    -> "1",
      "CHROM"  -> Chr1,
      "REF"    -> "A",
      "Sample" -> "sample_1",
      "NEW=K"  -> "N/A;N/A;N/A=hi,mom"
    )
    val annotation2 = Map(
      "POS"    -> "2",
      "CHROM"  -> Chr1,
      "REF"    -> "C",
      "Sample" -> "sample_1",
      "NEW=K"  -> "0.5"
    )
    val input       = builder.toTempFile()
    val output      = makeTempFile(getClass.getSimpleName, VcfExtension)
    val annotations = makeTempFile(getClass.getSimpleName, TextExtension)
    val writer      = Io.toWriter(annotations)
    val keys        = annotation1.keySet.toSeq.sorted.toList
    val values1     = keys.map(annotation1.apply)
    val values2     = keys.map(annotation2.apply)

    writer.write(keys.mkString(Delimiter.toString) + lineSeparator)
    writer.write(values1.mkString(Delimiter.toString) + lineSeparator)
    writer.write(values2.mkString(Delimiter.toString) + lineSeparator)
    writer.close()

    val _ = an[IllegalArgumentException] shouldBe thrownBy {
      new SeshatMerge(
        input       = input,
        annotations = annotations,
        output      = output
      ).execute()
    }
  }
}
