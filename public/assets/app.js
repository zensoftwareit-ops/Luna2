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

  document.querySelectorAll('[data-copy-target]').forEach((button) => {
    button.addEventListener('click', async () => {
      const input = document.getElementById(button.dataset.copyTarget || '');
      if (!(input instanceof HTMLInputElement)) return;
      try {
        if (navigator.clipboard?.writeText) {
          await navigator.clipboard.writeText(input.value);
        } else {
          input.focus();
          input.select();
          document.execCommand('copy');
        }
        const original = button.textContent;
        button.textContent = 'Copiato';
        window.setTimeout(() => { button.textContent = original; }, 1600);
      } catch (_error) {
        input.focus();
        input.select();
      }
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
    const vatCatalogNode = document.querySelector('[data-vat-article-catalog]');
    let vatArticles = [];
    try { vatArticles = JSON.parse(vatCatalogNode?.textContent || '[]'); } catch (_error) { vatArticles = []; }
    const vatByCode = new Map(vatArticles.map((article) => [String(article.code || '').toUpperCase(), article]));
    let index = 0;
    const addLine = () => {
      const fragment = template.content.cloneNode(true);
      const row = fragment.querySelector('tr');
      row.querySelectorAll('[data-field]').forEach((input) => {
        input.name = `lines[${index}][${input.dataset.field}]`;
      });
      const product = row.querySelector('[data-product-select]');
      const vatArticle = row.querySelector('[data-field="vat_code"]');
      const applyVatArticle = () => {
        if (!vatArticle) return;
        const article = vatByCode.get(String(vatArticle.value || '').trim().toUpperCase());
        const help = row.querySelector('[data-vat-article-help]');
        if (!article) {
          vatArticle.setCustomValidity(vatArticle.value ? 'Seleziona un articolo IVA presente nel catalogo.' : 'Seleziona l’articolo IVA.');
          if (help) help.textContent = '';
          return;
        }
        vatArticle.value = article.code;
        vatArticle.setCustomValidity('');
        const nature = row.querySelector('[data-field="vat_nature"]');
        const rate = row.querySelector('[data-field="vat_rate"]');
        if (nature) nature.value = article.nature || '';
        if (rate && Number(article.rate || 0) > 0) rate.value = article.rate;
        if (help) help.textContent = `${article.description}${article.nature ? ` · ${article.nature}` : ''} · detraibile ${Number(article.deductible_percent ?? 100).toLocaleString('it-IT')}%`;
      };
      vatArticle?.addEventListener('change', applyVatArticle);
      vatArticle?.addEventListener('blur', applyVatArticle);
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
    const requireBalanced = accountingForm.dataset.requireBalanced === '1';
    const refresh = () => {
      let debit = 0; let credit = 0; let invalidSide = false;
      const rows = [...body.querySelectorAll('tr')];
      rows.forEach((row) => {
        const rowDebit = decimal(row.querySelector('[data-account-field="debit"]')?.value);
        const rowCredit = decimal(row.querySelector('[data-account-field="credit"]')?.value);
        debit += rowDebit;
        credit += rowCredit;
        if (rowDebit > 0 && rowCredit > 0) invalidSide = true;
      });
      accountingForm.querySelector('[data-debit-total]').textContent = debit.toLocaleString('it-IT', { style: 'currency', currency: 'EUR' });
      accountingForm.querySelector('[data-credit-total]').textContent = credit.toLocaleString('it-IT', { style: 'currency', currency: 'EUR' });
      const status = accountingForm.querySelector('[data-balance-status]');
      const balanced = rows.length >= 2 && debit > 0 && !invalidSide && Math.abs(debit - credit) < 0.005;
      if (status) {
        status.textContent = balanced
          ? 'Quadrata'
          : invalidSide
            ? 'Una riga contiene sia Dare sia Avere'
            : rows.length < 2
              ? 'Servono almeno due righe'
              : `Differenza ${(debit - credit).toLocaleString('it-IT', { style: 'currency', currency: 'EUR' })}`;
        status.classList.toggle('balanced', balanced);
        status.classList.toggle('unbalanced', !balanced);
      }
      return balanced;
    };
    let accountCatalog = [];
    try { accountCatalog = JSON.parse(document.querySelector('[data-manual-account-catalog]')?.textContent || '[]'); } catch (_) { accountCatalog = []; }
    const normalizeAccount = (text) => String(text || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('it');
    const setupAccountLookup = (row, values) => {
      const lookup = row.querySelector('[data-line-account-lookup]');
      const search = row.querySelector('[data-line-account-search]');
      const hidden = row.querySelector('[data-account-field="account_id"]');
      const results = row.querySelector('[data-line-account-results]');
      const select = (account) => { hidden.value = String(account.id); search.value = `${account.code} · ${account.name}`; results.hidden = true; search.setCustomValidity(''); };
      const resolveTypedAccount = () => {
        const query = normalizeAccount(search.value.trim());
        if (!query) return false;
        const exact = accountCatalog.find((account) => normalizeAccount(account.code) === query || normalizeAccount(`${account.code} · ${account.name}`) === query);
        const candidates = accountCatalog.filter((account) => normalizeAccount(`${account.code} ${account.name}`).includes(query));
        const match = exact || (candidates.length === 1 ? candidates[0] : null);
        if (!match) return false;
        select(match);
        return true;
      };
      const render = () => {
        const query = normalizeAccount(search.value);
        const matches = accountCatalog.filter((account) => !query || normalizeAccount(`${account.code} ${account.name}`).includes(query)).slice(0, 35);
        results.replaceChildren(...matches.map((account) => { const option = document.createElement('button'); option.type = 'button'; option.className = 'account-lookup-option'; option.innerHTML = `<strong></strong><small></small>`; option.querySelector('strong').textContent = account.code; option.querySelector('small').textContent = account.name; option.addEventListener('click', () => select(account)); return option; }));
        if (!matches.length) { const empty = document.createElement('div'); empty.className = 'account-lookup-empty'; empty.textContent = 'Nessun conto trovato.'; results.appendChild(empty); }
        results.hidden = false;
      };
      const selected = accountCatalog.find((account) => Number(account.id) === Number(values.account_id || 0));
      if (selected) select(selected);
      search.addEventListener('focus', render);
      search.addEventListener('input', () => { hidden.value = ''; search.setCustomValidity(''); render(); });
      search.addEventListener('blur', () => window.setTimeout(() => { if (!hidden.value) resolveTypedAccount(); results.hidden = true; }, 150));
      search.addEventListener('change', () => { if (!hidden.value && !resolveTypedAccount()) search.setCustomValidity('Seleziona un conto dai risultati.'); });
      search.addEventListener('keydown', (event) => {
        if (event.key !== 'Enter') return;
        if (!hidden.value && resolveTypedAccount()) event.preventDefault();
      });
      lookup.resolveTypedAccount = resolveTypedAccount;
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
      setupAccountLookup(row, values);
      row.querySelector('[data-remove-line]').addEventListener('click', () => { row.remove(); refresh(); });
      body.appendChild(fragment);
      index += 1;
    };
    accountingForm.querySelector('[data-add-accounting-line]')?.addEventListener('click', addLine);
    let existing = [];
    try { existing = JSON.parse(accountingForm.dataset.existingLines || '[]'); } catch (_) { existing = []; }
    if (existing.length) existing.forEach((line) => addLine(line)); else { addLine(); addLine(); }
    accountingForm.addEventListener('submit', (event) => {
      let accountsValid = true;
      body.querySelectorAll('[data-line-account-lookup]').forEach((lookup) => {
        const search = lookup.querySelector('[data-line-account-search]');
        const hidden = lookup.querySelector('[data-account-field="account_id"]');
        if (!hidden.value && typeof lookup.resolveTypedAccount === 'function') lookup.resolveTypedAccount();
        const valid = Boolean(hidden.value);
        search.setCustomValidity(valid ? '' : 'Seleziona un conto dai risultati della ricerca.');
        if (!valid) accountsValid = false;
      });
      const balanceValid = !requireBalanced || refresh();
      const amountInput = body.querySelector('[data-account-field="debit"]');
      if (amountInput) amountInput.setCustomValidity(balanceValid ? '' : 'La scrittura deve avere almeno due righe e totale Dare uguale al totale Avere.');
      if (!accountsValid || !balanceValid) { event.preventDefault(); accountingForm.reportValidity(); }
    });
    refresh();
  }

  document.querySelectorAll('[data-guided-payment]').forEach((form) => {
    const lookup = form.querySelector('[data-guided-payment-lookup]');
    const search = form.querySelector('[data-guided-payment-search]');
    const item = form.querySelector('[data-guided-payment-item]');
    const results = form.querySelector('[data-guided-payment-results]');
    const amount = form.querySelector('[data-guided-payment-amount]');
    let catalog = [];
    let activeIndex = -1;
    try { catalog = JSON.parse(form.querySelector('[data-guided-payment-catalog]')?.textContent || '[]'); } catch (_) { catalog = []; }
    const normalize = (value) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('it');
    const close = () => { results.hidden = true; search.setAttribute('aria-expanded', 'false'); activeIndex = -1; };
    const select = (entry) => {
      item.value = String(entry.id);
      search.value = entry.label;
      search.setCustomValidity('');
      amount.value = String(entry.outstanding).replace('.', ',');
      close();
    };
    const matches = () => {
      const query = normalize(search.value.trim());
      return catalog.filter((entry) => !query || normalize(entry.label).includes(query)).slice(0, 40);
    };
    const render = () => {
      const filtered = matches();
      results.replaceChildren(...filtered.map((entry) => {
        const option = document.createElement('button');
        option.type = 'button';
        option.className = 'account-lookup-option';
        option.setAttribute('role', 'option');
        option.innerHTML = '<strong></strong><small></small>';
        option.querySelector('strong').textContent = entry.primary;
        option.querySelector('small').textContent = entry.secondary;
        option.addEventListener('mousedown', (event) => event.preventDefault());
        option.addEventListener('click', () => select(entry));
        return option;
      }));
      if (!filtered.length) {
        const empty = document.createElement('div');
        empty.className = 'account-lookup-empty';
        empty.textContent = 'Nessuna fattura o partita aperta trovata.';
        results.appendChild(empty);
      }
      results.hidden = false;
      search.setAttribute('aria-expanded', 'true');
      activeIndex = -1;
    };
    search?.addEventListener('focus', render);
    search?.addEventListener('input', () => { item.value = ''; amount.value = ''; search.setCustomValidity(''); render(); });
    search?.addEventListener('blur', () => window.setTimeout(close, 150));
    search?.addEventListener('keydown', (event) => {
      const options = [...results.querySelectorAll('.account-lookup-option')];
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        if (results.hidden) render();
        const visible = [...results.querySelectorAll('.account-lookup-option')];
        if (!visible.length) return;
        activeIndex = event.key === 'ArrowDown'
          ? (activeIndex + 1) % visible.length
          : (activeIndex <= 0 ? visible.length - 1 : activeIndex - 1);
        visible.forEach((option, index) => option.classList.toggle('active', index === activeIndex));
        visible[activeIndex].scrollIntoView({ block: 'nearest' });
      } else if (event.key === 'Enter' && !results.hidden) {
        const target = options[activeIndex] || (options.length === 1 ? options[0] : null);
        if (target) { event.preventDefault(); target.click(); }
      } else if (event.key === 'Escape') {
        close();
      }
    });
    form.addEventListener('submit', (event) => {
      const valid = Boolean(item.value);
      search.setCustomValidity(valid ? '' : 'Cerca e seleziona una fattura dai risultati.');
      if (!valid) { event.preventDefault(); search.reportValidity(); }
    });
  });

  document.querySelectorAll('[data-vat-summary]').forEach((root) => {
    const form = root.querySelector('[data-vat-summary-form]');
    const body = root.querySelector('[data-vat-summary-rows]');
    const template = root.querySelector('[data-vat-summary-row-template]');
    const taxableNode = root.querySelector('[data-vat-summary-taxable]');
    const vatNode = root.querySelector('[data-vat-summary-vat]');
    const statusNode = root.querySelector('[data-vat-summary-status]');
    const expectedTaxable = Number.parseFloat(root.dataset.expectedTaxable || '0') || 0;
    const expectedVat = Number.parseFloat(root.dataset.expectedVat || '0') || 0;
    let initial = [];
    let articles = [];
    let index = 0;
    try { initial = JSON.parse(root.querySelector('[data-vat-summary-initial]')?.textContent || '[]'); } catch (_) { initial = []; }
    try { articles = JSON.parse(root.querySelector('[data-vat-summary-articles]')?.textContent || '[]'); } catch (_) { articles = []; }
    const normalizeCode = (value) => String(value || '').trim().toLocaleUpperCase('it');
    const decimal = (value) => {
      const raw = String(value ?? '').trim();
      return Number.parseFloat(raw.includes(',') ? raw.replaceAll('.', '').replace(',', '.') : raw) || 0;
    };
    const money = (value) => value.toLocaleString('it-IT', { style: 'currency', currency: 'EUR' });
    const article = (code) => articles.find((candidate) => normalizeCode(candidate.code) === normalizeCode(code));
    const refresh = () => {
      let taxable = 0;
      let vat = 0;
      body.querySelectorAll('tr').forEach((row) => {
        taxable += decimal(row.querySelector('[data-vat-summary-field="taxable_amount"]')?.value);
        vat += decimal(row.querySelector('[data-vat-summary-field="vat_amount"]')?.value);
      });
      taxable = Math.round(taxable * 100) / 100;
      vat = Math.round(vat * 100) / 100;
      taxableNode.textContent = money(taxable);
      vatNode.textContent = money(vat);
      const taxableDifference = Math.round((taxable - expectedTaxable) * 100) / 100;
      const vatDifference = Math.round((vat - expectedVat) * 100) / 100;
      const balanced = Math.abs(taxableDifference) <= 0.02 && Math.abs(vatDifference) <= 0.02;
      statusNode.textContent = balanced
        ? 'Quadrato con il documento'
        : `Da quadrare · imponibile ${money(taxableDifference)} · IVA ${money(vatDifference)}`;
      statusNode.classList.toggle('balanced', balanced);
      statusNode.classList.toggle('unbalanced', !balanced);
      return balanced;
    };
    const updateArticle = (row, applyDefault = false) => {
      const code = row.querySelector('[data-vat-summary-field="vat_code"]');
      const description = row.querySelector('[data-vat-summary-description]');
      const metadata = row.querySelector('[data-vat-summary-metadata]');
      const deductible = row.querySelector('[data-vat-summary-field="deductible_percent"]');
      const selected = article(code.value);
      code.setCustomValidity(selected ? '' : 'Seleziona un articolo IVA valido dal catalogo.');
      description.textContent = selected?.description || 'Articolo non riconosciuto';
      metadata.textContent = selected
        ? `${Number(selected.rate || 0).toLocaleString('it-IT', { maximumFractionDigits: 2 })}%${selected.nature ? ` · ${selected.nature}` : ''}`
        : '—';
      if (selected && applyDefault) deductible.value = String(selected.deductible_percent ?? 100);
    };
    const valuesFrom = (row) => Object.fromEntries([...row.querySelectorAll('[data-vat-summary-field]')]
      .map((input) => [input.dataset.vatSummaryField, input.value]));
    const addRow = (values = {}) => {
      const fragment = template.content.cloneNode(true);
      const row = fragment.querySelector('tr');
      row.querySelectorAll('[data-vat-summary-field]').forEach((input) => {
        const field = input.dataset.vatSummaryField;
        input.name = `rows[${index}][${field}]`;
        input.value = Object.hasOwn(values, field) ? values[field] : (field === 'deductible_percent' ? '100' : field === 'purchase_classification' ? 'ORDINARY' : '');
        input.addEventListener('input', refresh);
        input.addEventListener('change', refresh);
      });
      const code = row.querySelector('[data-vat-summary-field="vat_code"]');
      code.addEventListener('input', () => updateArticle(row, false));
      code.addEventListener('change', () => updateArticle(row, true));
      row.querySelector('[data-remove-vat-summary-row]').addEventListener('click', () => {
        if (body.children.length <= 1) return;
        row.remove();
        refresh();
      });
      row.querySelector('[data-split-vat-summary-row]').addEventListener('click', () => {
        const valuesToSplit = valuesFrom(row);
        const taxableInput = row.querySelector('[data-vat-summary-field="taxable_amount"]');
        const vatInput = row.querySelector('[data-vat-summary-field="vat_amount"]');
        const originalTaxable = decimal(taxableInput.value);
        const originalVat = decimal(vatInput.value);
        const firstTaxable = Math.round(originalTaxable * 50) / 100;
        const firstVat = Math.round(originalVat * 50) / 100;
        taxableInput.value = firstTaxable.toFixed(2);
        vatInput.value = firstVat.toFixed(2);
        valuesToSplit.taxable_amount = (originalTaxable - firstTaxable).toFixed(2);
        valuesToSplit.vat_amount = (originalVat - firstVat).toFixed(2);
        const added = addRow(valuesToSplit);
        added.querySelector('[data-vat-summary-field="vat_code"]').focus();
        refresh();
      });
      body.appendChild(fragment);
      index += 1;
      updateArticle(row, false);
      refresh();
      return row;
    };
    initial.forEach((row) => addRow(row));
    if (!initial.length) addRow();
    root.querySelector('[data-add-vat-summary-row]')?.addEventListener('click', () => addRow());
    form.addEventListener('submit', (event) => {
      let articlesValid = true;
      body.querySelectorAll('tr').forEach((row) => {
        updateArticle(row, false);
        if (!article(row.querySelector('[data-vat-summary-field="vat_code"]').value)) articlesValid = false;
      });
      const balanced = refresh();
      const firstTaxable = body.querySelector('[data-vat-summary-field="taxable_amount"]');
      firstTaxable.setCustomValidity(balanced ? '' : 'Il riepilogo IVA deve quadrare con imponibile e IVA del documento.');
      if (!articlesValid || !balanced) {
        event.preventDefault();
        form.reportValidity();
      }
    });
  });

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
  const dataTables = [...document.querySelectorAll('.table-wrap table')].filter((table) =>
    !table.closest('form') && !table.matches('.line-table') && !table.closest('[data-no-table-tools]'));
  const parseTableDate = (value) => {
    const text = String(value || '').trim();
    const italian = text.match(/(\d{2})\/(\d{2})\/(\d{4})/);
    if (italian) return Date.UTC(Number(italian[3]), Number(italian[2]) - 1, Number(italian[1]));
    const iso = text.match(/(\d{4})-(\d{2})-(\d{2})/);
    return iso ? Date.UTC(Number(iso[1]), Number(iso[2]) - 1, Number(iso[3])) : null;
  };
  dataTables.filter((table) => !table.matches('.server-table,.selectable-table')).forEach((table, index) => {
      const wrap = table.closest('.table-wrap');
      const card = table.closest('.card');
      const printContainer = card || table.closest('section') || wrap;
      const title = card?.querySelector('h1,h2')?.textContent.trim()
        || document.querySelector('.page-intro h1,.topbar-title strong')?.textContent.trim()
        || `Tabella ${index + 1}`;
      const headers = [...(table.tHead?.rows[0]?.cells || [])].map((cell) => cell.textContent.replace(/\s+/g, ' ').trim());
      const dateIndex = headers.findIndex((label) => /data|scadenza|periodo|inizio|fine|apert|generat|creat|aggiornat/i.test(label));
      const partyIndex = headers.findIndex((label) => /cliente|fornitore|controparte|azienda/i.test(label));
      const parties = partyIndex < 0 ? [] : [...new Set([...table.tBodies].flatMap((body) => [...body.rows])
        .map((row) => row.cells[partyIndex]?.innerText.replace(/\s+/g, ' ').trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'it'));
      const tools = document.createElement('div');
      tools.className = 'table-export-tools';
      tools.innerHTML = '<div class="table-filter-fields"><label><span>Ricerca</span><input data-table-search type="search" placeholder="Cerca nelle righe…"></label></div><div class="table-output-actions"><span>Esporta</span><button type="button">PDF</button><button type="button">XLS</button><button type="button">CSV</button></div>';
      const fields = tools.querySelector('.table-filter-fields');
      if (dateIndex >= 0) fields.insertAdjacentHTML('beforeend', '<label><span>Dal</span><input data-table-date-from type="date"></label><label><span>Al</span><input data-table-date-to type="date"></label>');
      if (partyIndex >= 0 && parties.length > 0 && parties.length <= 100) {
        const select = document.createElement('select');
        select.dataset.tableParty = '1';
        select.innerHTML = '<option value="">Tutte</option>';
        parties.forEach((partyName) => {
          const option = document.createElement('option');
          option.value = partyName;
          option.textContent = partyName;
          select.appendChild(option);
        });
        const label = document.createElement('label');
        label.innerHTML = `<span>${headers[partyIndex]}</span>`;
        label.appendChild(select);
        fields.appendChild(label);
      }
      const localSearch = tools.querySelector('[data-table-search]');
      const dateFrom = tools.querySelector('[data-table-date-from]');
      const dateTo = tools.querySelector('[data-table-date-to]');
      const party = tools.querySelector('[data-table-party]');
      const [pdfButton, xlsButton, csvButton] = tools.querySelectorAll('button');
      const applyFilters = () => {
        const needle = localSearch.value.trim().toLocaleLowerCase('it');
        const fromTime = dateFrom?.value ? Date.parse(`${dateFrom.value}T00:00:00Z`) : null;
        const toTime = dateTo?.value ? Date.parse(`${dateTo.value}T23:59:59Z`) : null;
        [...table.tBodies].flatMap((body) => [...body.rows]).forEach((row) => {
          const textMatch = needle === '' || row.innerText.toLocaleLowerCase('it').includes(needle);
          const rowTime = dateIndex >= 0 ? parseTableDate(row.cells[dateIndex]?.innerText) : null;
          const dateMatch = (fromTime === null || (rowTime !== null && rowTime >= fromTime))
            && (toTime === null || (rowTime !== null && rowTime <= toTime));
          const rowParty = partyIndex >= 0 ? row.cells[partyIndex]?.innerText.replace(/\s+/g, ' ').trim() : '';
          const partyMatch = !party?.value || rowParty === party.value;
          row.dataset.filterMatch = textMatch && dateMatch && partyMatch ? '1' : '0';
        });
        table.dispatchEvent(new CustomEvent('tablefilter'));
      };
      [localSearch, dateFrom, dateTo, party].filter(Boolean).forEach((control) => {
        const eventName = control.tagName === 'INPUT' && control.type === 'search' ? 'input' : 'change';
        control.addEventListener(eventName, applyFilters);
      });
      pdfButton.addEventListener('click', () => {
        document.body.classList.add('print-table');
        printContainer?.classList.add('print-target');
        printContainer?.setAttribute('data-print-title', title);
        [...table.tBodies].flatMap((body) => [...body.rows]).forEach((row) => { row.hidden = row.dataset.filterMatch === '0'; });
        window.print();
        setTimeout(() => {
          document.body.classList.remove('print-table');
          printContainer?.classList.remove('print-target');
          printContainer?.removeAttribute('data-print-title');
          table.dispatchEvent(new CustomEvent('tablefilter'));
        }, 300);
      });
      xlsButton.addEventListener('click', () => exportVisibleTable(table, 'xls', title));
      csvButton.addEventListener('click', () => exportVisibleTable(table, 'csv', title));
      wrap?.insertAdjacentElement('beforebegin', tools);
      applyFilters();
  });

  const sortableValue = (cell) => {
    const value = (cell?.dataset.sortValue || cell?.innerText || '').replace(/\s+/g, ' ').trim();
    const italianDate = value.match(/^(\d{2})\/(\d{2})\/(\d{4})/);
    if (italianDate) return Date.UTC(Number(italianDate[3]), Number(italianDate[2]) - 1, Number(italianDate[1]));
    const numeric = value.replace(/[€%\s]/g, '').replace(/\./g, '').replace(',', '.');
    if (numeric !== '' && /^-?\d+(?:\.\d+)?$/.test(numeric)) return Number(numeric);
    return value.toLocaleLowerCase('it');
  };
  dataTables.filter((table) => !table.matches('.server-table,.selectable-table')).forEach((table) => {
    const body = table.tBodies[0];
    const rows = body ? [...body.rows].filter((row) => !row.querySelector('.table-empty')) : [];
    const headers = [...(table.tHead?.rows[0]?.cells || [])];
    if (!body || rows.length === 0 || headers.length === 0) return;
    let page = 1;
    let pageSize = 25;
    let sortIndex = -1;
    let sortDirection = 1;
    const pager = document.createElement('nav');
    pager.className = 'client-pagination';
    pager.setAttribute('aria-label', 'Paginazione tabella');
    pager.innerHTML = '<span data-client-page></span><div><button class="button ghost compact-button" type="button" data-client-prev>Precedente</button><button class="button ghost compact-button" type="button" data-client-next>Successiva</button></div><label>Righe <select><option>25</option><option>50</option><option>100</option><option>250</option></select></label>';
    table.closest('.table-wrap')?.insertAdjacentElement('afterend', pager);
    const render = () => {
      const filtered = rows.filter((row) => row.dataset.filterMatch !== '0');
      const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
      page = Math.min(page, pages);
      rows.forEach((row) => { row.hidden = true; });
      filtered.slice((page - 1) * pageSize, page * pageSize).forEach((row) => { row.hidden = false; });
      pager.querySelector('[data-client-page]').textContent = `Pagina ${page} di ${pages} · ${filtered.length} righe`;
      pager.querySelector('[data-client-prev]').disabled = page <= 1;
      pager.querySelector('[data-client-next]').disabled = page >= pages;
    };
    headers.forEach((header, index) => {
      if (header.classList.contains('actions-column') || header.classList.contains('selection-column') || header.textContent.trim() === '') return;
      header.classList.add('client-sortable-header');
      header.tabIndex = 0;
      header.setAttribute('role', 'button');
      header.setAttribute('aria-label', `Ordina per ${header.textContent.trim()}`);
      const sort = () => {
        if (sortIndex === index) sortDirection *= -1; else { sortIndex = index; sortDirection = 1; }
        rows.sort((a, b) => {
          const left = sortableValue(a.cells[index]);
          const right = sortableValue(b.cells[index]);
          return (typeof left === 'number' && typeof right === 'number' ? left - right : String(left).localeCompare(String(right), 'it', { numeric: true })) * sortDirection;
        }).forEach((row) => body.appendChild(row));
        headers.forEach((cell) => cell.removeAttribute('data-sort-direction'));
        header.dataset.sortDirection = sortDirection === 1 ? 'asc' : 'desc';
        page = 1;
        render();
      };
      header.addEventListener('click', sort);
      header.addEventListener('keydown', (event) => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); sort(); } });
    });
    pager.querySelector('[data-client-prev]').addEventListener('click', () => { page -= 1; render(); });
    pager.querySelector('[data-client-next]').addEventListener('click', () => { page += 1; render(); });
    pager.querySelector('select').addEventListener('change', (event) => { pageSize = Number(event.target.value); page = 1; render(); });
    table.addEventListener('tablefilter', () => { page = 1; render(); });
    render();
  });
})();
document.querySelectorAll('[data-party-form]').forEach((form) => {
    const button = form.querySelector('[data-vat-lookup]');
    const status = form.querySelector('[data-vat-lookup-status]');
    if (!button) return;
    button.addEventListener('click', async () => {
        const vat = form.querySelector('[name="vat_number"]');
        const country = form.querySelector('[name="country_code"]');
        if (!vat || !vat.value.trim()) { status.textContent = 'Inserisci prima la Partita IVA.'; vat?.focus(); return; }
        button.disabled = true; status.textContent = 'Verifica in corso…';
        try {
            const body = new FormData();
            body.append('_token', form.querySelector('[name="_token"]').value);
            body.append('vat_number', vat.value);
            body.append('country_code', country?.value || 'IT');
            const response = await fetch(form.action.replace('/save', '/vat-lookup'), {method: 'POST', body, headers: {'X-Requested-With': 'XMLHttpRequest'}});
            const payload = await response.json();
            if (!response.ok || !payload.ok) throw new Error(payload.message || 'Verifica non riuscita.');
            Object.entries(payload.data).forEach(([name, value]) => {
                const input = form.querySelector(`[name="${name}"]`);
                if (input && value !== '') input.value = value;
            });
            status.textContent = `Dati verificati (${payload.data.status}). Controllali prima di salvare.`;
        } catch (error) { status.textContent = error.message; }
        finally { button.disabled = false; }
    });
});

