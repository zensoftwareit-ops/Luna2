<?php
use Luna\Core\Csrf;
use Luna\Core\View;

$token = View::e(Csrf::token());
$currentQuery = [
    'q' => $search,
    'filters' => array_filter($filters, static fn ($value): bool => $value !== '' && $value !== null),
    'date_from' => array_filter($dateFrom, static fn ($value): bool => $value !== '' && $value !== null),
    'date_to' => array_filter($dateTo, static fn ($value): bool => $value !== '' && $value !== null),
    'sort' => $sort,
    'direction' => $direction,
    'per_page' => $perPage,
];
$url = static function (array $changes = []) use ($slug, $currentQuery): string {
    $query = array_replace_recursive($currentQuery, $changes);
    return '/r/' . rawurlencode($slug) . '?' . http_build_query($query);
};
$filterCount = count(array_filter($filters, static fn ($value): bool => $value !== '' && $value !== null))
    + count(array_filter($dateFrom, static fn ($value): bool => $value !== '' && $value !== null))
    + count(array_filter($dateTo, static fn ($value): bool => $value !== '' && $value !== null));
$exportQuery = http_build_query(array_filter([
    'q' => $search,
    'filters' => $currentQuery['filters'],
    'date_from' => $currentQuery['date_from'],
    'date_to' => $currentQuery['date_to'],
], static fn ($value): bool => $value !== '' && $value !== []));
?>
<section class="page-intro compact">
    <div>
        <span class="eyebrow"><?= View::e($module['group']) ?></span>
        <h1><?= View::e($module['title']) ?></h1>
        <p><?= number_format($total, 0, ',', '.') ?> record<?= $search !== '' || $filterCount ? ' corrispondenti ai filtri' : ' disponibili' ?>.</p>
    </div>
    <a class="button primary" href="/r/<?= View::e($slug) ?>/create"><?= View::icon('plus') ?> Nuovo <?= View::e(mb_strtolower($module['singular'])) ?></a>
</section>

<?php if ($savedViews): ?>
    <nav class="saved-view-bar" aria-label="Viste salvate">
        <span>Viste</span>
        <?php foreach ($savedViews as $saved): ?>
            <a class="saved-view-chip<?= !empty($saved['is_default']) ? ' default' : '' ?>" href="/r/<?= View::e($slug) ?>?<?= View::e(http_build_query($saved['query'])) ?>">
                <?= View::e($saved['name']) ?><?= !empty($saved['is_default']) ? '<small>predefinita</small>' : '' ?>
            </a>
            <form method="post" action="/workspace/views/<?= (int) $saved['id'] ?>/delete" class="saved-view-delete" data-confirm="Eliminare la vista salvata?">
                <input type="hidden" name="_token" value="<?= $token ?>">
                <input type="hidden" name="redirect" value="<?= View::e($_SERVER['REQUEST_URI'] ?? '/r/' . $slug) ?>">
                <button type="submit" aria-label="Elimina vista <?= View::e($saved['name']) ?>">×</button>
            </form>
        <?php endforeach; ?>
    </nav>
<?php endif; ?>

<section class="list-toolbar professional-toolbar">
    <form method="get" class="search-form grow">
        <span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Cerca in <?= View::e(mb_strtolower($module['title'])) ?>…" autocomplete="off"></span>
        <?php foreach ($filters as $field => $value): if ($value === '' || $value === null) continue; ?>
            <input type="hidden" name="filters[<?= View::e($field) ?>]" value="<?= View::e($value) ?>">
        <?php endforeach; ?>
        <?php foreach ($dateFrom as $field => $value): if ($value === '' || $value === null) continue; ?><input type="hidden" name="date_from[<?= View::e($field) ?>]" value="<?= View::e($value) ?>"><?php endforeach; ?>
        <?php foreach ($dateTo as $field => $value): if ($value === '' || $value === null) continue; ?><input type="hidden" name="date_to[<?= View::e($field) ?>]" value="<?= View::e($value) ?>"><?php endforeach; ?>
        <input type="hidden" name="sort" value="<?= View::e($sort) ?>">
        <input type="hidden" name="direction" value="<?= View::e($direction) ?>">
        <button class="button" type="submit">Cerca</button>
    </form>
    <?php if ($filterFields): ?>
        <button class="button ghost<?= $filterCount ? ' has-filter' : '' ?>" type="button" data-filter-toggle>
            <?= View::icon('settings') ?> Filtri<?= $filterCount ? ' (' . $filterCount . ')' : '' ?>
        </button>
    <?php endif; ?>
    <div class="export-actions" aria-label="Esporta elenco">
        <a class="button ghost" href="/r/<?= View::e($slug) ?>/export/pdf?<?= View::e($exportQuery) ?>">PDF</a>
        <a class="button ghost" href="/r/<?= View::e($slug) ?>/export/xlsx?<?= View::e($exportQuery) ?>">XLSX</a>
        <a class="button ghost" href="/r/<?= View::e($slug) ?>/export/csv?<?= View::e($exportQuery) ?>">CSV</a>
    </div>
