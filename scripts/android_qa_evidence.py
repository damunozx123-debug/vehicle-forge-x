#!/usr/bin/env python3
"""Redact Vehicle Forge Android QA evidence before writing it to disk.

Reads from stdin; outputs only redacted text. No file, ADB or network access.
Never treat this as a guarantee that arbitrary logs contain no personal data.
"""
from __future__ import annotations

import re
import sys

VIN = re.compile(r"\b[A-HJ-NPR-Z0-9]{17}\b", re.I)
MAC = re.compile(r"\b(?:[0-9A-F]{2}[:-]){5}[0-9A-F]{2}\b", re.I)
EMAIL = re.compile(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", re.I)
IPV4 = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
AUTH = re.compile(r"(?i)\b(authorization)\s*[:=]\s*(?:bearer\s+)?[^\s,;]+")
SECRETS = re.compile(r"(?i)\b(api[_-]?key|access[_-]?token|refresh[_-]?token|password|secret)\s*[:=]\s*[^\s,;]+")
PLATE = re.compile(r"(?i)\b(plate|registration)\s*[:=]\s*[A-Z0-9-]{3,10}")
PHONE_NZ = re.compile(r"(?<!\w)(?:\+64|0064|0)(?:2\d{7,9}|[3-9]\d{6,8})(?!\w)")


def redact(value: str) -> str:
    value = VIN.sub("[VIN REDACTED]", value)
    value = MAC.sub("[MAC REDACTED]", value)
    value = EMAIL.sub("[EMAIL REDACTED]", value)
    value = IPV4.sub("[IP REDACTED]", value)
    value = AUTH.sub("authorization=[REDACTED]", value)
    value = SECRETS.sub(lambda m: m.group(1) + "=[REDACTED]", value)
    value = PLATE.sub(lambda m: m.group(1) + "=[REDACTED]", value)
    return PHONE_NZ.sub("[PHONE REDACTED]", value)


if __name__ == "__main__":
    sys.stdout.write(redact(sys.stdin.read()))
