-- ADR-0019: the duel table records how many hands a duel lasted, added now while the
-- table is empty. DuelOutcome.handsPlayed exists only at the moment a duel finishes, so a
-- column added after the first real duel cannot be backfilled.
--
-- NOT NULL, no default: every completed duel played some number of hands, and a nullable
-- column would push "did this duel record it?" onto every reader forever. There are no
-- existing rows, so no backfill value is needed.

ALTER TABLE duel ADD COLUMN hands_played INTEGER NOT NULL;