</section>

<?php if ($filterFields): ?>
    <section class="filter-panel<?= $filterCount ? ' open' : '' ?>" data-filter-panel>
        <form method="get" class="filter-grid">
            <input type="hidden" name="q" value="<?= View::e($search) ?>">
            <?php foreach ($filterFields as $field => $settings): $value = $filters[$field] ?? ''; ?>
                <?php if (($settings['type'] ?? '') === 'date'): ?>
                    <label class="field"><span><?= View::e($settings['label']) ?> dal</span><input type="date" name="date_from[<?= View::e($field) ?>]" value="<?= View::e($dateFrom[$field] ?? '') ?>"></label>
                    <label class="field"><span><?= View::e($settings['label']) ?> al</span><input type="date" name="date_to[<?= View::e($field) ?>]" value="<?= View::e($dateTo[$field] ?? '') ?>"></label>
                    <?php continue; ?>
                <?php endif; ?>
                <label class="field">
                    <span><?= View::e($settings['label']) ?></span>
                    <?php if (in_array(($settings['type'] ?? ''), ['select', 'relation'], true)): ?>
                        <select name="filters[<?= View::e($field) ?>]">
                            <option value="">Qualsiasi</option>
                            <?php foreach ($settings['options'] ?? [] as $key => $label): ?>
                                <option value="<?= View::e($key) ?>" <?= (string) $value === (string) $key ? 'selected' : '' ?>><?= View::e($label) ?></option>
                            <?php endforeach; ?>
                        </select>
                    <?php elseif (($settings['type'] ?? '') === 'checkbox'): ?>
                        <select name="filters[<?= View::e($field) ?>]"><option value="">Qualsiasi</option><option value="1" <?= (string) $value === '1' ? 'selected' : '' ?>>Sì</option><option value="0" <?= (string) $value === '0' ? 'selected' : '' ?>>No</option></select>
                    <?php endif; ?>
                </label>
            <?php endforeach; ?>
            <div class="filter-actions"><a class="button ghost" href="/r/<?= View::e($slug) ?>">Azzera</a><button class="button primary" type="submit">Applica filtri</button></div>
        </form>
    </section>
<?php endif; ?>

<div class="collection-actions">
    <form id="bulk-form" method="post" action="/r/<?= View::e($slug) ?>/bulk" class="bulk-form">
        <input type="hidden" name="_token" value="<?= $token ?>">
        <span data-selection-count>0 selezionati</span>
        <select name="operation" aria-label="Operazione sulla selezione" required>
            <option value="">Operazione massiva…</option>
            <option value="export">Esporta selezione</option>
            <?php if (isset($module['fields']['active'])): ?><option value="activate">Attiva</option><option value="deactivate">Disattiva</option><?php endif; ?>
        </select>
        <button class="button compact-button" type="submit" disabled data-bulk-submit>Applica</button>
    </form>
    <details class="save-view-control">
        <summary class="button ghost">Salva vista</summary>
        <form method="post" action="/workspace/views">
            <input type="hidden" name="_token" value="<?= $token ?>">
            <input type="hidden" name="module_key" value="<?= View::e($slug) ?>">
            <input type="hidden" name="query_json" value="<?= View::e(json_encode($currentQuery, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)) ?>">
            <input type="hidden" name="redirect" value="<?= View::e($_SERVER['REQUEST_URI'] ?? '/r/' . $slug) ?>">
            <label class="field"><span>Nome della vista</span><input name="name" maxlength="100" required></label>
            <label class="plain-check"><input type="checkbox" name="is_default" value="1"> Imposta come predefinita</label>
            <button class="button primary compact-button" type="submit">Salva</button>
        </form>
    </details>
</div>

