# What `no-secret-in-a-url.test.ts` does not see

The sweep drives `signUp`, `signIn`, `attachRecoveryEmail`, `forgotPassword`, `verifyEmail`,
`resetPassword`, `revokeThisDevice` and `signOut` directly, through a recording `fetch` double, and
checks every `path`, every parsed body and `window.location.href` after each of the eight calls,
against seven secrets — the handle, the password, the session token, the recovery address, the two
mailed tokens and the new password a reset sets. That is a real guarantee about these eight
functions as they exist today — and only about them. Three surfaces sit outside what driving eight
functions in one process can ever observe:

- **A future caller that builds its own URL.** The sweep exercises the eight modules under
  `web-client/src/account/` and nothing else. A screen, a retry wrapper, a logger or a new endpoint
  that concatenates a secret into a path itself — rather than going through one of these eight
  functions — is invisible to this test, however it is written. Extending this file's coverage to a
  new caller is that caller's ticket, not a side effect of this one.
- **A real browser's `Referer` header.** jsdom never makes a network request; `fetch` here is a
  recording double, not a socket, and nothing in this test suite renders a page that then navigates
  or loads a resource from another origin. Whatever a real browser would put in a `Referer` header
  under those conditions is not something a jsdom-based test can produce or inspect, so this sweep
  says nothing about it in either direction.
- **`ADR-0081`'s fragment token — reachable once it is in hand, not while it is still in the bar.**
  The recovery links `ADR-0081` describes put a token in the fragment behind `#/reset` and
  `#/verify`; the merged screens read it once and clear it with `history.replaceState`
  (`ADR-0081` §5) before ever calling `verifyEmail` or `resetPassword`. This sweep drives those two
  directly with `VERIFY_TOKEN` and `RESET_TOKEN` as plain arguments — the same shape a screen hands
  them in — so the _value_ a mailed link carries is now in `SECRETS` and checked against
  `window.location.href` after all eight calls: a regression that left either token in the address
  bar once a screen already held it would redden here. The fragment itself — the moment
  `#/reset/<token>` or `#/verify/<token>` sits in the bar while a screen mounts, `ADR-0081`'s one
  sanctioned exception to "no secret in the address bar" — is not: this sweep drives no screen and
  reads no `window.location.hash`, so that moment is invisible to it exactly as it was before this
  ticket.
