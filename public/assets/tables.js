(() => {
  'use strict';

  const PAGE_SIZES = [25, 50, 100, 250];

  const sortableValue = (cell) => {
    const text = cell?.innerText.replace(/\s+/g, ' ').trim() || '';
    const italianDate = text.match(/^(\d{2})\/(\d{2})\/(\d{4})/);
    if (italianDate) {
      return { numeric: true, value: Date.UTC(Number(italianDate[3]), Number(italianDate[2]) - 1, Number(italianDate[1])) };
    }
    const isoDate = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (isoDate) {
      return { numeric: true, value: Date.UTC(Number(isoDate[1]), Number(isoDate[2]) - 1, Number(isoDate[3])) };
    }
    const number = text.replace(/[€%\s]/g, '').replace(/\./g, '').replace(',', '.');
    if (number !== '' && /^-?\d+(?:\.\d+)?$/.test(number)) {
      return { numeric: true, value: Number(number) };
    }
    return { numeric: false, value: text.toLocaleLowerCase('it') };
  };

  const eligible = (table) => !(
    table.dataset.lunaTable === 'ready'
    || table.matches('.line-table,.selectable-table,[data-no-table-controls]')
    || table.closest('form')
    || !table.tHead
    || table.tBodies.length === 0
  );

  const enhance = (table) => {
    if (!eligible(table)) return;

    const rows = [...table.tBodies]
      .flatMap((body) => [...body.rows])
      .filter((row) => !row.querySelector('.table-empty') && row.cells.length > 1);
    if (rows.length === 0) return;

    table.dataset.lunaTable = 'ready';
    rows.forEach((row, index) => {
      row.dataset.lunaOriginalIndex = String(index);
      row.dataset.lunaFilterMatch = '1';
    });

    let page = 1;
    let perPage = PAGE_SIZES[0];
    let sortColumn = -1;
    let sortDirection = 'asc';

    const wrap = table.closest('.table-wrap');
    const pagination = document.createElement('nav');
    pagination.className = 'luna-table-pagination';
    pagination.setAttribute('aria-label', 'Paginazione tabella');
    pagination.innerHTML = `
      <span data-luna-summary></span>
      <div>
        <button type="button" data-luna-previous aria-label="Pagina precedente">‹</button>
        <span data-luna-page></span>
        <button type="button" data-luna-next aria-label="Pagina successiva">›</button>
      </div>
      <label>Righe
        <select data-luna-size>${PAGE_SIZES.map((size) => `<option value="${size}">${size}</option>`).join('')}</select>
      </label>`;
    wrap.insertAdjacentElement('afterend', pagination);

    const summary = pagination.querySelector('[data-luna-summary]');
    const pageLabel = pagination.querySelector('[data-luna-page]');
    const previous = pagination.querySelector('[data-luna-previous]');
    const next = pagination.querySelector('[data-luna-next]');
    const size = pagination.querySelector('[data-luna-size]');

    const render = () => {
      const matching = rows.filter((row) => row.dataset.lunaFilterMatch !== '0');
      const pageCount = Math.max(1, Math.ceil(matching.length / perPage));
      page = Math.min(Math.max(1, page), pageCount);
      const first = (page - 1) * perPage;
      const currentRows = new Set(matching.slice(first, first + perPage));

      rows.forEach((row) => {
        row.hidden = !currentRows.has(row);
      });
      summary.textContent = `${matching.length} righe`;
      pageLabel.textContent = `${page} / ${pageCount}`;
      previous.disabled = page === 1;
      next.disabled = page === pageCount;
      pagination.hidden = matching.length <= perPage;
    };

    [...table.tHead.rows[0].cells].forEach((header, index) => {
      if (header.classList.contains('actions-column')
        || header.classList.contains('selection-column')
        || header.textContent.trim() === ''
        || header.querySelector('a,button,input,select')) {
        return;
      }

      header.classList.add('luna-sortable');
      header.tabIndex = 0;
      header.setAttribute('role', 'button');
      header.setAttribute('aria-sort', 'none');

      const sort = () => {
        sortDirection = sortColumn === index && sortDirection === 'asc' ? 'desc' : 'asc';
        sortColumn = index;
        [...table.tHead.rows[0].cells].forEach((cell) => {
          cell.setAttribute('aria-sort', cell === header ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none');
        });
        rows.sort((left, right) => {
          const a = sortableValue(left.cells[index]);
          const b = sortableValue(right.cells[index]);
          const comparison = a.numeric && b.numeric
            ? a.value - b.value
            : String(a.value).localeCompare(String(b.value), 'it', { numeric: true, sensitivity: 'base' });
          const stableComparison = comparison || Number(left.dataset.lunaOriginalIndex) - Number(right.dataset.lunaOriginalIndex);
          return sortDirection === 'asc' ? stableComparison : -stableComparison;
        });
        rows.forEach((row) => row.parentElement.appendChild(row));
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

    const localSearch = table.closest('.card')?.querySelector('.table-export-tools input[type="search"]');
    localSearch?.addEventListener('input', () => {
      const query = localSearch.value.trim().toLocaleLowerCase('it');
      rows.forEach((row) => {
        row.dataset.lunaFilterMatch = query === '' || row.innerText.toLocaleLowerCase('it').includes(query) ? '1' : '0';
      });
      page = 1;
      render();
    });

    previous.addEventListener('click', () => {
      page -= 1;
      render();
    });
    next.addEventListener('click', () => {
      page += 1;
      render();
    });
    size.addEventListener('change', () => {
      const selected = Number(size.value);
      perPage = PAGE_SIZES.includes(selected) ? selected : PAGE_SIZES[0];
      page = 1;
      render();
    });

    render();
  };

  document.querySelectorAll('.table-wrap > table').forEach(enhance);
})();
