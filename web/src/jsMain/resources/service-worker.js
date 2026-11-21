const CACHE_PREFIX = `skywright:${self.registration.scope}:`;
const CACHE_NAME = `${CACHE_PREFIX}1`;
const entry = new URL("index.html", self.registration.scope).href;
const assets = new Set(
  [
    "index.html",
    "style.css",
    "skywright.js",
    "manifest.webmanifest",
    "icon-192.png",
    "icon-512.png",
  ].map((path) => new URL(path, self.registration.scope).href),
);

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll([...assets])),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      for (const key of await caches.keys()) {
        if (key.startsWith(CACHE_PREFIX) && key !== CACHE_NAME)
          await caches.delete(key);
      }
      await self.clients.claim();
    })(),
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  const isEntry =
    request.mode === "navigate" &&
    (url.origin + url.pathname === self.registration.scope ||
      url.origin + url.pathname === entry);
  if (!isEntry && !assets.has(request.url)) return;
  event.respondWith(
    (async () => {
      const cache = await caches.open(CACHE_NAME);
      const key = isEntry ? entry : request;
      try {
        const response = await fetch(request);
        if (response.ok) await cache.put(key, response.clone()).catch(() => {});
        return response;
      } catch (error) {
        const cached = await cache.match(key);
        if (cached) return cached;
        throw error;
      }
    })(),
  );
});
