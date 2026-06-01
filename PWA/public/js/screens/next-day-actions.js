import { positions, yahoo } from '../api.js';

export async function render(container) {
  container.innerHTML = `<div class="screen"><h2>Next Day Actions</h2><div class="spinner mt-16"></div></div>`;

  try {
    const posList = await positions.list();
    const actions = [];

    for (const p of posList.slice(0, 10)) {
      try {
        const analysis = await yahoo.analysis(p.ticker);
        if (analysis.calendarEvents?.earnings) {
          const e = analysis.calendarEvents.earnings;
          if (e.earningsDate?.[0]) {
            actions.push({ ticker: p.ticker, type: 'Earnings', date: new Date(e.earningsDate[0].raw * 1000).toLocaleDateString() });
          }
        }
      } catch {}
    }

    container.innerHTML = `<div class="screen">
      <h2 class="mb-16">Next Day Actions</h2>
      ${actions.length > 0 ? actions.map(a => `
        <div class="card p-12 mb-8">
          <div class="flex justify-between"><span class="text-bold">${a.ticker}</span><span class="badge badge-green">${a.type}</span></div>
          <div class="text-xs text-muted">${a.date}</div>
        </div>
      `).join('') : '<div class="text-center text-muted p-16">No upcoming events found</div>'}
    </div>`;
  } catch (err) {
    container.innerHTML = `<div class="screen"><h2>Next Day Actions</h2><div class="text-muted mt-16">Error: ${err.message}</div></div>`;
  }
}
