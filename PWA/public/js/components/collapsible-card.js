import { getPref, setPref } from '../preferences.js';

export function collapsibleCard(id, title, contentHtml, opts = {}) {
  const pinKey = `pin_card_${id}`;
  const pinned = getPref(pinKey) ?? opts.defaultPinned ?? false;
  const expanded = pinned || opts.defaultExpanded || false;

  return `
    <div class="collapsible-card" id="card-${id}">
      <div class="collapsible-header" data-card="${id}">
        <h3>${title}</h3>
        <button class="pin-btn${pinned ? ' pinned' : ''}" data-pin="${id}" title="Pin">&#128204;</button>
        <span class="arrow${expanded ? ' expanded' : ''}">&#9660;</span>
      </div>
      <div class="collapsible-divider"></div>
      <div class="collapsible-content${expanded ? ' expanded' : ''}" id="content-${id}">
        <div class="collapsible-body">${contentHtml}</div>
      </div>
    </div>
  `;
}

export function initCollapsibleCards(container) {
  container.querySelectorAll('.collapsible-header').forEach(header => {
    header.addEventListener('click', (e) => {
      if (e.target.closest('.pin-btn')) return;
      const id = header.dataset.card;
      const content = document.getElementById(`content-${id}`);
      const arrow = header.querySelector('.arrow');
      content.classList.toggle('expanded');
      arrow.classList.toggle('expanded');
    });
  });

  container.querySelectorAll('.pin-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const id = btn.dataset.pin;
      const pinKey = `pin_card_${id}`;
      const newVal = !getPref(pinKey);
      setPref(pinKey, newVal);
      btn.classList.toggle('pinned', newVal);
    });
  });
}
