# seshatio

[![CI](https://github.com/clintval/seshatio/actions/workflows/python_package.yml/badge.svg?branch=main)](https://github.com/clintval/seshatio/actions/workflows/python_package.yml?query=branch%3Amain)
[![Python Versions](https://img.shields.io/badge/python-3.11_|_3.12-blue)](https://github.com/clintval/seshatio)
[![MyPy Checked](http://www.mypy-lang.org/static/mypy_badge.svg)](http://mypy-lang.org/)
[![Poetry](https://img.shields.io/endpoint?url=https://python-poetry.org/badge/v0.json)](https://python-poetry.org/)
[![Ruff](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/astral-sh/ruff/main/assets/badge/v2.json)](https://docs.astral.sh/ruff/)

A toTools for programatically annotating VCFs with the Seshat TP53 database.

## Recommended Installation

Install the Python package and dependency management tool [`poetry`](https://python-poetry.org/docs/#installation) using official documentation.
You must have Python 3.11 or greater available on your system path, which could be managed by [`mamba`](https://mamba.readthedocs.io/en/latest/installation/mamba-installation.html), [`pyenv`](https://github.com/pyenv/pyenv), or another package manager. 
Finally, install the dependencies of the project with:

```console
poetry install
```

To check successful installation, run:

```console
poetry run seshatio hello --name Fulcrum
```

## Installing into a Mamba Environment

Install the Python package and dependency management tool [`poetry`](https://python-poetry.org/docs/#installation) and the environment manager [`mamba`](https://mamba.readthedocs.io/en/latest/installation/mamba-installation.html) using official documentation.
Create and activate a virtual environment with Python 3.11 or greater:

```console
mamba create -n seshatio python=3.11
mamba activate seshatio
```

Then, because Poetry will auto-detect an activated environment, install the project with:

```console
poetry install
```

To check successful installation, run:

```console
seshatio hello --name Fulcrum
```

## Development and Testing

See the [contributing guide](./CONTRIBUTING.md) for more information.
