import { describe, expect, it } from "vitest";
import * as askText from "./name-ask-text";

describe("the ask's words", () => {
  it("states every string exactly, character for character, and exports no seventh", () => {
    // Exactly these exports, and no others — an extra or a missing one fails here
    // even if every literal below still matches, because `toBe` cannot see either.
    expect(Object.keys(askText).sort()).toEqual(
      [
        "NAME_ASK_HEADING",
        "NAME_IS_FOR",
        "NAME_KEEPS",
        "NAME_FIELD_LABEL",
        "TAKE_NAME_LABEL",
        "SKIP_AND_PLAY_LABEL",
      ].sort(),
    );

    expect(askText.NAME_ASK_HEADING).toBe("Choose your name");
    expect(askText.NAME_IS_FOR).toBe(
      "This is the name a rival will see across the table, and the name that appears on the ladder.",
    );
    expect(askText.NAME_KEEPS).toBe(
      "You can change it later — but a name you give up is gone for good, for you and for whoever plays after you.",
    );
    expect(askText.NAME_FIELD_LABEL).toBe("Your name");
    expect(askText.TAKE_NAME_LABEL).toBe("Take this name");
    expect(askText.SKIP_AND_PLAY_LABEL).toBe("Duel without a name");
  });
});
