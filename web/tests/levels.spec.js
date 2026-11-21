import { expect, test } from "@playwright/test";

test("clearing First Flight unlocks Paper Maze across reloads", async ({
  page,
}) => {
  test.slow();
  await page.clock.install({ time: new Date("2026-01-01T00:00:00Z") });
  await page.clock.pauseAt(new Date("2026-01-01T00:01:00Z"));
  await page.goto("/");

  const levels = page.getByRole("button", { name: "Levels", exact: true });
  await expect(levels).toBeEnabled();
  await levels.press("Enter");
  await expect(page.getByRole("button", { name: /Paper Maze/ })).toBeDisabled();
  await expect(page.getByRole("button", { name: /Crosswinds/ })).toBeDisabled();
  await page.getByRole("button", { name: "First Flight" }).press("Enter");
  await page.clock.runFor(1600);
  await expect(page.getByRole("heading", { name: "Sky closed" })).toBeVisible();
  await expect(page.locator("#details")).toHaveText("First Flight");
  await page.getByRole("button", { name: "Fly again" }).press("Enter");

  // Each row gives the input times for one wall. Hold on the first input to cut
  // the opening, then tap to stay in the gap.
  const inputs = [
    [717, 1283, 1917, 2567, 3200],
    [3933, 4500, 5133, 5783, 6417, 7000],
    [7667, 8233, 8883, 9517, 10167, 10817],
    [11517, 12083, 12733, 13367, 14017, 14633],
    [15283, 15850, 16483, 17133, 17767, 18450],
    [19133, 19700, 20333, 20983, 21617],
  ];
  let elapsed = 0;
  for (const [holdAt, ...flapTimes] of inputs) {
    await page.clock.runFor(holdAt - elapsed);
    await page.keyboard.down("Space");
    await page.clock.runFor(200);
    await page.keyboard.up("Space");
    elapsed = holdAt + 200;

    for (const flapAt of flapTimes) {
      await page.clock.runFor(flapAt - elapsed);
      await page.keyboard.press("Space");
      elapsed = flapAt;
    }
  }
  await page.clock.runFor(700);
  await expect(
    page.getByRole("heading", { name: "Level clear" }),
  ).toBeVisible();
  await expect(page.locator("#score")).toHaveText("6");
  await page.getByRole("button", { name: "Next level" }).press("Enter");
  await page.clock.runFor(1600);
  await expect(page.getByRole("heading", { name: "Sky closed" })).toBeVisible();
  await expect(page.locator("#details")).toHaveText("Paper Maze");

  await page.getByRole("button", { name: "Main menu" }).press("Enter");
  await expect(levels).toBeFocused();
  await expect(page.locator("#details")).toHaveText("Endless best 0");
  await page.reload();
  await expect(levels).toBeEnabled();
  await levels.press("Enter");
  await expect(page.getByRole("button", { name: "Paper Maze" })).toBeEnabled();
  await expect(page.getByRole("button", { name: /Crosswinds/ })).toBeDisabled();
});
