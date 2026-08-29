---
description: Discard unstaged changes (all, or on a specified path)
agent: build
---
!`git status --porcelain`

The user wants to discard unstaged changes. If `$ARGUMENTS` is empty, this means ALL unstaged changes. If `$ARGUMENTS` is a path, this means only changes on that path.

Before discarding anything, list exactly what will be discarded based on the git status output above:
- For modified tracked files: show the file paths that will be reverted with `git checkout --`
- For untracked files: show the file paths that will be removed with `rm`

Ask the user to confirm before proceeding. If the user confirms:
- If discarding all: run `git checkout -- .` for modified tracked files, and `rm <path>` for each untracked file
- If discarding a specific path: run `git checkout -- <path>` for a tracked file, or `rm <path>` if it is untracked

Leave everything else untouched. Show the full output of each command. If the user does not confirm, do nothing.