document.querySelectorAll('[data-document-form]').forEach((form) => {
    const party = form.querySelector('[data-counterparty]');
    const documentDate = form.querySelector('[name="document_date"]');
    const dueDate = form.querySelector('[name="due_date"]');
    const calculateDueDate = () => {
        const option = party?.selectedOptions?.[0];
        if (!option || !documentDate?.value || !dueDate) return;
        const base = new Date(`${documentDate.value}T12:00:00`);
        if (option.dataset.monthEnd === '1') base.setMonth(base.getMonth() + 1, 0);
        base.setDate(base.getDate() + Number(option.dataset.paymentDays || 30));
        dueDate.value = `${base.getFullYear()}-${String(base.getMonth()+1).padStart(2,'0')}-${String(base.getDate()).padStart(2,'0')}`;
        const method = form.querySelector('[name="payment_method_code"]');
        if (method && option.dataset.method) method.value = option.dataset.method;
        if (form.querySelector('[data-withholding-enabled]') && option.dataset.withholding) {
            form.querySelector('[data-withholding-enabled]').checked = option.dataset.withholding === '1';
            [['withholding_type','withholdingType'],['withholding_rate','withholdingRate'],['withholding_taxable_percent','withholdingTaxable'],['withholding_cause','withholdingCause']].forEach(([name,key]) => {
                const input = form.querySelector(`[name="${name}"]`); if (input && option.dataset[key]) input.value = option.dataset[key];
            });
        }
    };
    party?.addEventListener('change', calculateDueDate);
    documentDate?.addEventListener('change', calculateDueDate);
});

