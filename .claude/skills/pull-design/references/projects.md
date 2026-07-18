# Bro — Claude Design project IDs

Two projects on claude.ai/design, read with the **DesignSync** tool
(`list_files` to discover current paths, then `get_file`). Read-only — never
push. Do not hardcode file paths; always `list_files` first.

## Bro Design System — tokens, guidelines, components

- **projectId:** `c5a6030b-52d3-4ecc-ab51-4460eebdc7df`
- Type: design system. Appears in DesignSync `list_projects`.
- Canonical source for design tokens and component contracts.

## Bro designs — app screen mockups

- **projectId:** `f6b3ad66-7433-4a29-92da-213733550154`
- URL: https://claude.ai/design/p/f6b3ad66-7433-4a29-92da-213733550154
- Type: **design project, NOT a design system** — so it does *not* appear in
  DesignSync `list_projects`. Address it by ID directly.
- Holds the app screen mockups.
