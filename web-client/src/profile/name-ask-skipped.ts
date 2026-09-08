/**
 * The one key this browser uses to record that the player skipped the name ask.
 *
 * Per `ADR-0119` §5, the skipped bit is a fact about this browser and does not travel.
 * Exported because the server column is intentionally absent.
 */
export const NAME_ASK_SKIPPED_STORAGE_KEY = "pd.nameAskSkipped";

const SENTINEL = "1";

/** Whether this browser has recorded that the player skipped the name ask. */
export function readNameAskSkipped(storage: Storage): boolean {
  const value = storage.getItem(NAME_ASK_SKIPPED_STORAGE_KEY);
  return value?.trim() === SENTINEL;
}

/** Record that the player skipped the name ask on this browser. */
export function markNameAskSkipped(storage: Storage): void {
  storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, SENTINEL);
}
