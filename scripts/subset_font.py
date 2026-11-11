#!/usr/bin/env python3
"""
FASE 4 (3.10): Font Subsetting Utility for Velocita (< 12KB Font Asset Target).
Strips a monospace TrueType font down to only the exact glyphs required by Velocita UI:
`0 1 2 3 4 5 6 7 8 9 . - k m / h p o t s A C : ±`
"""

import subprocess
import sys
from pathlib import Path

ALLOWED_CHARS = "0123456789.-km/hpotsAC:± "

def subset_font(input_ttf: str, output_ttf: str) -> None:
    unicodes = ",".join(f"U+{ord(c):04X}" for c in sorted(set(ALLOWED_CHARS)))
    cmd = [
        "pyftsubset",
        input_ttf,
        f"--unicodes={unicodes}",
        "--layout-features=tnum",
        "--no-hinting",
        "--desubroutinize",
        f"--output-file={output_ttf}",
    ]
    print("Running:", " ".join(cmd))
    subprocess.run(cmd, check=True)
    size_kb = Path(output_ttf).stat().st_size / 1024.0
    print(f"Subset font written to {output_ttf} ({size_kb:.2f} KB)")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 scripts/subset_font.py <input.ttf> <output.ttf>")
        sys.exit(1)
    subset_font(sys.argv[1], sys.argv[2])
