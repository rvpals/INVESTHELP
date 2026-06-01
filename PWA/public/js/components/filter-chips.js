export function filterChips(items, selected, onChange) {
  return `<div class="chip-row">${items.map(item => {
    const isSelected = Array.isArray(selected) ? selected.includes(item.value) : selected === item.value;
    return `<button class="chip${isSelected ? ' selected' : ''}" data-value="${item.value}">${item.label}</button>`;
  }).join('')}</div>`;
}

export function initFilterChips(container, onChange) {
  container.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
      chip.classList.toggle('selected');
      const selected = Array.from(container.querySelectorAll('.chip.selected')).map(c => c.dataset.value);
      onChange(selected);
    });
  });
}

export function initSingleSelect(container, onChange) {
  container.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
      container.querySelectorAll('.chip').forEach(c => c.classList.remove('selected'));
      chip.classList.add('selected');
      onChange(chip.dataset.value);
    });
  });
}
