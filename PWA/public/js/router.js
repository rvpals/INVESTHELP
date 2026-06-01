const routes = [];
let currentCleanup = null;

export function route(pattern, handler) {
  const paramNames = [];
  const regex = pattern.replace(/:(\w+)/g, (_, name) => {
    paramNames.push(name);
    return '([^/]+)';
  });
  routes.push({ regex: new RegExp('^' + regex + '$'), paramNames, handler });
}

export function navigate(hash) {
  location.hash = hash;
}

export function getHash() {
  return location.hash.slice(1) || '/';
}

export async function handleRoute() {
  const hash = getHash();
  const app = document.getElementById('app');

  for (const r of routes) {
    const match = hash.match(r.regex);
    if (match) {
      const params = {};
      r.paramNames.forEach((name, i) => {
        params[name] = decodeURIComponent(match[i + 1]);
      });
      if (currentCleanup) { currentCleanup(); currentCleanup = null; }
      app.innerHTML = '<div class="loading-screen"><div class="spinner"></div></div>';
      try {
        const cleanup = await r.handler(app, params);
        if (typeof cleanup === 'function') currentCleanup = cleanup;
      } catch (err) {
        app.innerHTML = `<div class="screen"><h2>Error</h2><p>${err.message}</p></div>`;
        console.error('Route error:', err);
      }
      return;
    }
  }
  app.innerHTML = '<div class="screen"><h2>Not Found</h2><p>Page not found.</p></div>';
}

window.addEventListener('hashchange', handleRoute);
