import os
import pickle
import time
from base64 import urlsafe_b64decode
from collections.abc import Iterable
from collections.abc import Iterator
from datetime import datetime
from datetime import timedelta
from html import unescape
from itertools import chain
from logging import Logger
from logging import getLogger
from pathlib import Path
from typing import cast
from zipfile import ZipFile

from bs4 import BeautifulSoup
from google.auth.transport.requests import Request as AuthTransportRequest
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build as build_google_client

from .._exceptions import SeshatError

logger: Logger = getLogger("tp53.seshat")

DEFAULT_CACHE_PATH: Path = Path("~/.tp53/seshat/seshat-gmail-find-token.pickle")
"""The default Gmail OAuth cache file path."""

FROM: str = "support@genevia.fi"
"""The sender email address for the Seshat server."""

USER_ID: str = "me"
"""The Gmail user ID for your primary Gmail account."""


def strip_gzipped_extension(path: Path | str) -> Path:
    """Remove the GZIP extension only if it is present."""
    path = Path(path)
    return path.with_suffix("") if path.suffix == ".gz" else path


class Part:
    """A node in a multi-part MIME tree as per RFC 2046."""

    def __init__(
        self,
        mime_type: str,
        body: dict[str, object],
        filename: str,
        headers: dict[str, object],
        part_id: str,
    ) -> None:
        self.mime_type = mime_type
        self.body = body
        self.filename = filename
        self.headers = headers
        self.part_id = part_id

    def __repr__(self) -> str:
        return (
            f"{self.__class__.__qualname__}("
            + f"mime_type={self.mime_type}, "
            + f"body={repr(self.body)}, "
            + f"filename={repr(self.filename)}, "
            + f"headers={repr(self.headers)}, "
            + f"part_id={repr(self.part_id)})"
        )


class HtmlPart(Part):
    """An HTML type node in a multi-part MIME tree as per RFC 2046."""

    def __init__(
        self,
        mime_type: str,
        body: dict[str, object],
        filename: str,
        headers: dict[str, object],
        part_id: str,
    ) -> None:
        super().__init__(mime_type, body, filename, headers, part_id)
        assert mime_type == "text/html"
        self.soup = BeautifulSoup(urlsafe_b64decode(cast(str, self.body["data"])), "lxml")

    def all_text(self) -> list[str]:
        if self.soup.body is None:
            raise ValueError("HTML page does not have a body!")
        text: str = "".join(self.soup.body.findAll(text=True))
        lines: list[str] = list(text.replace("\r", "").strip().split("\n"))
        return lines


class TextPart(Part):
    """A plaintext type node in a multi-part MIME tree as per RFC 2046."""

    def __init__(
        self,
        mime_type: str,
        body: dict[str, object],
        filename: str,
        headers: dict[str, object],
        part_id: str,
    ) -> None:
        super().__init__(mime_type, body, filename, headers, part_id)
        assert mime_type == "text/plain"
        self.text = urlsafe_b64decode(cast(str, self.body["data"])).decode("UTF-8")


class Attachment(Part):
    """An attachment type node in a multi-part MIME tree as per RFC 2046."""

    def __init__(
        self,
        mime_type: str,
        body: dict[str, object],
        filename: str,
        headers: dict[str, object],
        part_id: str,
    ) -> None:
        super().__init__(mime_type, body, filename, headers, part_id)
        self.attachment_id = body.get("attachmentId")
        self.attachment_size = body.get("size")


class Message:
    """An email message."""

    def __init__(self, snippet: str, parts: list[Part] | None) -> None:
        self.snippet = snippet
        self.parts = parts if parts is not None else []

    def html_parts(self) -> list[HtmlPart]:
        """Return the HTML parts of this message."""
        parts: list[HtmlPart] = []
        for part in self.parts:
            if isinstance(part, HtmlPart):
                parts.append(part)
        return parts

    def attachments(self) -> list[Attachment]:
        """Return the attachment parts of this message."""
        parts: list[Attachment] = []
        for part in self.parts:
            if isinstance(part, Attachment):
                parts.append(part)
        return parts

    def __repr__(self) -> str:
        return f"<{self.__class__.__qualname__}: {repr(self.snippet)}>"


