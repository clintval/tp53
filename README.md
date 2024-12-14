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

## Upload a VCF to the Seshat TP53 Annotation Server

Upload a VCF to the [Seshat TP53 annotation server](http://vps338341.ovh.net/) using a headless browser.

```bash
❯ python -m tp53.seshat.upload_vcf \
    --input "input.vcf" \
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
❯ bcftools view in.vcf --exclude 'SVTYPE!="."' > out.noSV.vcf
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

## Development and Testing

See the [contributing guide](./CONTRIBUTING.md) for more information.

## References

- [Soussi, Thierry, et al. “Recommendations for Analyzing and Reporting TP53 Gene Variants in the High-Throughput Sequencing Era.” Human Mutation, vol. 35, no. 6, 2014, pp. 766–778., doi:10.1002/humu.22561](https://doi.org/10.1002/humu.22561)
