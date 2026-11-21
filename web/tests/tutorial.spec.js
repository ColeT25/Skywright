import { expect, test } from "@playwright/test";

async function openStartMenu(page) {
  await page.clock.install({ time: new Date("2026-01-01T00:00:00Z") });
  await page.clock.pauseAt(new Date("2026-01-01T00:01:00Z"));
  await page.goto("/");
  const tutorial = page.getByRole("button", { name: "Tutorial", exact: true });
  await expect(tutorial).toBeEnabled();
  return tutorial;
}

test("completes the tutorial without adding practice points to the best score", async ({
  page,
}) => {
  const tutorial = await openStartMenu(page);
  await tutorial.press("Enter");
  const cue = page.getByRole("status");
  await expect(cue).toContainText("Flap");
  await expect(page.locator("#sky")).toBeFocused();
  await page.clock.runFor(3000);
  await expect(cue).toContainText("Flap");

  await page.keyboard.press("Space");
  await page.clock.runFor(320);
  await expect(cue).toContainText("Hold to glide");
  await page.keyboard.down("Space");
  await page.clock.runFor(240);
  await expect(cue).toContainText("Release to fire");
  await page.keyboard.up("Space");
  await page.clock.runFor(450);
  await expect(cue).toContainText("Fly through");

  // Read the next instruction while the game waits for input.
  await page.clock.runFor(3000);
  await expect(cue).toContainText("Fly through");
  await page.keyboard.press("Space");
  await page.clock.runFor(1900);
  await expect(cue).toContainText("Try the gap again");
  for (let flap = 0; flap < 4; flap++) {
    await page.keyboard.press("Space");
    await page.clock.runFor(610);
  }
  await expect(cue).toContainText("Tutorial complete");

  const chooseLevel = page.getByRole("button", { name: "Choose a level" });
  await expect(chooseLevel).toBeFocused();
  await chooseLevel.press("Enter");
  await page.getByRole("button", { name: "Back", exact: true }).press("Enter");
  await expect(page.locator("#details")).toContainText("Endless best 0");
  await page
    .getByRole("button", { name: "Endless", exact: true })
    .press("Enter");
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");
  await expect(cue).toBeHidden();
  await page.clock.runFor(1600);
  await expect(page.getByRole("heading", { name: "Sky closed" })).toBeVisible();
  await expect(page.locator("#details")).toContainText("Best 0");
});

test.describe("phone viewport", () => {
  test.use({
    viewport: { width: 412, height: 915 },
    isMobile: true,
    hasTouch: true,
  });

  test("can hold, retry a crash, and exit on touch", async ({ page }) => {
    const tutorial = await openStartMenu(page);
    await tutorial.tap();
    const cue = page.getByRole("status");
    await page.touchscreen.tap(140, 350);
    await page.clock.runFor(320);
    await expect(cue).toContainText("Hold to glide");

    const input = await page.context().newCDPSession(page);
    const point = { x: 140, y: 350 };
    await input.send("Input.dispatchTouchEvent", {
      type: "touchStart",
      touchPoints: [point],
    });
    await page.clock.runFor(240);
    await expect(cue).toContainText("Release to fire");
    await page.clock.runFor(1600);
    await expect(cue).toContainText("Try the shot again");
    await input.send("Input.dispatchTouchEvent", {
      type: "touchEnd",
      touchPoints: [],
    });

    await input.send("Input.dispatchTouchEvent", {
      type: "touchStart",
      touchPoints: [point],
    });
    await page.clock.runFor(240);
    await expect(cue).toContainText("Release to fire");
    await input.send("Input.dispatchTouchEvent", {
      type: "touchEnd",
      touchPoints: [],
    });
    await page.clock.runFor(450);
    await expect(cue).toContainText("Fly through");

    await page.getByRole("button", { name: "Exit tutorial" }).tap();
    await expect(tutorial).toBeFocused();
    await expect(cue).toBeHidden();
    await tutorial.tap();
    await expect(cue).toContainText("Flap");
    await page.keyboard.press("Escape");
    await expect(tutorial).toBeFocused();
  });
});
