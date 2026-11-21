import { expect, test } from "@playwright/test";

async function startWithHeldSpace(page) {
  await page.clock.install({ time: new Date("2026-01-01T00:00:00Z") });
  await page.clock.pauseAt(new Date("2026-01-01T00:01:00Z"));
  await page.goto("/");
  await expect(
    page.getByRole("button", { name: "Endless", exact: true }),
  ).toBeEnabled();
  await page.keyboard.down("Space");
  await page.clock.runFor(240);
}

async function flyTowardFirstWall(page) {
  // Keep the moth near the opening until it reaches the wall.
  await page.clock.runFor(430);
  for (let flap = 0; flap < 3; flap++) {
    await page.keyboard.press("Space");
    await page.clock.runFor(640);
  }
}

test("starts, pauses, and resumes with the keyboard", async ({ page }) => {
  await page.goto("/");
  await expect(
    page.getByRole("button", { name: "Endless", exact: true }),
  ).toBeEnabled();
  await page.keyboard.press("Space");
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");

  await page.keyboard.press("Escape");
  await expect(page.getByRole("heading", { name: "Paused" })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");
});

test("clears the first wall and keeps the result after visiting the tutorial", async ({
  page,
}) => {
  await startWithHeldSpace(page);
  await page.keyboard.up("Space");
  await flyTowardFirstWall(page);
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");

  await page.keyboard.press("Space");
  await page.clock.runFor(2240);
  await expect(page.getByRole("heading", { name: "Sky closed" })).toBeVisible();
  await expect(page.locator("#score")).toHaveText("1");
  await expect(page.locator("#details")).toContainText("Best 1");

  const tutorial = page.getByRole("button", { name: "Tutorial", exact: true });
  await tutorial.press("Enter");
  await expect(
    page.getByRole("heading", { name: "Flap", exact: true }),
  ).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("heading", { name: "Sky closed" })).toBeVisible();
  await expect(page.locator("#score")).toHaveText("1");
  await expect(page.locator("#details")).toContainText("Best 1");
  await expect(tutorial).toBeFocused();

  await page.getByRole("button", { name: "Fly again" }).press("Enter");
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");

  await page.clock.runFor(1600);
  await expect(page.getByRole("heading", { name: "Sky closed" })).toBeVisible();
  await expect(page.locator("#score")).toHaveText("0");
  await expect(page.locator("#details")).toContainText("Best 1");
});

test("pausing during a hold cancels the shot after resuming", async ({
  page,
}) => {
  await startWithHeldSpace(page);
  await page.keyboard.press("Escape");
  await expect(page.getByRole("heading", { name: "Paused" })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");
  await page.keyboard.up("Space");

  await flyTowardFirstWall(page);
  await expect(page.getByRole("heading", { name: "Sky closed" })).toBeVisible();
  await expect(page.locator("#score")).toHaveText("0");
});

test("remembers the sound setting after reload", async ({ page }) => {
  await page.goto("/");
  const sound = page.getByRole("button", { name: "Sound" });
  await expect(sound).toHaveAttribute("aria-pressed", "true");
  await sound.click();
  await expect(sound).toHaveAttribute("aria-pressed", "false");
  await page.reload();
  await expect(sound).toHaveAttribute("aria-pressed", "false");
});

test.describe("phone viewport", () => {
  test.use({
    viewport: { width: 412, height: 915 },
    isMobile: true,
    hasTouch: true,
  });

  test("starts on touch and can pause and resume with touch", async ({
    page,
  }) => {
    await page.goto("/");
    await expect(
      page.getByRole("button", { name: "Endless", exact: true }),
    ).toBeEnabled();
    await page.locator("#sky").tap({ position: { x: 12, y: 20 } });
    await expect(page.locator("#game")).toHaveAttribute(
      "data-phase",
      "playing",
    );

    await page.getByRole("button", { name: "Pause game" }).tap();
    await expect(page.getByRole("heading", { name: "Paused" })).toBeVisible();
    await page.getByRole("button", { name: "Resume" }).tap();
    await expect(page.locator("#game")).toHaveAttribute(
      "data-phase",
      "playing",
    );
  });
});

test("reloads and starts a run offline after the cache is ready", async ({
  context,
  page,
}) => {
  await page.goto("/");
  await expect(
    page.getByRole("button", { name: "Endless", exact: true }),
  ).toBeEnabled();
  await expect
    .poll(() =>
      page.evaluate(() => Boolean(navigator.serviceWorker.controller)),
    )
    .toBe(true);

  await context.setOffline(true);
  await page.reload();
  await page.getByRole("button", { name: "Tutorial", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Flap", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Exit tutorial" }).click();
  await page.getByRole("button", { name: "Endless", exact: true }).click();
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");
});
