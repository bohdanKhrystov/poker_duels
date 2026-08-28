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
- **`ADR-0081`'s fragment token — the two calls that receive it do not stand in the same relation
  to it leaving the bar.** The recovery links `ADR-0081` describes put a token in the fragment
  behind `#/reset` and `#/verify`; `Lobby`'s own mount effect (`clearToken`) is what clears it with
  `history.replaceState` (`ADR-0081` §5). For `resetPassword`, fired from a submit handler that can
  only run after mount and every mount effect has already completed, that clear is already done by
  the time it is called. For `verifyEmail` it is not: `VerifyScreen` issues it from its **own**
  mount effect, and React runs a child's mount effects before its parent's — `VerifyScreen` is the
  child of `Lobby` here — so `verifyEmail`'s call precedes `Lobby`'s clear, both in the same commit
  with no paint between them. Measured directly on the merged screens, the order is
  `["verifyEmail", "replaceState"]`, the reverse of `resetPassword`'s. This sweep drives both
  functions directly, as bare calls, with `VERIFY_TOKEN` and `RESET_TOKEN` as plain arguments; its
  `window.location.href` check after each call would catch either function writing to
  `window.location` itself, which neither does today — but it drives no screen and reads no
  `window.location.hash`, so the ordering above, including the moment `verifyEmail`'s call and the
  token's presence in the bar coincide, is invisible to it, exactly as it was before this ticket.
