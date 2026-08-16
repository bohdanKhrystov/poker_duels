/** The ceiling on the first retry, doubling from there. */
export const FIRST_RETRY_MILLIS = 500;

/** The ceiling no retry ever exceeds, however long the server stays away. */
export const LONGEST_RETRY_MILLIS = 10_000;

/**
 * How long to wait before reconnect attempt number [attempt] (0 for the first
 * retry after a socket closes).
 *
 * Equal jitter: half the ceiling, plus [jitter] of the other half. Half a
 * window of spread is enough to keep two tabs that dropped together from
 * retrying in lockstep, and keeping the lower half fixed means the first
 * retry is still fast.
 *
 * @param attempt How many retries have already been made, 0-based.
 * @param jitter A number in [0, 1). The caller owns the source, so a test
 *   hands it a value rather than a distribution.
 */
export function retryDelayMillis(attempt: number, jitter: number): number {
  const ceiling = Math.min(
    FIRST_RETRY_MILLIS * 2 ** attempt,
    LONGEST_RETRY_MILLIS,
  );
  return Math.floor(ceiling / 2 + jitter * (ceiling / 2));
}
