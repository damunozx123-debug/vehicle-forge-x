#!/usr/bin/env python3
"""Generate truthful static store graphics from the checked-in SVG brand mark.

Requirements: CairoSVG==2.8.2. This never manufactures app screenshots.
"""
from pathlib import Path
from xml.sax.saxutils import escape
import base64
import io
import struct

import cairosvg

ROOT = Path(__file__).resolve().parents[1]
META = ROOT / "fastlane/metadata/android/en-US"
OUT = META / "images"
LOGO = ROOT / "assets/store/brand-mark.svg"
COLORS = {
    "Vehicle Forge": "#20DFC5",
    "PatternX Atlas": "#B99CFF",
    "GPU WattLab": "#FFC24D",
    "NetIntel": "#4EC9FF",
}

def check_png(blob: bytes, expected_size: tuple[int, int]) -> None:
    if blob[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", blob[16:24]) != expected_size:
        raise ValueError("Generated PNG has unexpected format or dimensions")

def main() -> None:
    title = (META / "title.txt").read_text(encoding="utf-8").strip()
    subtitle = (META / "short_description.txt").read_text(encoding="utf-8").strip()
    if title not in COLORS:
        raise SystemExit(f"Unknown app title: {title}")
    if not LOGO.is_file():
        raise SystemExit("Missing assets/store/brand-mark.svg")
    accent = COLORS[title]
    OUT.mkdir(parents=True, exist_ok=True)
    icon = cairosvg.svg2png(url=str(LOGO), output_width=512, output_height=512)
    check_png(icon, (512, 512))
    (OUT / "icon.png").write_bytes(icon)
    encoded = base64.b64encode(icon).decode("ascii")
    size = 62 if len(title) < 18 else 48
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 500" width="1024" height="500">
    <defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop stop-color="#081320"/><stop offset="1" stop-color="#172C40"/>
    </linearGradient></defs>
    <rect width="1024" height="500" fill="url(#bg)"/>
    <g stroke="{accent}" opacity=".16" stroke-width="2" fill="none">
      <path d="M0 70H1024 M0 430H1024 M40 0V500 M970 0V500"/>
      <circle cx="190" cy="250" r="174"/><circle cx="190" cy="250" r="209"/>
      <path d="M570 0L1024 454 M730 0L1024 294"/>
    </g>
    <image href="data:image/png;base64,{encoded}" x="68" y="116" width="270" height="270"/>
    <text x="377" y="220" fill="#F3F9FF" font-family="DejaVu Sans,Arial,sans-serif"
      font-size="{size}" font-weight="bold">{escape(title.upper())}</text>
    <text x="378" y="277" fill="#C1D1E3" font-family="DejaVu Sans,Arial,sans-serif"
      font-size="23">{escape(subtitle)}</text>
    <rect x="377" y="319" width="275" height="40" rx="9" fill="none" stroke="{accent}" stroke-width="2"/>
    <text x="514" y="346" text-anchor="middle" fill="{accent}" font-size="18"
      font-family="DejaVu Sans,Arial,sans-serif" font-weight="bold">OPEN SOURCE · ANDROID</text>
    </svg>"""
    feature = cairosvg.svg2png(bytestring=svg.encode("utf-8"))
    check_png(feature, (1024, 500))
    (OUT / "featureGraphic.png").write_bytes(feature)
    print(f"Created {title}: 512x512 icon, 1024x500 feature graphic (not app screenshots)")

if __name__ == "__main__":
    main()
