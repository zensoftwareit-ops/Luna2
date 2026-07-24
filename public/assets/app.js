(() => {
  'use strict';

  const sidebar = document.querySelector('#sidebar');
  const setMenu = (open) => {
    sidebar?.classList.toggle('open', open);
    document.body.classList.toggle('menu-open', open);
  };
  document.querySelector('[data-menu-toggle]')?.addEventListener('click', () => setMenu(true));
  document.querySelectorAll('[data-menu-close]').forEach((button) => button.addEventListener('click', () => setMenu(false)));
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape') setMenu(false); });

  document.querySelectorAll('form[data-confirm]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      if (!window.confirm(form.dataset.confirm || 'Confermare?')) event.preventDefault();
    });
  });

  document.querySelectorAll('.clickable-row').forEach((row) => {
    row.addEventListener('dblclick', () => { if (row.dataset.href) window.location.href = row.dataset.href; });
  });

  document.querySelector('[data-history-back]')?.addEventListener('click', () => window.history.back());

  const globalSearch = document.querySelector('.topbar-search input[type="search"]');
  document.addEventListener('keydown', (event) => {
    const target = event.target;
    const isTyping = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || target instanceof HTMLSelectElement || target?.isContentEditable;
    if (event.key === '/' && !isTyping && globalSearch) {
      event.preventDefault();
      globalSearch.focus();
      globalSearch.select();
    }
  });

  document.querySelector('[data-filter-toggle]')?.addEventListener('click', () => {
    document.querySelector('[data-filter-panel]')?.classList.toggle('open');
  });

  const rowSelectors = [...document.querySelectorAll('[data-row-select]')];
  const selectAll = document.querySelector('[data-select-all]');
  const selectionCount = document.querySelector('[data-selection-count]');
  const bulkSubmit = document.querySelector('[data-bulk-submit]');
  const refreshSelection = () => {
    const selected = rowSelectors.filter((input) => input.checked).length;
    if (selectionCount) selectionCount.textContent = `${selected} selezionat${selected === 1 ? 'o' : 'i'}`;
    if (bulkSubmit) bulkSubmit.disabled = selected === 0;
    if (selectAll) {
      selectAll.checked = selected > 0 && selected === rowSelectors.length;
      selectAll.indeterminate = selected > 0 && selected < rowSelectors.length;
    }
  };
  selectAll?.addEventListener('change', () => {
    rowSelectors.forEach((input) => { input.checked = selectAll.checked; });
    refreshSelection();
  });
  rowSelectors.forEach((input) => input.addEventListener('change', refreshSelection));
  refreshSelection();

  document.querySelectorAll('.topbar-popover').forEach((popover) => {
    popover.addEventListener('toggle', () => {
      if (!popover.open) return;
      document.querySelectorAll('.topbar-popover[open]').forEach((other) => {
        if (other !== popover) other.removeAttribute('open');
      });
    });
  });
  document.addEventListener('click', (event) => {
    document.querySelectorAll('.topbar-popover[open]').forEach((popover) => {
      if (!popover.contains(event.target)) popover.removeAttribute('open');
    });
  });

  document.querySelectorAll('.file-drop input[type="file"]').forEach((input) => {
    input.addEventListener('change', () => {
      const label = input.closest('.file-drop');
      const strong = label?.querySelector('strong');
      if (strong && input.files?.[0]) strong.textContent = input.files[0].name;
      label?.classList.toggle('has-file', Boolean(input.files?.length));
    });
  });

  document.querySelectorAll('form').forEach((form) => {
    form.addEventListener('submit', (event) => {
      if (event.defaultPrevented) return;
      const button = form.querySelector('button[type="submit"]:focus');
      if (!button || button.dataset.noBusy !== undefined) return;
      button.classList.add('is-busy');
      button.setAttribute('aria-busy', 'true');
    });
  });

  document.querySelectorAll('.module-checkbox').forEach((checkbox) => {
    const refresh = () => {
      const card = checkbox.closest('.module-card');
      card?.classList.toggle('enabled', checkbox.checked);
      const label = card?.querySelector('.module-state small');
      if (label) label.textContent = checkbox.checked ? 'Attivo' : 'Disattivato';
    };
    checkbox.addEventListener('change', refresh);
    refresh();
  });

  const documentForm = document.querySelector('[data-document-form]');
  if (documentForm) {
    const body = documentForm.querySelector('[data-lines]');
    const template = document.querySelector('#line-template');
    let index = 0;
    const addLine = () => {
      const fragment = template.content.cloneNode(true);
      const row = fragment.querySelector('tr');
      row.querySelectorAll('[data-field]').forEach((input) => {
        input.name = `lines[${index}][${input.dataset.field}]`;
      });
      const product = row.querySelector('[data-product-select]');
      product.name = `lines[${index}][product_id]`;
      product.addEventListener('change', () => {
        const option = product.selectedOptions[0];
        if (!option?.value) return;
        row.querySelector('[data-field="product_code"]').value = option.dataset.code || '';
        row.querySelector('[data-field="description"]').value = option.dataset.name || '';
        row.querySelector('[data-field="unit"]').value = option.dataset.unit || 'NR';
        row.querySelector('[data-field="unit_price"]').value = option.dataset.price || '0';
        row.querySelector('[data-field="vat_rate"]').value = option.dataset.vat || '22';
      });
      row.querySelector('[data-remove-line]').addEventListener('click', () => row.remove());
      body.appendChild(fragment);
      index += 1;
    };
    documentForm.querySelector('[data-add-line]')?.addEventListener('click', addLine);
    addLine();
  }

  const accountingForm = document.querySelector('[data-accounting-form]');
  if (accountingForm) {
    const body = accountingForm.querySelector('[data-accounting-lines]');
    const template = document.querySelector('#accounting-line-template');
    let index = 0;
    const decimal = (value) => {
      const raw = String(value).trim();
      const normalized = raw.includes(',') ? raw.replaceAll('.', '').replace(',', '.') : raw;
      return Number.parseFloat(normalized) || 0;
    };
    const refresh = () => {
      let debit = 0; let credit = 0;
      body.querySelectorAll('tr').forEach((row) => {
        debit += decimal(row.querySelector('[data-account-field="debit"]')?.value);
        credit += decimal(row.querySelector('[data-account-field="credit"]')?.value);
      });
      accountingForm.querySelector('[data-debit-total]').textContent = debit.toLocaleString('it-IT', { style: 'currency', currency: 'EUR' });
      accountingForm.querySelector('[data-credit-total]').textContent = credit.toLocaleString('it-IT', { style: 'currency', currency: 'EUR' });
      const status = accountingForm.querySelector('[data-balance-status]');
      if (status) {
        const balanced = debit > 0 && Math.abs(debit - credit) < 0.005;
        status.textContent = balanced ? 'Quadrata' : `Differenza ${(debit - credit).toLocaleString('it-IT', { style: 'currency', currency: 'EUR' })}`;
        status.classList.toggle('balanced', balanced);
        status.classList.toggle('unbalanced', !balanced);
      }
    };
    const addLine = (values = {}) => {
      const fragment = template.content.cloneNode(true);
      const row = fragment.querySelector('tr');
      row.querySelectorAll('[data-account-field]').forEach((input) => {
        input.name = `lines[${index}][${input.dataset.accountField}]`;
        if (Object.hasOwn(values, input.dataset.accountField)) input.value = values[input.dataset.accountField] ?? '';
        input.addEventListener('input', refresh);
        input.addEventListener('change', refresh);
      });
      row.querySelector('[data-remove-line]').addEventListener('click', () => { row.remove(); refresh(); });
      body.appendChild(fragment);
      index += 1;
    };
    accountingForm.querySelector('[data-add-accounting-line]')?.addEventListener('click', addLine);
    let existing = [];
    try { existing = JSON.parse(accountingForm.dataset.existingLines || '[]'); } catch (_) { existing = []; }
    if (existing.length) existing.forEach((line) => addLine(line)); else { addLine(); addLine(); }
    refresh();
  }

  const cleanFilename = (value) => String(value || 'esportazione')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'esportazione';
  const tableMatrix = (table) => {
    const ignored = [...table.querySelectorAll('thead th')].map((cell) =>
      cell.classList.contains('actions-column') || cell.classList.contains('selection-column') || cell.textContent.trim() === '');
    return [...table.rows].filter((row) => row.closest('thead') || row.dataset.filterMatch !== '0').map((row) => [...row.cells]
      .filter((_, index) => !ignored[index])
      .map((cell) => cell.innerText.replace(/\s+/g, ' ').trim()));
  };
  const downloadBlob = (content, type, extension, title) => {
    const blob = new Blob([content], { type });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${cleanFilename(title)}-${new Date().toISOString().slice(0, 10)}.${extension}`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(link.href);
  };
  const csvCell = (value) => `"${String(value).replaceAll('"', '""')}"`;
  const exportVisibleTable = (table, format, title) => {
    const matrix = tableMatrix(table);
    if (format === 'csv') {
      downloadBlob(`\uFEFF${matrix.map((row) => row.map(csvCell).join(';')).join('\r\n')}`, 'text/csv;charset=utf-8', 'csv', title);
      return;
    }
    const rows = matrix.map((row, rowIndex) => `<tr>${row.map((cell) => `<${rowIndex === 0 ? 'th' : 'td'}>${cell.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')}</${rowIndex === 0 ? 'th' : 'td'}>`).join('')}</tr>`).join('');
    const workbook = `\uFEFF<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="utf-8"></head><body><table>${rows}</table></body></html>`;
    downloadBlob(workbook, 'application/vnd.ms-excel;charset=utf-8', 'xls', title);
  };
  if (!document.querySelector('.exportable-toolbar')) {
    document.querySelectorAll('.card .table-wrap table').forEach((table, index) => {
      if (table.closest('[data-no-table-export]')) return;
      const card = table.closest('.card');
      const title = card?.querySelector('h1,h2')?.textContent.trim()
        || document.querySelector('.page-intro h1,.topbar-title strong')?.textContent.trim()
        || `Tabella ${index + 1}`;
      const tools = document.createElement('div');
      tools.className = 'table-export-tools';
      tools.innerHTML = '<label><span>Filtra tabella</span><input type="search" placeholder="Cerca nelle righe…"></label><span>Output</span><button type="button">PDF</button><button type="button">XLS</button><button type="button">CSV</button>';
      const localSearch = tools.querySelector('input');
      const [pdfButton, xlsButton, csvButton] = tools.querySelectorAll('button');
      localSearch.addEventListener('input', () => {
        const needle = localSearch.value.trim().toLocaleLowerCase('it');
        [...table.tBodies].flatMap((body) => [...body.rows]).forEach((row) => {
          row.dataset.filterMatch = needle === '' || row.innerText.toLocaleLowerCase('it').includes(needle) ? '1' : '0';
        });
        table.dispatchEvent(new CustomEvent('luna:table-filter'));
      });
      pdfButton.addEventListener('click', () => {
        document.body.classList.add('print-table');
        card?.classList.add('print-target');
        card?.setAttribute('data-print-title', title);
        window.print();
        setTimeout(() => {
          document.body.classList.remove('print-table');
          card?.classList.remove('print-target');
          card?.removeAttribute('data-print-title');
        }, 300);
      });
      xlsButton.addEventListener('click', () => exportVisibleTable(table, 'xls', title));
      csvButton.addEventListener('click', () => exportVisibleTable(table, 'csv', title));
      const header = card?.querySelector(':scope > .card-header');
      if (header) header.appendChild(tools); else card?.insertBefore(tools, card.firstChild);
    });
  }

  const tableSortValue = (cell) => {
    const raw = cell?.innerText.replace(/\s+/g, ' ').trim() || '';
    const dateMatch = raw.match(/^(\d{2})\/(\d{2})\/(\d{4})/);
    if (dateMatch) return { type: 'number', value: Date.UTC(Number(dateMatch[3]), Number(dateMatch[2]) - 1, Number(dateMatch[1])) };
    const isoDate = raw.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (isoDate) return { type: 'number', value: Date.UTC(Number(isoDate[1]), Number(isoDate[2]) - 1, Number(isoDate[3])) };
    const normalized = raw.replace(/[€%\s]/g, '').replace(/\./g, '').replace(',', '.');
    if (normalized !== '' && /^-?\d+(?:\.\d+)?$/.test(normalized)) {
      return { type: 'number', value: Number(normalized) };
    }
    return { type: 'text', value: raw.toLocaleLowerCase('it') };
  };

  const enhanceDataTable = (table) => {
    if (table.dataset.tableEnhanced === '1'
      || table.matches('.line-table,.selectable-table,[data-no-table-controls]')
      || table.closest('form')
      || !table.tHead
      || table.tBodies.length === 0) return;

    const rows = [...table.tBodies].flatMap((body) => [...body.rows]);
    const dataRows = rows.filter((row) => !row.querySelector('.table-empty') && !row.hasAttribute('data-table-placeholder'));
    if (dataRows.length === 0) return;

    table.dataset.tableEnhanced = '1';
    dataRows.forEach((row, index) => {
      row.dataset.tableOriginalIndex = String(index);
      if (row.dataset.filterMatch === undefined) row.dataset.filterMatch = '1';
    });

    let page = 1;
    let perPage = 25;
    let sortIndex = -1;
    let sortDirection = 'asc';

    const wrap = table.closest('.table-wrap');
    const pager = document.createElement('nav');
    pager.className = 'table-pagination';
    pager.setAttribute('aria-label', 'Paginazione tabella');
    pager.innerHTML = '<span data-table-summary></span><div><button type="button" data-table-prev aria-label="Pagina precedente">‹</button><span data-table-page></span><button type="button" data-table-next aria-label="Pagina successiva">›</button></div><label>Righe <select data-table-size><option>25</option><option>50</option><option>100</option><option>250</option></select></label>';
    wrap.insertAdjacentElement('afterend', pager);

    const summary = pager.querySelector('[data-table-summary]');
    const pageLabel = pager.querySelector('[data-table-page]');
    const previous = pager.querySelector('[data-table-prev]');
    const next = pager.querySelector('[data-table-next]');
    const size = pager.querySelector('[data-table-size]');

    const render = () => {
      const matching = dataRows.filter((row) => row.dataset.filterMatch !== '0');
      const pages = Math.max(1, Math.ceil(matching.length / perPage));
      page = Math.min(Math.max(1, page), pages);
      const start = (page - 1) * perPage;
      const current = new Set(matching.slice(start, start + perPage));
      dataRows.forEach((row) => { row.hidden = !current.has(row); });
      summary.textContent = `${matching.length} righe`;
      pageLabel.textContent = `${page} / ${pages}`;
      previous.disabled = page <= 1;
      next.disabled = page >= pages;
      pager.classList.toggle('is-compact', matching.length <= perPage);
    };

    [...table.tHead.rows[0].cells].forEach((header, index) => {
      if (header.classList.contains('actions-column')
        || header.classList.contains('selection-column')
        || header.textContent.trim() === ''
        || header.querySelector('a,button,input,select')) return;
      header.classList.add('sortable-header');
      header.tabIndex = 0;
      header.setAttribute('role', 'button');
      header.setAttribute('aria-sort', 'none');
      const sort = () => {
        sortDirection = sortIndex === index && sortDirection === 'asc' ? 'desc' : 'asc';
        sortIndex = index;
        [...table.tHead.rows[0].cells].forEach((cell) => cell.setAttribute('aria-sort', cell === header ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none'));
        dataRows.sort((left, right) => {
          const a = tableSortValue(left.cells[index]);
          const b = tableSortValue(right.cells[index]);
          const compared = a.type === 'number' && b.type === 'number'
            ? a.value - b.value
            : String(a.value).localeCompare(String(b.value), 'it', { numeric: true, sensitivity: 'base' });
          const stable = compared || Number(left.dataset.tableOriginalIndex) - Number(right.dataset.tableOriginalIndex);
          return sortDirection === 'asc' ? stable : -stable;
        });
        dataRows.forEach((row) => row.parentElement.appendChild(row));
        page = 1;
        render();
      };
      header.addEventListener('click', sort);
      header.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          sort();
        }
      });
    });

    previous.addEventListener('click', () => { page -= 1; render(); });
    next.addEventListener('click', () => { page += 1; render(); });
    size.addEventListener('change', () => { perPage = Number(size.value) || 25; page = 1; render(); });
    table.addEventListener('luna:table-filter', () => { page = 1; render(); });
    render();
  };

  document.querySelectorAll('.table-wrap > table').forEach(enhanceDataTable);
})();
