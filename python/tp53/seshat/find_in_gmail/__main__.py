r"""
Download Seshat VCF annotations by awaiting a server-generated email.

This tool is used to programmatically wait for, and retrieve, a batch results
email from the Seshat TP53 annotation server. The tool works by searching a
user-controlled Gmail inbox for a recent Seshat email that contains the result
annotations for a given VCF input file, by name. It is critically important to
be aware that there is no way to prove which annotation files, as they arrive
via email, are to be linked with which VCF file on disk. This tool assists in
the correct pairing of VCF input files, and subsequent annotation files, by
letting you specify how many hours back in time you will let the Gmail query
search (`--newer-than`). Limiting the window of time in which an email should
have arrived minimizes the chance of discovering stale annotation files from an
old Seshat execution in the cases where VCF filenames may be non-unique.

If the batch results email from the Seshat annotation server has not yet
arrived, this tool will wait a set number of seconds (`--wait-for`) before
exiting with exception. It normally takes less than 1 minute for the Seshat
server to annotate an average TP53-only VCF.

#### Search Criteria

The following rules are used to find annotation files:

  1. The email contains the filename of the input VCF
  2. The email subject line must contain "Results of batch analysis"
  3. The email is at least `--newer-than` hours old
  4. The email is from the address "support@genevia.fi"

#### Outputs:

  * <output>.seshat.long-\\d{8}_\\d{6}_\\d{6}.tsv:
    The long format Seshat annotations for the input VCF.
  * <output>.seshat.short-\\d{8}_\\d{6}_\\d{6}.tsv:
    The short format Seshat annotations for the input VCF.
  * <output>.seshat.zip:
    The original ZIP archive from Seshat.

#### Gmail Authentication

After installing all Python dependencies, you must create a Google developer's
OAuth file. First-time 2FA may be required depending on the configuration of
your Gmail service. If 2FA is required, then this script will block until you
acknowledge your 2FA prompt. A 2FA prompt is often delivered through an
auto-opening web browser.

To create a Google developer's OAuth file, navigate to the following URL and
follow the instructions.

  - https://developers.google.com/gmail/api/quickstart/python#authorize_credentials_for_a_desktop_application

Ensure your OAuth file is configured as a "Desktop app" and then download the
credentials as JSON. Save your credentials file somewhere safe, ideally in a
secure user folder with restricted permissions (chmod 700). Set your OAuth file
permissions to also restrict unwarranted access (chmod 600).

This script will store a cached token after first-time authentication is
successful. This cached token can be found in the user's home directory within
a hidden directory. Token caching greatly speeds up continued executions of this
script. As of now, the token is cached at the following location:

  - '~/.tp53/seshat/seshat-gmail-find-token.pickle'

If the cached token is missing, or becomes stale, then you will need to provide
your OAuth credentials file.

A typical Google developer's OAuth file is of the format:

  {
    "installed": {
        "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "client_id": "272111863110-csldkfjlsdkfjlksdjflksdincie.apps.googleusercontent.com",
        "client_secret": "sdlfkjsdlkjfijciejijcei",
        "project_id": "gmail-access-2398293892838",
        "redirect_uris": [
          "urn:ietf:wg:oauth:2.0:oob",
          "http://localhost"
        ],
        "token_uri": "https://oauth2.googleapis.com/token"
      }
  }

#### Server Failures

If Seshat fails to annotate the VCF file but still emails the user a response,
then this tool will emit the email body to STDERR and exit with a non-zero
status.

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

from ._find_in_gmail import find_in_gmail

if __name__ == "__main__":
    formatter = argparse.RawTextHelpFormatter

    cli_args = sys.argv[1:]

    parser = argparse.ArgumentParser(
        prog="find_in_gmail",
        description=__doc__,
        add_help=True,
        formatter_class=formatter,
        epilog=r"Copyright © Clint Valentine 2024",
    )

    _ = parser.add_argument(
        "--input",
        required=True,
        type=Path,
        help="The path to the original VCF which was uploaded.",
    )
    _ = parser.add_argument(
        "--output",
        required=True,
        type=Path,
        help="The path to write the TP53 annotations to.",
    )
    _ = parser.add_argument(
        "--newer-than",
        default=5,
        type=int,
        help="Limit search to emails newer than this many hours.\n(default: 5).",
    )
    _ = parser.add_argument(
        "--wait-for",
        default=200,
        type=int,
        help="Seconds to wait for an email to arrive.\n(default: 200).",
    )
    _ = parser.add_argument(
        "--credentials",
        default=None,
        type=Path,
        help="The path to the Gmail authentication credentials JSON.",
    )
    args = parser.parse_args(cli_args)

    logging.basicConfig(datefmt="[%X]", level=logging.INFO)
    logging.getLogger("googleapiclient.discovery_cache").setLevel(logging.ERROR)

    find_in_gmail(
        infile=args.input,
        output=args.output,
        newer_than=args.newer_than,
        wait_for=args.wait_for,
        credentials=args.credentials,
    )
