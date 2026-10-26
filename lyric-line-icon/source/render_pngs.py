"""Render legacy launcher PNGs and the Play Store icon from the Lyric Line design.

Usage: python3 render_pngs.py <output-root>
Requires: pip install cairosvg
"""
import os
import sys

import cairosvg

GLYPH = """
  <rect x="30" y="34" width="40" height="6" rx="3" fill="#C9C3B6"/>
  <rect x="24" y="50" width="60" height="9" rx="4.5" fill="#111418"/>
  <rect x="24" y="50" width="36" height="9" rx="4.5" fill="#FF6B5B"/>
  <rect x="32" y="69" width="34" height="6" rx="3" fill="#C9C3B6"/>
"""
BG = "#F4F1EA"


def svg(view_box, shape):
    x, y, w, h = view_box
    if shape == "square":  # legacy rounded-square launcher icon
        r = w * 0.18
        clip = f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}"/>'
    elif shape == "circle":
        clip = f'<circle cx="{x + w / 2}" cy="{y + h / 2}" r="{w / 2}"/>'
    else:  # full bleed (Play Store applies its own mask)
        clip = f'<rect x="{x}" y="{y}" width="{w}" height="{h}"/>'
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x} {y} {w} {h}">'
        f'<defs><clipPath id="m">{clip}</clipPath></defs>'
        f'<g clip-path="url(#m)"><rect x="0" y="0" width="108" height="108" fill="{BG}"/>{GLYPH}</g>'
        "</svg>"
    )


def render(markup, size, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    cairosvg.svg2png(bytestring=markup.encode(), write_to=path, output_width=size, output_height=size)


def main(out):
    # Legacy icons show the adaptive icon's visible 72dp area.
    legacy_view = (18, 18, 72, 72)
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, px in densities.items():
        render(svg(legacy_view, "square"), px, f"{out}/res/mipmap-{name}/ic_launcher.png")
        render(svg(legacy_view, "circle"), px, f"{out}/res/mipmap-{name}/ic_launcher_round.png")

    # Play Store listing icon: 512x512, full square, a little extra margin around the glyph.
    render(svg((12, 12, 84, 84), "full"), 512, f"{out}/playstore/ic_launcher-playstore.png")


if __name__ == "__main__":
    main(sys.argv[1])
