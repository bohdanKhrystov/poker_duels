# What `no-secret-in-a-url.test.ts` does not see

The sweep drives `signUp`, `signIn`, `signOut` and `revokeThisDevice` directly, through a recording
`fetch` double, and checks every `path`, every parsed body and `window.location.href` after each
call. That is a real guarantee about these four functions as they exist today — and only about
them. Three surfaces sit outside what driving four functions in one process can ever observe:

- **A future caller that builds its own URL.** The sweep exercises the four modules under
  `web-client/src/account/` and nothing else. A screen, a retry wrapper, a logger or a new endpoint
  that concatenates a secret into a path itself — rather than going through one of these four
  functions — is invisible to this test, however it is written. Extending this file's coverage to a
  new caller is that caller's ticket, not a side effect of this one.
- **A real browser's `Referer` header.** jsdom never makes a network request; `fetch` here is a
  recording double, not a socket, and nothing in this test suite renders a page that then navigates
  or loads a resource from another origin. Whatever a real browser would put in a `Referer` header
  under those conditions is not something a jsdom-based test can produce or inspect, so this sweep
  says nothing about it in either direction.
- **`ADR-0081`'s fragment token.** The recovery links `ADR-0081` describes put a token in the
  fragment segment behind `#/reset` and `#/verify` — a deliberate, sanctioned exception to "no
  secret in the address bar" that `STORY-0417` will build and that this ticket explicitly leaves
  alone (`TASK-041224`'s Out of scope: _the recovery links_). This sweep's `window.location.href`
  check would fail the day that fragment exists on a screen this test drives; it does not drive that
  screen, so it cannot fail today for a reason it did not decide. `STORY-0417` owns those screens and
  must not simply widen this sweep to tolerate a token it was built to catch.