<section class="card data-card">
    <div class="table-wrap">
        <table class="selectable-table">
            <thead>
            <tr>
                <th class="selection-column"><input type="checkbox" data-select-all aria-label="Seleziona tutti i record visibili"></th>
                <?php foreach ($module['columns'] as $column): ?>
                    <?php $nextDirection = $sort === $column && $direction === 'ASC' ? 'DESC' : 'ASC'; ?>
                    <th>
                        <a class="sort-link<?= $sort === $column ? ' active' : '' ?>" href="<?= View::e($url(['sort' => $column, 'direction' => $nextDirection, 'page' => 1])) ?>">
                            <?= View::e($module['fields'][$column]['label'] ?? ucfirst(str_replace('_', ' ', $column))) ?>
                            <?php if ($sort === $column): ?><span aria-hidden="true"><?= $direction === 'ASC' ? '↑' : '↓' ?></span><?php endif; ?>
                        </a>
                    </th>
                <?php endforeach; ?>
                <th class="actions-column">Azioni</th>
            </tr>
            </thead>
            <tbody>
            <?php foreach ($rows as $row): ?>
                <tr>
                    <td class="selection-column"><input form="bulk-form" type="checkbox" name="ids[]" value="<?= (int) $row['id'] ?>" data-row-select aria-label="Seleziona record <?= (int) $row['id'] ?>"></td>
                    <?php foreach ($module['columns'] as $index => $column): ?>
                        <?php $field = $module['fields'][$column] ?? []; $value = $row[$column] ?? null; ?>
                        <td class="<?= $index === 0 ? 'primary-cell' : '' ?>" data-label="<?= View::e($field['label'] ?? ucfirst(str_replace('_', ' ', $column))) ?>">
                            <?php if (($field['type'] ?? '') === 'checkbox'): ?><span class="badge <?= $value ? 'status-active' : 'status-muted' ?>"><?= $value ? 'Attivo' : 'No' ?></span>
                            <?php elseif (($field['type'] ?? '') === 'decimal'): ?><?= View::money($value) ?>
                            <?php elseif (($field['type'] ?? '') === 'date'): ?><?= View::date($value) ?>
                            <?php else: ?><?= View::e($value ?? '—') ?><?php endif; ?>
                        </td>
                    <?php endforeach; ?>
                    <td class="row-actions">
                        <a class="table-action" href="/r/<?= View::e($slug) ?>/<?= (int) $row['id'] ?>/edit">Modifica</a>
                        <form method="post" action="/r/<?= View::e($slug) ?>/<?= (int) $row['id'] ?>/delete" data-confirm="Eliminare definitivamente il record?">
                            <input type="hidden" name="_token" value="<?= $token ?>"><button class="table-action danger-text" type="submit">Elimina</button>
                        </form>
                    </td>
                </tr>
            <?php endforeach; ?>
            <?php if (!$rows): ?><tr><td colspan="<?= count($module['columns']) + 2 ?>"><div class="table-empty"><span><?= View::icon('search') ?></span><strong>Nessun risultato</strong><small>Modifica i filtri o inserisci il primo record.</small></div></td></tr><?php endif; ?>
            </tbody>
        </table>
    </div>
</section>

<?php if ($pages > 1 || $total > 25): ?>
    <nav class="pagination" aria-label="Paginazione">
        <span>Pagina <?= $page ?> di <?= $pages ?></span>
        <div>
            <a class="button ghost compact-button<?= $page <= 1 ? ' disabled' : '' ?>" <?= $page > 1 ? 'href="' . View::e($url(['page' => $page - 1])) . '"' : 'aria-disabled="true"' ?>>Precedente</a>
            <a class="button ghost compact-button<?= $page >= $pages ? ' disabled' : '' ?>" <?= $page < $pages ? 'href="' . View::e($url(['page' => $page + 1])) . '"' : 'aria-disabled="true"' ?>>Successiva</a>
        </div>
        <form method="get">
            <?php foreach ($currentQuery as $key => $value): if ($key === 'per_page' || is_array($value)) continue; ?><input type="hidden" name="<?= View::e($key) ?>" value="<?= View::e($value) ?>"><?php endforeach; ?>
            <?php foreach ($currentQuery['filters'] ?? [] as $key => $value): ?><input type="hidden" name="filters[<?= View::e($key) ?>]" value="<?= View::e($value) ?>"><?php endforeach; ?>
            <?php foreach ($currentQuery['date_from'] ?? [] as $key => $value): ?><input type="hidden" name="date_from[<?= View::e($key) ?>]" value="<?= View::e($value) ?>"><?php endforeach; ?>
            <?php foreach ($currentQuery['date_to'] ?? [] as $key => $value): ?><input type="hidden" name="date_to[<?= View::e($key) ?>]" value="<?= View::e($value) ?>"><?php endforeach; ?>
            <label>Righe <select name="per_page" onchange="this.form.submit()"><?php foreach ([25, 50, 100, 250] as $size): ?><option value="<?= $size ?>" <?= $perPage === $size ? 'selected' : '' ?>><?= $size ?></option><?php endforeach; ?></select></label>
        </form>
    </nav>
<?php endif; ?>
