# Conventional Commits — Agent Spec

This file defines the commit convention for this repository. **All commits MUST follow this spec.**

## Format

```
<type>[optional scope][!]: <description>

[optional body]

[optional footer(s)]
```

## Rules

| Element | Rule |
|---|---|
| **type** | REQUIRED. Noun like `feat`, `fix`, etc. Followed by optional scope, optional `!`, then `:` and space. Case-insensitive (except BREAKING CHANGE). |
| **scope** | OPTIONAL. Noun in parens, e.g. `fix(parser):` |
| `!` | Breaking change indicator. Placed before `:`. Makes `BREAKING CHANGE:` footer optional. |
| **description** | REQUIRED after `: `. Short summary. |
| **body** | OPTIONAL. After blank line. Free-form paragraphs. |
| **footer** | OPTIONAL. After blank line. `Token: value` or `Token #value`. Use `-` not spaces. `BREAKING CHANGE` is exception (spaces allowed). |

## SemVer Mapping

| Commit | Version Bump |
|---|---|
| `fix` | PATCH |
| `feat` | MINOR |
| `BREAKING CHANGE` (`!` or footer) | MAJOR |

## Recognized Types

| Type | Use |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `build` | Build system / deps |
| `chore` | Maintenance / tooling |
| `ci` | CI config |
| `docs` | Documentation |
| `perf` | Performance improvement |
| `refactor` | Code change, no behavior change |
| `style` | Formatting, whitespace |
| `test` | Adding/updating tests |
| `revert` | Reverting a previous commit |

## Breaking Changes — Two Forms

1. `feat(api)!: drop support for Node 6`
2. Footer: `BREAKING CHANGE: environment variables now take precedence`

`BREAKING-CHANGE` is synonymous with `BREAKING CHANGE` in footers.

## Examples

```
feat: add Polish language
fix(parser): handle empty input
feat(api)!: send email on ship
docs: correct spelling of CHANGELOG
revert: let us never again speak of the noodle incident

Refs: 676104e, a215868
```
