# Read-Only Boundary: the projects-notes Repository

`github.com/Mattan41/projects-notes` (locally `~/IdeaProjects/projects-notes`) is the
owner's own notes and infrastructure repository — the place production's compose
files, `nginx.conf`, the host `.env`, the runbooks and the backup scripts actually
live. It exists so the owner can write things down while working things out: it is
not a deliverable, and it may be mid-reorganisation at any moment.

The canonical, longer form of this rule is
`~/Documents/Cline/Rules/projects-notes-readonly.md`. Keep the two in step.

## Reading is allowed — writing never is

- Read it freely for context. It is often the only place a host-side fact exists.
- Never create, edit, move, rename or delete anything there, and touch nothing under
  `.git` (no `git add`, no `git mv`, no commits) — not even for a typo, a stray empty
  file, or a file that the current task itself just created.
- Do not translate, "fix" or restructure what you read there. Its Swedish file names,
  comments and documents are deliberate; the Swedish-to-English rule used elsewhere
  does not apply to that repository.

## Put the change in the edit report instead

Anything that repository needs is reported at the end of the turn as concrete,
copy-pasteable content for the owner to apply:

- `docker-compose` changes: services, environment, ports, tags, file name, and where
  it runs from
- `ALLOWED_ORIGINS` / `FRONTEND_URL` values, per host
- OAuth redirect URIs that have to be registered (Google Cloud console, GitHub OAuth
  app) and which hostname each one must contain
- Cloudflare setup: tunnel public hostnames, the origin each one points at, Access
  policies and service tokens, DNS
- the host-side commands and the verification steps that prove the change worked

Name the file a snippet replaces and why it has to change. The owner owns the write.