document.querySelectorAll('[data-withholding-form]').forEach((form) => {
    const item = form.querySelector('[data-withholding-item]');
    const refresh = () => {
        const option = item?.selectedOptions?.[0]; if (!option?.value) return;
        form.querySelector('[name="gross_amount"]').value = option.dataset.gross || '';
        form.querySelector('[name="rate_percent"]').value = option.dataset.rate || '20';
        form.querySelector('[name="taxable_percent"]').value = option.dataset.taxable || '100';
        form.querySelector('[name="withholding_type"]').value = ['IRPEF','INPS','ENASARCO','OTHER'].includes(option.dataset.type) ? option.dataset.type : 'IRPEF';
        const payment = new Date(`${form.querySelector('[name="record_date"]').value}T12:00:00`);
        const due = new Date(payment.getFullYear(), payment.getMonth() + 1, 16, 12);
        form.querySelector('[name="due_date"]').value = `${due.getFullYear()}-${String(due.getMonth()+1).padStart(2,'0')}-16`;
    };
    item?.addEventListener('change', refresh);
    form.querySelector('[name="record_date"]')?.addEventListener('change', refresh);
});

(() => {
    const dialog = document.querySelector('[data-account-change-dialog]');
    const catalogNode = document.querySelector('[data-account-catalog]');
    if (!(dialog instanceof HTMLDialogElement) || !catalogNode) return;

    let accounts = [];
    try { accounts = JSON.parse(catalogNode.textContent || '[]'); } catch (_error) { return; }
    const form = dialog.querySelector('[data-account-change-form]');
    const search = dialog.querySelector('[data-account-search]');
    const value = dialog.querySelector('[data-account-value]');
    const results = dialog.querySelector('[data-account-results]');
    const lineLabel = dialog.querySelector('[data-account-change-line]');
    const description = dialog.querySelector('[data-account-description]');
    let allowed = [];
    let activeIndex = -1;

    const normalize = (text) => String(text || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('it');
    const hideResults = () => {
        results.hidden = true;
        search.setAttribute('aria-expanded', 'false');
        activeIndex = -1;
    };
    const choose = (account) => {
        value.value = String(account.id);
        search.value = `${account.code} · ${account.name}`;
        search.setCustomValidity('');
        hideResults();
    };
    const render = () => {
        const query = normalize(search.value.trim());
        const matches = allowed.filter((account) => !query || normalize(`${account.code} ${account.name}`).includes(query)).slice(0, 40);
        results.replaceChildren();
        activeIndex = -1;
        if (matches.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'account-lookup-empty';
            empty.textContent = 'Nessun conto compatibile trovato.';
            results.appendChild(empty);
        } else {
            matches.forEach((account) => {
                const option = document.createElement('button');
                option.type = 'button';
                option.className = 'account-lookup-option';
                option.setAttribute('role', 'option');
                const code = document.createElement('strong');
                code.textContent = account.code;
                const name = document.createElement('small');
                name.textContent = account.name;
                option.append(code, name);
                option.addEventListener('click', () => choose(account));
                results.appendChild(option);
            });
        }
        results.hidden = false;
        search.setAttribute('aria-expanded', 'true');
    };
    const moveActive = (direction) => {
        const options = [...results.querySelectorAll('.account-lookup-option')];
        if (options.length === 0) return;
        activeIndex = (activeIndex + direction + options.length) % options.length;
        options.forEach((option, index) => option.classList.toggle('active', index === activeIndex));
        options[activeIndex].scrollIntoView({block: 'nearest'});
    };

    document.querySelectorAll('[data-open-account-change]').forEach((button) => {
        button.addEventListener('click', () => {
            form.action = button.dataset.action || '';
            lineLabel.textContent = button.dataset.lineLabel || '';
            const current = Number(button.dataset.currentAccount || 0);
            const allowedTypes = String(button.dataset.allowedTypes || '').split(',').filter(Boolean);
            allowed = accounts.filter((account) => allowedTypes.includes(account.type));
            form.reset();
            const currentAccount = accounts.find((account) => Number(account.id) === current);
            value.value = currentAccount ? String(currentAccount.id) : '';
            search.value = button.dataset.currentAccountLabel || (currentAccount ? `${currentAccount.code} · ${currentAccount.name}` : '');
            if (description) description.value = button.dataset.currentDescription || '';
            hideResults();
            dialog.showModal();
            window.setTimeout(() => search.focus(), 0);
        });
    });
    dialog.querySelectorAll('[data-close-account-change]').forEach((button) => button.addEventListener('click', () => dialog.close()));
    dialog.addEventListener('click', (event) => {
        if (event.target === dialog) dialog.close();
    });
    search.addEventListener('focus', render);
    search.addEventListener('input', () => {
        value.value = '';
        search.setCustomValidity('');
        render();
    });
    search.addEventListener('keydown', (event) => {
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            if (results.hidden) render();
            moveActive(event.key === 'ArrowDown' ? 1 : -1);
        } else if (event.key === 'Enter' && activeIndex >= 0) {
            event.preventDefault();
            results.querySelectorAll('.account-lookup-option')[activeIndex]?.click();
        } else if (event.key === 'Escape' && !results.hidden) {
            event.preventDefault();
            hideResults();
        }
    });
    form.addEventListener('submit', (event) => {
        if (value.value) return;
        event.preventDefault();
        search.setCustomValidity('Seleziona un conto dai risultati della ricerca.');
        search.reportValidity();
        search.focus();
        render();
    });
})();

