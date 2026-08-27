/**
 * The one key this browser's offer-settled flag is stored under.
 *
 * `ADR-0086` §1 and §2 settle that this module owns it alongside the predicate it feeds.
 * The key appears here and nowhere else in production source.
 */
export const ACCOUNT_OFFER_SETTLED_STORAGE_KEY = "pd.accountOfferSettled";

/** The sentinel a settled offer stores. It carries no information — `ADR-0086` §3. */
const SETTLED = "1";

/**
 * Read whether this browser has settled the account offer.
 *
 * Returns `true` iff the stored value, trimmed, is exactly the sentinel. Absent, blank, and
 * every other value read as `false`, so the player is re-offered after their next win if this
 * browser cannot read the answer — the failure direction `ADR-0085` §Consequences chose. A
 * `Storage` that refuses the write loses the answer the same way.
 */
export function readOfferSettled(storage: Storage): boolean {
  return storage.getItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY)?.trim() === SETTLED;
}

/**
 * Mark the account offer as settled by this browser.
 *
 * Stores the sentinel value. `ADR-0086` §4 provides no way to clear it — an un-dismiss would have
 * to add an export, which is a diff a reviewer sees rather than a hidden assumption.
 */
export function markOfferSettled(storage: Storage): void {
  storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, SETTLED);
}
