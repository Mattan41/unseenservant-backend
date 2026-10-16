# Git Safety

Commits belong to the owner. Work on the working tree; never on history.

- Never run `git commit`, `git push`, `git merge`, `git rebase`, `git cherry-pick`,
  `git commit --amend`, `git reset --hard`, or anything else that writes history or
  goes near a remote. Do not create, move or delete branches or tags.
- Discarding work is also off limits: no `git checkout -- <path>`, `git restore`,
  `git clean`. If a change has to be backed out, do it by editing the file, and only
  when the owner has asked for that specific change to be backed out.
- Reading is fine and expected: `git status`, `git diff`, `git log`, `git show`,
  `git blame`, `git stash list`. `git add` is allowed but only when explicitly asked
  for — staging is not committing, but an unrequested add still changes what the next
  `git commit` would pick up.
- Never let a secret or a build artifact reach the index: `.env`, `.env.local`,
  `*.code-workspace`, tokens, webhook URLs, client secrets, `node_modules/`, `dist/`,
  `target/`, `.nuxt/`. Check `.gitignore` before adding anything that carries
  credentials; if such a file is already tracked, report it rather than "fixing" it
  silently.
- When a task ends with a dirty tree, say plainly what is dirty and what the next step
  is (normally: review, then commit) — and include a suggested commit message with the
  edit report so the owner can commit with one paste.
