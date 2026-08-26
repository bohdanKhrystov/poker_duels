import {
  createContext,
  useContext,
  type ReactElement,
  type ReactNode,
} from "react";
import type { SignUpOutcome } from "./sign-up";
import type { SignInOutcome } from "./sign-in";
import type { SignOutOutcome } from "./sign-out";
import type { RevokeOutcome } from "./revoke-device";

export interface AccountCalls {
  readonly signUp: (handle: string, password: string) => Promise<SignUpOutcome>;
  readonly signIn: (handle: string, password: string) => Promise<SignInOutcome>;
  readonly signOut: () => Promise<SignOutOutcome>;
  readonly revokeThisDevice: () => Promise<RevokeOutcome>;
}

const AccountContext = createContext<AccountCalls | null>(null);

/**
 * Makes the account calls available to components below in the tree.
 *
 * The calls object must be a stable reference (a module-scope constant, not an inline
 * object) — this provider hands it down as-is, and a reference that changes
 * on every render would appear to change to consumers as well.
 */
export function AccountProvider(props: {
  calls: AccountCalls;
  children: ReactNode;
}): ReactElement {
  const { calls, children } = props;

  return (
    <AccountContext.Provider value={calls}>{children}</AccountContext.Provider>
  );
}

/** The account calls, or `null` where no provider is above. */
export function useAccount(): AccountCalls | null {
  return useContext(AccountContext);
}