class Gmail:
    """A client for interacting with Google's Gmail."""

    cache: Path = Path(DEFAULT_CACHE_PATH).expanduser().absolute()

    def __init__(self, auth: Credentials) -> None:
        self.auth: Credentials = auth
        self.client = build_google_client("gmail", "v1", credentials=self.auth)

    @classmethod
    def from_auth_json(cls, path: Path) -> "Gmail":
        """Build a client from credentials located on the filesystem."""
        return cls(cls.get_login_credentials(Path(path)))

    @staticmethod
    def build_parts(payload: dict[str, object]) -> Iterable[Part]:
        """Recursively parse an email payload and return all MIME parts flattened."""
        mime_type: str = cast(str, payload.get("mimeType"))
        if mime_type.startswith("multipart"):
            parts = cast(Iterable[dict[str, object]], payload["parts"])
            return chain.from_iterable((Gmail.build_parts(part) for part in parts))
        else:
            body = cast(dict[str, object], payload["body"])
            filename = cast(str, payload["filename"])
            headers = cast(dict[str, object], payload["headers"])
            part_id = cast(str, payload["partId"])
            if "attachmentId" in body:
                return [Attachment(mime_type, body, filename, headers, part_id)]
            elif mime_type == "text/html":
                return [HtmlPart(mime_type, body, filename, headers, part_id)]
            elif mime_type == "text/plain":
                return [TextPart(mime_type, body, filename, headers, part_id)]
            else:
                raise ValueError(f"Unsupported payload: {payload}")

    @staticmethod
    def get_login_credentials(credentials: Path | None = None) -> Credentials:
        """Login to Gmail using a cached token if available, otherwise use JSON credentials."""
        auth: Credentials | None = None
        if credentials is not None:
            credentials = Path(credentials).expanduser().absolute()
        if not Gmail.cache.parent.exists():
            os.makedirs(Gmail.cache.parent)
            os.chmod(Gmail.cache.parent, 0o700)
        if Gmail.cache.exists():
            with Gmail.cache.open("rb") as handle:
                auth = cast(Credentials, pickle.load(handle))
        if auth is not None and not auth.expired:
            return auth
        elif auth is not None and auth.expired and auth.refresh_token:
            request: AuthTransportRequest = AuthTransportRequest()  # type: ignore[no-untyped-call]
            auth.refresh(request)
            return auth
        elif credentials is not None and credentials.exists():
            flow = InstalledAppFlow.from_client_secrets_file(
                credentials, ["https://www.googleapis.com/auth/gmail.readonly"]
            )
            auth = cast(Credentials | None, flow.run_local_server(port=0))
            if auth is None:
                raise ValueError("Was not able to generate")
            with Gmail.cache.open("wb") as handle:
                pickle.dump(auth, handle)
            os.chmod(Gmail.cache, 0o600)
            return auth
        else:
            raise ValueError("You must provide a valid credentials file.")

    @staticmethod
    def format_query(*args: str | dict[str, str], **kwargs: dict[str, str]) -> str:
        """Construct a Gmail query from key-value pairs."""
        words: list[str] = []
        pairs: dict[str, str] = {}

        for arg in args:
            if isinstance(arg, str):
                words.append(arg)
            elif isinstance(arg, dict):
                pairs.update(arg)

        terms = words + [f"{key}:{value}" for key, value in {**pairs, **kwargs}.items()]
        return " ".join(terms)

    def find(
        self,
        query: str | None = None,
        user_id: str | None = USER_ID,
        include_spam_trash: bool = True,
        batch_by_n: int | None = None,
    ) -> Iterator[dict[str, str]]:
        """Find all emails by query. Paginate all items lazily."""
        response: dict[str, str | list[dict[str, str]] | None] = {"nextPageToken": None}
        while "nextPageToken" in response:
            logger.info(f"Searching Gmail messages with: {query}")
            response = (
                self.client.users()
                .messages()
                .list(
                    userId=user_id,
                    q=query,
                    pageToken=response.get("nextPageToken"),
                    includeSpamTrash=include_spam_trash,
                    maxResults=batch_by_n,
                )
                .execute()
            )
            messages = cast(list[dict[str, str]], response.get("messages"))
            if messages is not None:
                for message in messages:
                    yield message

    def find_first(
        self,
        query: str | None = None,
        user_id: str | None = USER_ID,
        include_spam_trash: bool = True,
    ) -> dict[str, str] | None:
        """An optimized find for returning the first email that matches the query."""
        return next(self.find(query, user_id, include_spam_trash, batch_by_n=1), None)

    def wait_for_message(
        self, query: str, wait_for: int = 200, wait_between: int = 5
    ) -> dict[str, str] | None:
        """Continually search for a message until `wait_for` seconds has elapsed."""
        wait_start = datetime.now()
        message_meta: dict[str, str] | None = None
        while message_meta is None and datetime.now() < wait_start + timedelta(seconds=wait_for):
            message_meta = self.find_first(query)
            if message_meta is None:
                time.sleep(wait_between)
        return message_meta

    def fetch_message(self, message_id: str, user_id: str = USER_ID) -> Message:
        """Fetch a message from Gmail for the given user."""
        response = self.client.users().messages().get(userId=user_id, id=message_id).execute()
        snippet = unescape(response["snippet"])
        payload = response["payload"]
        return Message(snippet, parts=list(Gmail.build_parts(payload)))

    def data_from_attachment(
        self, message_id: str, attachment: Attachment, user_id: str = USER_ID
    ) -> str:
        """Fetch an attachment on a message from Gmail for the given user."""
        data: str = cast(
            str,
            attachment.body.get(
                "data",
                self.client.users()
                .messages()
                .attachments()
                .get(userId=user_id, messageId=message_id, id=attachment.attachment_id)
                .execute()
                .get("data"),
            ),
        )
        return urlsafe_b64decode(data).decode()


