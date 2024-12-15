package io.cvbio.seshat

import com.fulcrumgenomics.commons.CommonsDef.FilenameSuffix
import io.cvbio.collection.mutable.bimap.BiMap

/** Various helpers for working with VCF files. */
object VcfUtil {

  /** The extension for VCF files. */
  private[seshat] val VcfExtension: FilenameSuffix = ".vcf"

  /** The extension for text files. */
  private[seshat] val TextExtension: FilenameSuffix = ".txt"

  /** The VCF INFO header record description prefix for when the ID and values were UTF-8 encoded. */
  private[seshat] val Utf8DescriptionPrefixSentinel: String = "<<UTF-8 encoded key and value>>"

  /** A bi-directional map of specification invalid characters and their percent string encoding. */
  private[seshat] val SpecialCharCodec: BiMap[Char, String] = BiMap[Char, String](
    ':'  -> "%3A",
    ';'  -> "%3B",
    '='  -> "%3D",
    ','  -> "%2C",
    '\r' -> "%0D",
    '\n' -> "%0A",
    '\t' -> "%09",
    ' '  -> "%20",
    '%'  -> "%25",
  )

  /** Some characters have special meanings in VCF files.  When they appear in strings for use as an INFO/FORMAT key
   * or value they must be encoded. These functions will perform this encoding/decoding for all characters as outlined
   * in VCF 4.3 §1.2. This function is not to be used on a string representation of a list of INFO/FORMAT values.
   * URL encoding is similar, but conflates Space (" ") and Plus ("+").
   * Spaces are allowed in VCF v4.3, but this isn't yet supported by HTSJDK so they are also encoded.
   *
   * In the case of encoding a list of INFO/FORMAT values encoded as a string, you must first split the string on the
   * comma-delimiter, encode, and then re-join the string:
   *
   * {{{
   *     "1.0 2.0,3.0 4.0,5.0".split(',').map(VcfUtil.fieldEncode).mkString(",")
   * }}}
   *
   * Which will return:
   *
   * {{{
   *     "1.0%202.0,3.0%204.0,5.0"
   * }}}
   */
  private[seshat] def fieldEncode(string: String): String = string.map(c => SpecialCharCodec.getOrElse(c, c.toString)).mkString
}
