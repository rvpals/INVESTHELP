import { dateInputValue, dateInputToEpochDays } from '../utils/dates.js';

export function dateRangePicker(startEpochDays, endEpochDays, onChangeId) {
  return `
    <div class="flex gap-8 items-center">
      <div class="form-group" style="flex:1">
        <label>From</label>
        <input type="date" class="input" id="${onChangeId}-start" value="${dateInputValue(startEpochDays)}">
      </div>
      <div class="form-group" style="flex:1">
        <label>To</label>
        <input type="date" class="input" id="${onChangeId}-end" value="${dateInputValue(endEpochDays)}">
      </div>
    </div>
  `;
}

export function initDateRangePicker(container, id, onChange) {
  const startEl = container.querySelector(`#${id}-start`);
  const endEl = container.querySelector(`#${id}-end`);
  if (startEl) startEl.addEventListener('change', () => onChange(dateInputToEpochDays(startEl.value), dateInputToEpochDays(endEl.value)));
  if (endEl) endEl.addEventListener('change', () => onChange(dateInputToEpochDays(startEl.value), dateInputToEpochDays(endEl.value)));
}
