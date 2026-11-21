import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const reportUrl = new URL(
  "../build/coverage/report/coverage-final.json",
  import.meta.url,
);
const coverage = JSON.parse(readFileSync(reportUrl, "utf8"));

for (const name of ["GameCore", "ScreenLayout"]) {
  const sourceUrl = new URL(
    `../src/jsMain/kotlin/com/skywright/game/${name}.kt`,
    import.meta.url,
  );
  const file = coverage[fileURLToPath(sourceUrl)];
  if (!file || !Object.values(file.f).some((hits) => hits > 0)) {
    throw new Error(
      `Coverage did not map executed functions back to ${name}.kt`,
    );
  }
}