(() => {
    const dialog = document.querySelector('[data-line-split-dialog]');
    const catalogNode = document.querySelector('[data-account-catalog]');
    if (!(dialog instanceof HTMLDialogElement) || !catalogNode) return;
    let accounts = [];
    try { accounts = JSON.parse(catalogNode.textContent || '[]'); } catch (_error) { return; }
    const form = dialog.querySelector('[data-line-split-form]');
    const rows = dialog.querySelector('[data-line-split-rows]');
    const list = dialog.querySelector('[data-split-account-options]');
    const label = dialog.querySelector('[data-line-split-label]');
    const totalNode = dialog.querySelector('[data-line-split-total]');
    const addButton = dialog.querySelector('[data-add-line-split]');
    const listId = `split-account-options-${Math.random().toString(36).slice(2)}`;
    list.id = listId;
    let allowed = [];
    let total = 0;
    let sequence = 0;
    const money = (value) => new Intl.NumberFormat('it-IT', {style: 'currency', currency: 'EUR'}).format(value);
    const accountLabel = (account) => `${account.code} · ${account.name}`;
    const matchAccount = (value) => allowed.find((account) => accountLabel(account) === value || account.code === value.trim());
    const addRow = (amount = '') => {
        const index = sequence++;
        const row = document.createElement('div');
        row.className = 'line-split-row';
        row.innerHTML = `<label><span>Conto</span><input type="search" class="split-account-search" list="${listId}" placeholder="Cerca codice o descrizione…" autocomplete="off" required><input type="hidden" name="allocations[${index}][account_id]"></label><label><span>Importo</span><input type="number" name="allocations[${index}][amount]" min="0.01" step="0.01" value="${amount}" required></label><label><span>Descrizione</span><input name="allocations[${index}][description]" maxlength="500" placeholder="Facoltativa"></label><button type="button" class="dialog-close split-remove" aria-label="Rimuovi">×</button>`;
        const search = row.querySelector('.split-account-search');
        const hidden = row.querySelector('input[type="hidden"]');
        search.addEventListener('input', () => { hidden.value = ''; search.setCustomValidity(''); });
        search.addEventListener('change', () => {
            const account = matchAccount(search.value);
            hidden.value = account ? String(account.id) : '';
            search.setCustomValidity(account ? '' : 'Seleziona un conto dall’elenco.');
        });
        row.querySelector('.split-remove').addEventListener('click', () => { if (rows.children.length > 2) row.remove(); });
        rows.appendChild(row);
    };
    document.querySelectorAll('[data-open-line-split]').forEach((button) => button.addEventListener('click', () => {
        form.action = button.dataset.action || '';
        label.textContent = button.dataset.lineLabel || '';
        total = Number(button.dataset.lineAmount || 0);
        totalNode.textContent = money(total);
        const allowedTypes = String(button.dataset.allowedTypes || '').split(',').filter(Boolean);
        allowed = accounts.filter((account) => allowedTypes.includes(account.type));
        list.replaceChildren(...allowed.map((account) => {
            const option = document.createElement('option'); option.value = accountLabel(account); return option;
        }));
        rows.replaceChildren(); sequence = 0;
        addRow(total ? (total / 2).toFixed(2) : '');
        addRow(total ? (total - Number((total / 2).toFixed(2))).toFixed(2) : '');
        dialog.showModal();
    }));
    addButton.addEventListener('click', () => addRow(''));
    dialog.querySelectorAll('[data-close-line-split]').forEach((button) => button.addEventListener('click', () => dialog.close()));
    dialog.addEventListener('click', (event) => { if (event.target === dialog) dialog.close(); });
    form.addEventListener('submit', (event) => {
        let valid = true;
        rows.querySelectorAll('.line-split-row').forEach((row) => {
            const search = row.querySelector('.split-account-search');
            const hidden = row.querySelector('input[type="hidden"]');
            const account = matchAccount(search.value);
            hidden.value = account ? String(account.id) : '';
            search.setCustomValidity(account ? '' : 'Seleziona un conto dall’elenco.');
            if (!account) valid = false;
        });
        if (!valid) { event.preventDefault(); form.reportValidity(); }
    });
})();
