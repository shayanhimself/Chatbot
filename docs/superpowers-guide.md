# Using Superpowers — Spec-Driven Development Workflow

**What:** [Superpowers](https://github.com/obra/superpowers) is a software-development methodology for coding agents, delivered as a Claude Code plugin — a set of composable skills plus a session-start hook that makes the agent check for and follow them automatically. Built by Jesse Vincent / Prime Radiant. This project uses it as the backbone of our spec-driven process (see `CLAUDE.md`).

---

## 1. Installation

Superpowers is in Anthropic's official plugin marketplace (auto-registered in Claude Code):

```
/plugin install superpowers@claude-plugins-official
```

Alternative (author's own marketplace):

```
/plugin marketplace add obra/superpowers-marketplace
/plugin install superpowers@superpowers-marketplace
```

### Choose the install scope

The install prompt asks for a scope:

| Scope | Stored in | Effect |
|---|---|---|
| User (default) | `~/.claude` | Active in **every** session, all projects |
| **Project** ← recommended here | `.claude/settings.json` (committed) | This repo only, shared with collaborators |
| Local | this repo, personal | This repo only, just you |

Non-interactive: `claude plugin install superpowers@claude-plugins-official --scope project`

We use **project scope**: superpowers is opinionated (hard-gates design before code, strict TDD) — right for this project, intrusive for quick hacks elsewhere. Project scope also ships the workflow with the repo. Note: collaborators get prompted once to run the install command before the plugin loads for them.

After install/enable/disable: `/reload-plugins`.

### Prerequisites

- **git repo** — the worktree skill requires git (`git init` if not done)
- A runnable test baseline once the project is scaffolded (worktree setup verifies clean tests)

---

## 2. How it works: automatic, not command-driven

You don't invoke superpowers explicitly. The plugin's session-start hook injects a `using-superpowers` bootstrap into each session; from then on the agent checks for a relevant skill before any task. The README calls the skills "mandatory workflows, not suggestions."

Manual invocation is still possible — skills are namespaced slash commands: `/superpowers:brainstorming`, `/superpowers:writing-plans`, etc. Type `/` to browse.

You can also steer in plain language: *"skip brainstorming — the spec already exists in specs/001, go straight to planning."*

---

## 3. The workflow, stage by stage

### Stage 1 — Brainstorming (`brainstorming`)

Triggers the moment you describe something to build. The agent does **not** write code; it asks clarifying questions **one at a time**, explores alternatives, then presents the design in digestible sections for approval.

Contains a HARD-GATE:

> Do NOT invoke any implementation skill, write any code, scaffold any project, or take any implementation action until you have presented a design and the user has approved it.

Applies to *every* task, including "simple" ones — the skill explicitly rejects "this is too simple to need a design."

Output: a design document. **In this project, tell the agent to save designs into `specs/`** (user preference overrides the skill default) — our specs are the source of truth per `CLAUDE.md`.

### Stage 2 — Isolated workspace (`using-git-worktrees`)

After design approval: creates a git worktree on a new branch, runs project setup, verifies a clean test baseline before any work starts.

### Stage 3 — Planning (`writing-plans`)

Breaks the approved design into bite-sized tasks (2–5 minutes each) with exact file paths, complete code, and verification steps. Written to be executable by "an enthusiastic junior engineer with zero context and questionable taste" — i.e., precise enough for a fresh subagent. Emphasizes TDD, YAGNI, DRY.

Plans saved to: `docs/superpowers/plans/YYYY-MM-DD-<feature-name>.md`

Division of labor with our specs: **spec = what/why** (lives in `specs/`), **plan = how** (lives in `docs/superpowers/plans/`).

### Stage 4 — Implementation (you say "go")

Two modes:

- **`subagent-driven-development`** — fresh subagent per task; each task gets a two-stage review (spec compliance first, then code quality). Can run autonomously for hours without drifting from the plan.
- **`executing-plans`** — batch execution with human checkpoints between batches. Pick this when you want tighter control.

### Stage 5 — TDD enforcement (`test-driven-development`)

Active throughout implementation. Strict RED → GREEN → REFACTOR:

1. **RED** — write a failing test, watch it fail
2. **GREEN** — write minimal code, watch it pass
3. **REFACTOR** — clean up, commit

**The delete rule:** code written before its test gets deleted and re-implemented test-first. No keeping it "as reference," no adapting it. Rationale: a test written after code passes immediately and proves nothing; the fail→pass transition is the evidence. Scope limits: applies to new code in the current task only — existing untested code gets tests added, not deleted; exploratory spikes are fine but are thrown away before real TDD implementation starts.

### Stage 6 — Code review (`requesting-code-review` / `receiving-code-review`)

Runs between tasks. Reviews work against the plan, reports issues by severity. Critical issues block progress.

### Stage 7 — Finishing (`finishing-a-development-branch`)

When all tasks complete: verifies tests, presents options — merge / open PR / keep working / discard — and cleans up the worktree.

---

## 4. Other skills in the box

| Skill | When it fires |
|---|---|
| `systematic-debugging` | Bug work — 4-phase root-cause process (includes root-cause tracing, defense-in-depth, condition-based waiting) |
| `verification-before-completion` | Before declaring anything done — evidence over claims |
| `dispatching-parallel-agents` | Concurrent subagent workflows |
| `writing-skills` | Creating new skills (use this to grow our own project skills, e.g. a future chat-engine domain skill) |
| `using-superpowers` | The bootstrap/introduction skill |

---

## 5. Using it in this project — quick recipes

**New feature from scratch:**
> "I want to build the chat engine (spec 001)."
→ brainstorming fires → answer its questions → approve design → it plans → say "go".

**Spec already written:**
> "Spec is final in `specs/001-chat-engine.md`. Skip brainstorming, write the implementation plan."

**Quick fix without full ceremony:**
> "Trivial one-line fix, skip the workflow: …"
(Expect pushback from the brainstorming hard-gate; be explicit.)

**Bug hunt:**
> "Chat stream drops first token on Nano engine" → `systematic-debugging` fires automatically.

**Tighter control during implementation:** ask for `executing-plans` (checkpointed batches) instead of subagent-driven development.

---

## 6. Caveats

- **Question-at-a-time brainstorming feels slow** the first time. That interrogation *is* the spec-driven part — worth it.
- **Context cost:** hook + skills add tokens every turn in scope. `/plugin` → plugin details shows the estimate. (Reason we keep it project-scoped.)
- **Telemetry:** brainstorming's optional visual companion loads a Prime Radiant logo from their site (carries only the superpowers version — no project/prompt data). Disable with env var `SUPERPOWERS_DISABLE_TELEMETRY=1`; it also honors `DISABLE_TELEMETRY` and `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC`.
- **Updates:** official-marketplace plugins auto-update by default; `/plugin` → Marketplaces tab to toggle.

---

## References

- Repo: https://github.com/obra/superpowers
- Marketplace: https://github.com/obra/superpowers-marketplace
- Release announcement: https://blog.fsck.com/2025/10/09/superpowers/
- Discord: https://discord.gg/35wsABTejz
- Our process rules: `CLAUDE.md` · Product brief: `specs/000-product-brief.md`
