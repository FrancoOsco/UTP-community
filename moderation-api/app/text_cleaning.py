import re

_MENTION = re.compile(r"@(user|usuario)\b", re.IGNORECASE)
_URL = re.compile(r"https?://\S+|\bn?LINK\b", re.IGNORECASE)   # \bn?LINK\b cubre el "nlink" residual
_MULTI_Q = re.compile(r"\?{2,}")
_LEADING_Q = re.compile(r"^\s*\?+\s*")
_SPACES = re.compile(r"\s+")
_LEADING_PUNCT = re.compile(r"^[\s,.;:]+")


def clean_text(text: str) -> str:
    s = str(text).replace("\ufffd", "")
    s = _MENTION.sub(" ", s)
    s = _URL.sub(" ", s)
    s = _MULTI_Q.sub(" ", s)
    s = _LEADING_Q.sub("", s)
    s = _SPACES.sub(" ", s)
    s = _LEADING_PUNCT.sub("", s)
    return s.strip()