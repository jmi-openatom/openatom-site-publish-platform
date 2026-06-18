# Prototype Instructions

Run the local server yourself and open the preview in the in-app browser. Do not give the user server-start instructions when you can run it.

Before making substantial visual changes, use the Product Design plugin's `get-context` skill when the visual source is unclear or no longer matches the current goal. When the user gives durable prototype-specific design feedback, preferences, or decisions, record them in `AGENTS.md`.

When implementing from a selected generated mock, treat that image as the source of truth for layout, component anatomy, density, spacing, color, typography, visible content, and hierarchy.

## Locked visual direction

- The selected source of truth is `../design-reference/option-2.png`.
- Preserve the full-width top navigation, atmospheric raster header wash, two-column project gallery, and right-side deployment activity rail.
- Use Chinese UI copy, restrained hairline borders, white/near-white surfaces, Phosphor icons, and the real project preview crops in `public/previews/`.
- All visible controls should remain functional against the Spring Boot API; development login is only a local fallback for OAuth-independent validation.
