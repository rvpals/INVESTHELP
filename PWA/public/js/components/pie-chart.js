const CHART_COLORS = [
  '#2E7D32', '#0277BD', '#E65100', '#6A1B9A', '#33691E', '#B71C1C',
  '#37474F', '#F9A825', '#C2185B', '#424242', '#5C6BC0', '#BF360C',
  '#00796B', '#455A64', '#4E342E', '#1A237E', '#00897B', '#880E4F',
  '#F57F17', '#01579B',
];

export function renderPieChart(canvas, data, opts = {}) {
  if (!data || data.length === 0) return;
  const ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.parentElement.clientWidth;
  const h = opts.height || 200;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';
  ctx.scale(dpr, dpr);

  const total = data.reduce((s, d) => s + d.value, 0);
  if (total === 0) return;

  const cx = w / 2;
  const cy = h / 2;
  const radius = Math.min(cx, cy) - 10;
  let startAngle = -Math.PI / 2;

  data.forEach((d, i) => {
    const sliceAngle = (d.value / total) * 2 * Math.PI;
    const color = d.color || CHART_COLORS[i % CHART_COLORS.length];

    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.arc(cx, cy, radius, startAngle, startAngle + sliceAngle);
    ctx.closePath();
    ctx.fillStyle = color;
    ctx.fill();

    // Label inside slice
    if (sliceAngle > 0.2) {
      const midAngle = startAngle + sliceAngle / 2;
      const lx = cx + Math.cos(midAngle) * radius * 0.65;
      const ly = cy + Math.sin(midAngle) * radius * 0.65;
      ctx.fillStyle = '#fff';
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(d.label, lx, ly);
    }

    startAngle += sliceAngle;
  });
}
