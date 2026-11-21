import { expect, test } from "@playwright/test";

test("starts, pauses, and resumes with the keyboard", async ({ page }) => {
  await page.goto("/");
  await expect(
    page.getByRole("button", { name: "Play", exact: true }),
  ).toBeEnabled();
  await page.keyboard.press("Space");
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");

  await page.keyboard.press("Escape");
  await expect(page.getByRole("heading", { name: "Paused" })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");
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
      page.getByRole("button", { name: "Play", exact: true }),
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
    page.getByRole("button", { name: "Play", exact: true }),
  ).toBeEnabled();
  await expect
    .poll(() =>
      page.evaluate(() => Boolean(navigator.serviceWorker.controller)),
    )
    .toBe(true);

  await context.setOffline(true);
  await page.reload();
  await page.getByRole("button", { name: "Play", exact: true }).click();
  await expect(page.locator("#game")).toHaveAttribute("data-phase", "playing");
});
