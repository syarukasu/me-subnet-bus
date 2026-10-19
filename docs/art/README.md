# Subnet plate artwork

Source: `subnet-atlas-source.png`, generated with the built-in image generation tool
on 2026-10-19. The user's concept sheet defines the color/shape direction; it was not
cropped or pasted into game textures.

Generation prompt: a square 2x2 atlas without margins, captions or perspective;
flat 16x16 Minecraft pixel-art tiles with white/light-gray beveled frames and dark
inner seams. Top-left green recessed square ring, top-right cyan recessed square
ring, bottom-left lavender panel with ivory ring, bottom-right charcoal metal edge
with white/gray rails. Crisp limited palette, no antialiasing, diffuse texture maps
rather than a presentation sheet.

`tools/Build-Models.ps1` extracts and nearest-neighbor samples the four quadrants
to 16x16 PNG files. White arrows are model geometry using a small ivory area of
the power texture. The original atlas is not packaged in the mod.
