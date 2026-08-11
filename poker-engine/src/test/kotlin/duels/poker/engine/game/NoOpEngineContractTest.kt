package duels.poker.engine.game

/** Proves the suite runs and inherits its tests: [NoOpEngine] rejects every case. */
internal class NoOpEngineContractTest : PokerEngineContract() {
    override fun engine(): PokerEngine = NoOpEngine
}
