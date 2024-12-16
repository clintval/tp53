# tp53 & seshat

[![Install with Bioconda](https://img.shields.io/badge/Install%20with-bioconda-brightgreen.svg)](http://bioconda.github.io/recipes/seshat/README.html)
[![Anaconda Version](https://anaconda.org/bioconda/seshat/badges/version.svg)](https://anaconda.org/bioconda/seshat)
[![Language](https://img.shields.io/badge/language-scala-c22d40.svg)](https://www.scala-lang.org/)
[![Java Version](https://img.shields.io/badge/java-11,17,21-c22d40.svg)](https://github.com/AdoptOpenJDK/homebrew-openjdk)
[![Python Versions](https://img.shields.io/badge/python-3.11_|_3.12_|_3.13-blue)](https://github.com/clintval/typeline)

Tools for programmatically annotating VCFs with the [Seshat TP53 database](http://vps338341.ovh.net/).

![Mount Shuksan](.github/img/cover.jpg)

## Installation

Install with the Conda or Mamba package manager after setting your [Bioconda channels](https://bioconda.github.io/#usage):

```console
conda install seshat
```

## Quick Usage Example

For round-trip Seshat annotation of a VCF file, execute a command like:

```bash
❯ seshat round-trip \
    --input "sample.library.vcf" \
    --output "sample.library" \
    --email "example@example.com"
```
```console
15:23:53 INFO  SeshatUploadVcf - Executing command: python3 -m tp53.seshat.upload_vcf --input sample.library.vcf --assembly hg38 --email example@example.com --url http://vps338341.ovh.net/batch_analysis --wait-for 5
15:23:54 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Uploading 0 %...
15:23:54 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Uploading 60%...
15:23:54 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Uploading 73%...
15:23:54 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Upload complete!
15:23:54 INFO  SeshatFindInGmail - Executing command: python3 -m tp53.seshat.find_in_gmail --input sample.library.vcf --output sample.library --newer-than 5 --wait-for 200
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Successfully logged into the Gmail service.
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Querying for a VCF named: sample.library.vcf
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Searching Gmail messages with: sample.library.vcf from:support@genevia.fi newer_than:5h subject:"Results of batch analysis"
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Message found with the following metadata: {'id': '193cbfdcdb5bc87c', 'threadId': '193cbfdcdb5bc87c'}
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Message contents are as follows:
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Results of batch analysis
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Analyzed batch file:
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  sample.library.vcf
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Time taken to run the analysis:
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  0 minutes 10 seconds
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Summary:
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  The input file contained
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:      23 mutations out of which
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:      23 were TP53 mutations.
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Writing attachment to ZIP archive: sample.library.vcf.seshat.zip
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Extracting ZIP archive: sample.library.vcf.seshat.zip
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Output file renamed to: sample.library.seshat.short-20241215_212333_875215.tsv
15:23:55 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Output file renamed to: sample.library.seshat.long-20241215_212333_969299.tsv
15:23:55 INFO  SeshatMerge - Starting to zip annotations and VCF variants.
15:23:56 INFO  SeshatMerge - Successfully annotated variant calls.
```

See [Upload a VCF to Seshat](#upload-a-vcf-to-seshat) and [Download a Seshat Annotation from Gmail](#download-a-seshat-annotation-from-gmail) for pre-requisite setup.

## Upload a VCF to Seshat

Upload a VCF to the Seshat annotation webserver using a headless browser.

```bash
❯ seshat upload-vcf \
    --input "sample.library.vcf" \
    --email "example@gmail.com"
```
```console
16:11:02 INFO  SeshatUploadVcf - Executing command: python3 -m tp53.seshat.upload_vcf --input sample.library.vcf --assembly hg38 --email example@example.com --url http://vps338341.ovh.net/batch_analysis --wait-for 200
16:11:03 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Uploading 0 %...
16:11:03 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Uploading 60%...
16:11:03 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Uploading 66%...
16:11:03 INFO  SeshatUploadVcf - INFO:tp53.seshat.upload_vcf:Upload complete!
```

This tool is used to programmatically configure and upload batch variants in VCF format to the Seshat annotation server.
The tool works by building a headless Chrome browser instance and then interacting with the Seshat website directly through simulated key presses and mouse clicks.

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

###### Environment Setup

This script relies on Google Chrome:

```console
❯ brew install --cask google-chrome
```

Distributions of MacOS may require you to authenticate the Chrome driver ([link](https://stackoverflow.com/a/60362134)).

## Download a Seshat Annotation from Gmail

Download Seshat VCF annotations by awaiting a server-generated email.

```bash
❯ seshat find-in-gmail \
    --input "sample.library.vcf" \
    --output "sample.library" \
    --credentials "~/.secrets/credentials.json"
```
```console
16:14:06 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Successfully logged into the Gmail service.
16:14:06 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Querying for a VCF named: sample.library.vcf
16:14:06 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Searching Gmail messages with: sample.library.vcf from:support@genevia.fi newer_than:10h subject:"Results of batch analysis"
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Message found with the following metadata: {'id': '193cc295aa6ae27d', 'threadId': '193cc295aa6ae27d'}
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Message contents are as follows:
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Results of batch analysis
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Analyzed batch file:
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  sample.library.vcf
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Time taken to run the analysis:
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  0 minutes 10 seconds
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  Summary:
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:  The input file contained
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:      23 mutations out of which
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:      23 were TP53 mutations.
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Writing attachment to ZIP archive: sample.library.vcf.seshat.zip
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Extracting ZIP archive: sample.library.vcf.seshat.zip
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Output file renamed to: sample.library.seshat.short-20241215_221114_586973.tsv
16:14:07 INFO  SeshatFindInGmail - INFO:tp53.seshat.find_in_gmail:Output file renamed to: sample.library.seshat.long-20241215_221114_675170.tsv
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

You must create a Google developer's OAuth file.
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

## Merge Annotations into a VCF

Merge Seshat annotations into the `INFO` fields of the VCF.

```bash
❯ seshat merge \
    --input "sample.library.vcf" \
    --annotations "sample.library.seshat.long-20241215_221114_675170.tsv" \
    --output "sample.library.seshat.annotated.vcf"
```

```console
16:16:34 INFO  SeshatMerge - Starting to zip annotations and VCF variants.
16:16:34 INFO  SeshatMerge - Successfully annotated variant calls.
```

## Development and Testing

See the [contributing guide](./CONTRIBUTING.md) for more information.

## References

- [Soussi, Thierry, et al. “Recommendations for Analyzing and Reporting TP53 Gene Variants in the High-Throughput Sequencing Era.” Human Mutation, vol. 35, no. 6, 2014, pp. 766–778., doi:10.1002/humu.22561](https://doi.org/10.1002/humu.22561)
