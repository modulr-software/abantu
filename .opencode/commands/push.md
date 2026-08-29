---
description: Push current branch to upstream (create upstream if missing)
agent: build
---
!`git branch --show-current`
!`git rev-parse --abbrev-ref --symbolic-full-name @{u} 2>/dev/null || echo "NO_UPSTREAM"`

Push the current branch to its upstream. If the output above shows "NO_UPSTREAM", create the upstream with `git push -u origin <branch-name>` where `<branch-name>` is the branch from the first command. If an upstream is already set, run `git push`. Show the full output. If the push is rejected (non-fast-forward, hook failure, auth error), stop and report the raw error to the user — do not attempt `--force`, do not rebase, do not retry.
