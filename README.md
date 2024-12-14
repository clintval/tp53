# tp53

[![PyPi Release](https://badge.fury.io/py/tp53.svg)](https://badge.fury.io/py/tp53)
[![CI](https://github.com/clintval/tp53/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/clintval/tp53/actions/workflows/tests.yml?query=branch%3Amain)
[![Python Versions](https://img.shields.io/badge/python-3.11_|_3.12_|_3.13-blue)](https://github.com/clintval/typeline)
[![basedpyright](https://img.shields.io/badge/basedpyright-checked-42b983)](https://docs.basedpyright.com/latest/)
[![mypy](https://www.mypy-lang.org/static/mypy_badge.svg)](https://mypy-lang.org/)
[![Poetry](https://img.shields.io/endpoint?url=https://python-poetry.org/badge/v0.json)](https://python-poetry.org/)
[![Ruff](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/astral-sh/ruff/main/assets/badge/v2.json)](https://docs.astral.sh/ruff/)

Tools for programmatically annotating VCFs with the Seshat TP53 database.

## Installation

The package can be installed with `pip`:

```console
pip install tp53
```

## Upload a VCF to Seshat

Upload a VCF to the [Seshat TP53 annotation server](http://vps338341.ovh.net/) using a headless browser.

```bash
❯ python -m tp53.seshat.upload_vcf \
    --input "sample.library.vcf" \
    --email "example@gmail.com"
```
```console
INFO:tp53.seshat.upload_vcf:Uploading 0 %...
INFO:tp53.seshat.upload_vcf:Uploading 53%...
INFO:tp53.seshat.upload_vcf:Uploading 53%...
INFO:tp53.seshat.upload_vcf:Uploading 60%...
INFO:tp53.seshat.upload_vcf:Uploading 60%...
INFO:tp53.seshat.upload_vcf:Uploading 66%...
INFO:tp53.seshat.upload_vcf:Uploading 66%...
INFO:tp53.seshat.upload_vcf:Uploading 80%...
INFO:tp53.seshat.upload_vcf:Uploading 80%...
INFO:tp53.seshat.upload_vcf:Upload complete!
```

This tool is used to programmatically configure and upload batch variants in VCF format to the Seshat annotation server.
The tool works by building a headless Chrome browser instance and then interacting with the Seshat website directly through simulated key presses and mouse clicks.
Unfortunately, Seshat does not provide a native programmatic API and one could not be reverse engineered.
Seshat also utilizes custom JavaScript in their form processing, so a lightweight approach of simply interacting with the HTML form elements was also not possible.

###### VCF Input Requirements

Seshat will not let the user know why a VCF fails to annotate, but it has been observed that Seshat can fail to parse some of [VarDictJava](https://github.com/AstraZeneca-NGS/VarDictJava)'s structural variants (SVs) as valid variant records.
One solution that has worked in the past is to remove SVs.
The following command will exclude all variants with a non-empty SVTYPE INFO key:

```bash
❯ bcftools view sample.library.vcf \
    --exclude 'SVTYPE!="."' \
  > sample.library.noSV.vcf
```

###### Automation

There are no terms and conditions posted on the Seshat annotation server's website, and there is no server-side `robots.txt` rule set.
In lieu of usage terms, we strongly encourage all users of this script to respect the Seshat resource by adhering to the following best practice:

- **Minimize Load**: Limit the rate of requests to the server
- **Minimize Connections**: Limit the number of concurrent requests

If you need to batch process dozens, or hundreds, of VCF callsets, you may consider improving this underlying Python script to randomize the user agent and IP address of your headless browser session to prevent from being labelled as a bot.

###### Environment Setup

This script relies on Google Chrome:

```console
❯ brew install --cask google-chrome
```

Distributions of MacOS may require you to authenticate the Chrome driver ([link](https://stackoverflow.com/a/60362134)).

## Download a Seshat Annotation from Gmail

Download [Seshat](http://vps338341.ovh.net/) VCF annotations by awaiting a server-generated email.

```bash
❯ python -m tp53.seshat.find_in_gmail \
    --input "sample.library.vcf" \
    --output "sample.library" \
    --credentials "~/.secrets/credentials.json"
```
```console
INFO:tp53.seshat.find_in_gmail:Successfully logged into the Gmail service.
INFO:tp53.seshat.find_in_gmail:Querying for a VCF named: sample.library.vcf
INFO:tp53.seshat.find_in_gmail:Searching Gmail messages with: sample.library.vcf from:support@genevia.fi newer_than:5h subject:"Results of batch analysis"
INFO:tp53.seshat.find_in_gmail:Message found with the following metadata: {'id': '193c310d2714b389', 'threadId': '193c30b7244e2662'}
INFO:tp53.seshat.find_in_gmail:Message contents are as follows:
INFO:tp53.seshat.find_in_gmail:  Results of batch analysis
INFO:tp53.seshat.find_in_gmail:  Analyzed batch file:
INFO:tp53.seshat.find_in_gmail:  sample.library.vcf
INFO:tp53.seshat.find_in_gmail:  Time taken to run the analysis:
INFO:tp53.seshat.find_in_gmail:  0 minutes 10 seconds
INFO:tp53.seshat.find_in_gmail:  Summary:
INFO:tp53.seshat.find_in_gmail:  The input file contained
INFO:tp53.seshat.find_in_gmail:    23 mutations out of which
INFO:tp53.seshat.find_in_gmail:    23 were TP53 mutations.
INFO:tp53.seshat.find_in_gmail:Writing attachment to ZIP archive: sample.library.vcf.seshat.zip
INFO:tp53.seshat.find_in_gmail:Extracting ZIP archive: sample.library.vcf.seshat.zip
INFO:tp53.seshat.find_in_gmail:Output file renamed to: sample.library.seshat.short-20241214_034753_129732.tsv
INFO:tp53.seshat.find_in_gmail:Output file renamed to: sample.library.seshat.long-20241214_034753_217420.tsv
```

This tool is used to programmatically wait for, and retrieve, a batch results email from the Seshat TP53 annotation server.
The tool works by searching a user-controlled Gmail inbox for a recent Seshat email that contains the result annotations for a given VCF input file, by name.
It is critically important to be aware that there is no way to prove which annotation files, as they arrive via email, are to be linked with which VCF file on disk.

This tool assists in the correct pairing of VCF input files, and subsequent annotation files, by letting you specify how many hours back in time you will let the Gmail query search (`--newer-than`).
Limiting the window of time in which an email should have arrived minimizes the chance of discovering stale annotation files from an old Seshat execution in the cases where VCF filenames may be non-unique.
If the batch results email from the Seshat annotation server has not yet arrived, this tool will wait a set number of seconds (`--wait-for`) before exiting with exception.
It normally takes less than 1 minute for the Seshat server to annotate an average TP53-only VCF.

###### Search Criteria

The following rules are used to find annotation files:

1. The email contains the filename of the input VCF
2. The email subject line must contain "Results of batch analysis"
3. The email is at least `--newer-than` hours old
4. The email is from the address [support@genevia.fi](mailto:support@genevia.fi)

###### Outputs:

- `<output>.seshat.long-\\d{8}_\\d{6}_\\d{6}.tsv`: The long format Seshat annotations for the input VCF
- `<output>.seshat.short-\\d{8}_\\d{6}_\\d{6}.tsv`: The short format Seshat annotations for the input VCF
- `<output>.seshat.zip`: The original ZIP archive from Seshat

###### Gmail Authentication

After installing all Python dependencies, you must create a Google developer's OAuth file.
First-time 2FA may be required depending on the configuration of your Gmail service.
If 2FA is required, then this script will block until you acknowledge your 2FA prompt.
A 2FA prompt is often delivered through an auto-opening web browser.

To create a Google developer's OAuth file, navigate to the following URL and follow the instructions.

- [Authorize Credentials for a Desktop Application](https://developers.google.com/gmail/api/quickstart/python#authorize_credentials_for_a_desktop_application)

Ensure your OAuth file is configured as a "Desktop app" and then download the credentials as JSON.
Save your credentials file somewhere safe, ideally in a secure user folder with restricted permissions (`chmod 700`).
Set your OAuth file permissions to also restrict unwarranted access (`chmod 600`).

This script will store a cached token after first-time authentication is successful.
This cached token can be found in the user's home directory within a hidden directory.
Token caching greatly speeds up continued executions of this script.
As of now, the token is cached at the following location:

```bash
"~/.tp53/seshat/seshat-gmail-find-token.pickle"
```

If the cached token is missing, or becomes stale, then you will need to provide your OAuth credentials file.

A typical Google developer's OAuth file is of the format:

```json
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
```

###### Server Failures

If Seshat fails to annotate the VCF file but still emails the user a response, then this tool will emit the email body to standard error and exit with a non-zero status.

## Development and Testing

See the [contributing guide](https://github.com/clintval/tp53/blob/main/CONTRIBUTING.md) for more information.

## References

- [Soussi, Thierry, et al. “Recommendations for Analyzing and Reporting TP53 Gene Variants in the High-Throughput Sequencing Era.” Human Mutation, vol. 35, no. 6, 2014, pp. 766–778., doi:10.1002/humu.22561](https://doi.org/10.1002/humu.22561)
