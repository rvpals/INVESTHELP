import { formatCurrency } from '../utils/format.js';

export function renderLineChart(canvas, points, opts = {}) {
  if (!points || points.length < 2) return;
  const ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.parentElement.clientWidth;
  const h = opts.height || 200;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';
  ctx.scale(dpr, dpr);

  const pad = { top: 10, right: 10, bottom: 30, left: 60 };
  const cw = w - pad.left - pad.right;
  const ch = h - pad.top - pad.bottom;

  const xs = points.map(p => p.x);
  const ys = points.map(p => p.y);
  const xMin = Math.min(...xs), xMax = Math.max(...xs);
  const yMin = Math.min(...ys), yMax = Math.max(...ys);
  const yRange = yMax - yMin || 1;

  const toX = x => pad.left + ((x - xMin) / (xMax - xMin || 1)) * cw;
  const toY = y => pad.top + ch - ((y - yMin) / yRange) * ch;

  // Grid
  ctx.strokeStyle = getCSS('--outline') || '#79747E';
  ctx.globalAlpha = 0.2;
  ctx.lineWidth = 1;
  for (let i = 0; i <= 3; i++) {
    const y = pad.top + (ch / 3) * i;
    ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(w - pad.right, y); ctx.stroke();
  }
  ctx.globalAlpha = 1;

  // Y-axis labels
  ctx.fillStyle = getCSS('--on-surface') || '#1C1B1F';
  ctx.font = '10px sans-serif';
  ctx.textAlign = 'right';
  ctx.textBaseline = 'middle';
  for (let i = 0; i <= 3; i++) {
    const val = yMax - (yRange / 3) * i;
    const y = pad.top + (ch / 3) * i;
    ctx.fillText(formatCurrency(val), pad.left - 6, y);
  }

  // Line
  const color = opts.color || getCSS('--primary') || '#2E7D32';
  ctx.strokeStyle = color;
  ctx.lineWidth = 2;
  ctx.lineJoin = 'round';
  ctx.beginPath();
  points.forEach((p, i) => {
    const x = toX(p.x), y = toY(p.y);
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.stroke();

  // Fill area
  ctx.globalAlpha = 0.08;
  ctx.fillStyle = color;
  ctx.lineTo(toX(points[points.length - 1].x), pad.top + ch);
  ctx.lineTo(toX(points[0].x), pad.top + ch);
  ctx.closePath();
  ctx.fill();
  ctx.globalAlpha = 1;

  // Points
  if (points.length <= 60) {
    points.forEach(p => {
      ctx.beginPath();
      ctx.arc(toX(p.x), toY(p.y), 3, 0, Math.PI * 2);
      ctx.fillStyle = color;
      ctx.fill();
    });
  }

  // X-axis labels
  ctx.fillStyle = getCSS('--on-surface') || '#1C1B1F';
  ctx.font = '10px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'top';
  const step = Math.max(1, Math.floor(points.length / 5));
  for (let i = 0; i < points.length; i += step) {
    const p = points[i];
    const x = toX(p.x);
    const label = p.dateLabel || new Date(p.x * (opts.xIsEpochDays ? 86400000 : 1000)).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    ctx.fillText(label, x, h - pad.bottom + 8);
  }

  // Tap-to-select
  if (opts.interactive !== false) {
    canvas.onclick = (e) => {
      const rect = canvas.getBoundingClientRect();
      const mx = e.clientX - rect.left;
      let closest = null, minDist = Infinity;
      points.forEach(p => {
        const d = Math.abs(toX(p.x) - mx);
        if (d < minDist) { minDist = d; closest = p; }
      });
      if (closest && minDist < 30) {
        renderLineChart(canvas, points, opts);
        const cx = toX(closest.x), cy = toY(closest.y);
        // Guide line
        ctx.strokeStyle = color;
        ctx.setLineDash([4, 4]);
        ctx.beginPath(); ctx.moveTo(cx, pad.top); ctx.lineTo(cx, pad.top + ch); ctx.stroke();
        ctx.setLineDash([]);
        // Tooltip
        ctx.fillStyle = getCSS('--on-surface') || '#1C1B1F';
        ctx.font = 'bold 12px sans-serif';
        ctx.textAlign = cx > w / 2 ? 'right' : 'left';
        const tx = cx + (cx > w / 2 ? -8 : 8);
        ctx.fillText(formatCurrency(closest.y), tx, cy - 14);
        const dateStr = closest.dateLabel || new Date(closest.x * (opts.xIsEpochDays ? 86400000 : 1000)).toLocaleDateString();
        ctx.font = '10px sans-serif';
        ctx.fillText(dateStr, tx, cy - 2);
        // Point highlight
        ctx.beginPath(); ctx.arc(cx, cy, 5, 0, Math.PI * 2);
        ctx.fillStyle = color; ctx.fill();
        ctx.strokeStyle = '#fff'; ctx.lineWidth = 2; ctx.stroke();
      }
    };
  }
}

function getCSS(prop) {
  return getComputedStyle(document.documentElement).getPropertyValue(prop).trim();
}
