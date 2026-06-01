export function renderMiniChart(canvas, points, opts = {}) {
  if (!points || points.length < 2) { canvas.style.display = 'none'; return; }
  canvas.style.display = 'block';
  const ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.parentElement.clientWidth;
  const h = opts.height || 100;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';
  ctx.scale(dpr, dpr);

  const pad = 8;
  const cw = w - pad * 2;
  const ch = h - pad * 2;

  const ys = points.map(p => p.y);
  const yMin = Math.min(...ys), yMax = Math.max(...ys);
  const yRange = yMax - yMin || 1;

  const toX = (i) => pad + (i / (points.length - 1)) * cw;
  const toY = (y) => pad + ch - ((y - yMin) / yRange) * ch;

  // Grid
  const color = opts.color || getComputedStyle(document.documentElement).getPropertyValue('--primary').trim() || '#2E7D32';
  ctx.strokeStyle = color;
  ctx.globalAlpha = 0.1;
  for (let i = 0; i < 3; i++) {
    const y = pad + (ch / 2) * i;
    ctx.beginPath(); ctx.moveTo(pad, y); ctx.lineTo(w - pad, y); ctx.stroke();
  }
  ctx.globalAlpha = 1;

  // Line
  ctx.strokeStyle = color;
  ctx.lineWidth = 2;
  ctx.lineJoin = 'round';
  ctx.beginPath();
  points.forEach((p, i) => {
    const x = toX(i), y = toY(p.y);
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.stroke();

  // Area fill
  ctx.globalAlpha = 0.06;
  ctx.fillStyle = color;
  ctx.lineTo(toX(points.length - 1), pad + ch);
  ctx.lineTo(toX(0), pad + ch);
  ctx.closePath();
  ctx.fill();
  ctx.globalAlpha = 1;
}
