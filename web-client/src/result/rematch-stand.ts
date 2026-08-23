export interface RematchStand {
  readonly mine: boolean;
  readonly theirs: boolean;
}

export function rematchStand(
  offers: readonly number[],
  mySeat: number | null,
): RematchStand {
  // A client that never received RoomJoined holds no seat, cannot tell whose an offer is,
  // and has no room to offer one in — ADR-0044 §1 puts the room on the socket, not on the frame.
  if (mySeat === null) {
    return {
      mine: false,
      theirs: false,
    };
  }

  return {
    mine: offers.includes(mySeat),
    theirs: offers.some((seat) => seat !== mySeat),
  };
}