def unpack_seshat_attachment(
    infile: Path,
    output: Path,
    attachment: str,
) -> None:
    """
    Unpack Seshat annotations for a VCF file into a directory structure.

    Args:
        infile: The path to the input VCF file.
        output: The output path prefix for writing the annotations files.
        attachment: The attachment in a single string.

    """
    infile_canonical = strip_gzipped_extension(infile)
    archive = Path(str(infile_canonical) + ".seshat.zip")

    os.makedirs(output.parent, exist_ok=True)

    logger.info(f"Writing attachment to ZIP archive: {archive}")
    with archive.open("wb") as handle:
        handle.write(attachment.encode())
    logger.info(f"Extracting ZIP archive: {archive}")
    with ZipFile(archive, "r") as handle:
        handle.extractall(output.parent)
        for annotation_tsv in map(Path, handle.namelist()):
            full_annotation_tsv = output.parent / annotation_tsv
            assert full_annotation_tsv.is_file()
            renamed = full_annotation_tsv.with_name(
                output.name + ".seshat." + full_annotation_tsv.name
            )
            full_annotation_tsv.rename(renamed)
            logger.info(f"Output file renamed to: {renamed}")


def find_in_gmail(
    infile: Path,
    output: Path,
    newer_than: int = 5,
    wait_for: int = 200,
    credentials: Path | None = None,
) -> None:
    """Wait for an email for Seshat given an annotated VCF file, then download the annotations."""
    gmail = (
        Gmail(Gmail.get_login_credentials())
        if credentials is None
        else Gmail.from_auth_json(credentials)
    )

    infile_canonical = strip_gzipped_extension(infile)

    logger.info("Successfully logged into the Gmail service.")
    logger.info(f"Querying for a VCF named: {infile_canonical.name}")

    query = Gmail.format_query(
        infile_canonical.name,
        {"from": FROM},
        {"newer_than": f"{newer_than}h"},
        {"subject": '"Results of batch analysis"'},
    )

    message_meta = gmail.wait_for_message(query, wait_for=wait_for)

    if message_meta is None:
        raise SeshatError(
            "Could not find an email from a successful Seshat annotation  for the given input VCF."
        )

    logger.info(f"Message found with the following metadata: {message_meta}")
    message = gmail.fetch_message(message_meta["id"])
    logger.info("Message contents are as follows:")

    for line in next(iter(message.html_parts())).all_text():
        logger.info(f"  >>> {line}")

    attachments = message.attachments()

    if len(attachments) == 0:
        raise SeshatError(
            "Email found without attachment indicating Seshat may have failed annotation."
        )
    else:
        attachment = gmail.data_from_attachment(
            message_id=message_meta["id"], attachment=next(iter(attachments))
        )
        unpack_seshat_attachment(infile, output, attachment)
