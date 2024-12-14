"""
Upload a VCF to the Seshat TP53 annotation server using a headless browser.

This tool is used to programmatically configure and upload batch variants in VCF
format to the Seshat annotation server. The tool works by building a headless
Chrome browser instance and then interacting with the Seshat website directly
through simulated key presses and mouse clicks. Unfortunately, Seshat does not
provide a native programmatic API and one could not be reverse engineered.
Seshat also utilizes custom JavaScript in their form processing, so a
lightweight approach of simply interacting with the HTML form elements was
also not possible.

#### VCF Input Requirements

Seshat will not let the user know why a VCF fails to annotate, but it has
been observed that Seshat can fail to parse some of VarDictJava's structural
variants (SVs) as valid variant records. One solution that has worked in the
past is to remove SVs. The following command will exclude all variants with a
non-empty SVTYPE INFO key:

  bcftools view in.vcf --exclude 'SVTYPE!="."' > out.noSV.vcf

#### Automation

There are no terms and conditions posted on the Seshat annotation server's
website, and there is no server-side `robots.txt` rule set. In lieu of usage
terms, we strongly encourage all users of this script to respect the Seshat
resource by adhering to the following best practice:

  - Minimize Load: Limit the rate of requests to the server
  - Minimize Connections: Limit the number of concurrent requests

If you need to batch process dozens, or hundreds, of VCF callsets, you may
consider improving this underlying Python script to randomize the user agent and
IP address of your headless browser session to prevent from being labelled as a
bot.

#### Environment Setup

This script relies on Chrome:

  brew install --cask google-chrome

Distributions of MacOS require you to authenticate the Chrome driver:

  - https://stackoverflow.com/a/60362134

#### References

  1. Soussi, Thierry, et al. “Recommendations for Analyzing and Reporting TP53
     Gene Variants in the High-Throughput Sequencing Era.” Human Mutation,
     vol. 35, no. 6, 2014, pp. 766–778., doi:10.1002/humu.22561.

───────
"""

import argparse
import logging
import sys
from pathlib import Path

from ._upload_vcf import DEFAULT_REMOTE_URL
from ._upload_vcf import HumanGenomeAssembly
from ._upload_vcf import upload_vcf

if __name__ == "__main__":
    formatter = argparse.RawTextHelpFormatter

    cli_args = sys.argv[1:]

    parser = argparse.ArgumentParser(
        prog="upload_vcf",
        description=__doc__,
        add_help=True,
        formatter_class=formatter,
        epilog=r"Copyright © Clint Valentine 2024",
    )

    _ = parser.add_argument(
        "--input",
        required=True,
        type=Path,
        help="The path to the VCF to upload.",
    )
    _ = parser.add_argument(
        "--email",
        required=True,
        type=str,
        help="The email address to receive annotated variants at.",
    )
    _ = parser.add_argument(
        "--assembly",
        type=HumanGenomeAssembly,
        default=HumanGenomeAssembly.hg38,
        help="The human genome assembly of the VCF.\n(default: hg38)",
    )
    _ = parser.add_argument(
        "--url",
        type=str,
        default=DEFAULT_REMOTE_URL,
        help="The Seshat TP53 web server URL.\n(default: http://vps338341.ovh.net/batch_analysis)",
    )
    _ = parser.add_argument(
        "--wait_for",
        type=int,
        default=5,
        help="Seconds to wait for upload to occur before failure.\n(default: 5)",
    )
    args = parser.parse_args(cli_args)

    logging.basicConfig(datefmt="[%X]", level=logging.INFO)

    upload_vcf(
        vcf=args.input,
        email=args.email,
        assembly=args.assembly,
        url=args.url,
        wait_for=args.wait_for,
    )
