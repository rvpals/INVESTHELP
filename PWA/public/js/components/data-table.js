export function dataTable(columns, rows, opts = {}) {
  const { onRowClick, id } = opts;
  return `
    <div class="data-table-wrapper"${id ? ` id="${id}"` : ''}>
      <table class="data-table">
        <thead>
          <tr>${columns.map(c => `<th>${typeof c === 'string' ? c : c.label}</th>`).join('')}</tr>
        </thead>
        <tbody>
          ${rows.map((row, ri) => `
            <tr${onRowClick ? ` class="clickable" data-row="${ri}"` : ''}>
              ${row.map(cell => `<td>${cell ?? ''}</td>`).join('')}
            </tr>
          `).join('')}
          ${rows.length === 0 ? `<tr><td colspan="${columns.length}" style="text-align:center;opacity:0.5;padding:20px">No data</td></tr>` : ''}
        </tbody>
      </table>
    </div>
  `;
}
