import logging
import time
from datetime import datetime
from datetime import timedelta
from enum import StrEnum
from enum import auto
from logging import Logger
from pathlib import Path

from chromedriver_py import binary_path
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.remote.webdriver import WebDriver as RemoteWebDriver

from .._exceptions import SeshatError

logger: Logger = logging.getLogger("tp53.seshat.upload_vcf")

DEFAULT_REMOTE_URL: str = "http://vps338341.ovh.net/batch_analysis"
"""The default remote Seshat batch analysis URL."""

SUCCESS: str = "complete"
"""File upload status sentinel word indicating a successful upload."""


class HumanGenomeAssembly(StrEnum):
    """Enumerations of the Seshat upload supported human genome assembly."""

    hg18 = auto()
    """The human genome assembly GRCh37 (hg18)."""

    hg19 = auto()
    """The human genome assembly GRCh37 (hg19)."""

    hg38 = auto()
    """The human genome assembly GRCh37 (hg19)."""


def upload_status(driver: RemoteWebDriver) -> str:
    """Query the file uploading status and return its text representation."""
    modal = driver.find_element(By.XPATH, '//*[@id="uploading-status-text"]')
    inner = modal.get_attribute("innerText")
    if inner is None:
        raise SeshatError("Modal CCS on website has changed!")
    return inner.strip()


def upload_vcf(
    vcf: Path | str,
    email: str,
    assembly: HumanGenomeAssembly,
    url: str = DEFAULT_REMOTE_URL,
    wait_for: int = 5,
) -> None:
    """
    Upload a VCF to the TP53 Seshat web server.

    Args:
        vcf: The path to the VCF to upload.
        email: The email address to receive annotated variants at.
        assembly: The human genome assembly of the VCF.
        url: The Seshat TP53 web server URL.
        wait_for: Seconds to wait for upload to occur before failure.
    """
    vcf = str(Path(vcf).expanduser().absolute())

    service = webdriver.ChromeService(executable_path=binary_path)
    options = webdriver.ChromeOptions()
    options.add_argument("headless")

    driver = webdriver.Chrome(service=service, options=options)

    driver.get(url)
    driver.find_element(By.XPATH, f'//select[@id="reference"]/option[@value="{assembly}"]').click()
    driver.find_element(By.XPATH, '//input[@id="email"]').send_keys(email)
    driver.find_element(By.XPATH, '//input[@id="upload-batch-file-input"]').send_keys(vcf)
    upload_start = datetime.now()
    driver.find_element(By.XPATH, '//button[@id="batch-analysis-upload"]').click()

    status: str = ""
    while (SUCCESS not in status) and datetime.now() < upload_start + timedelta(seconds=wait_for):
        status = upload_status(driver)
        logger.info(status)
        time.sleep(0.1)

    driver.quit()

    if SUCCESS not in status:
        raise SeshatError(f"Upload was not successful with status: {status}")
