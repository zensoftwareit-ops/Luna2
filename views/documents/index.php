<?php
use Luna\Core\View;

$currentQuery = array_filter([
    'q' => $search, 'from' => $from, 'to' => $to, 'counterparty_id' => $counterpartyId,
    'status' => $status, 'sort' => $sort, 'direction' => $direction, 'per_page' => $perPage,
], static fn ($value): bool => $value !== '' && $value !== 0);
$url = static fn (array $changes = []): string => '/documents/' . rawurlencode($type) . '?' . http_build_query(array_replace($currentQuery, $changes));
$exportQuery = http_build_query(array_diff_key($currentQuery, ['per_page' => true]));
$headers = [
    'number' => 'Documento', 'counterparty_name' => 'Controparte', 'subject' => 'Oggetto',
    'document_date' => 'Data', 'due_date' => 'Scadenza', 'taxable_total' => 'Imponibile',
    'vat_total' => 'IVA', 'total' => 'Totale', 'balance_due' => 'Residuo', 'status' => 'Stato',
];
?>
<section class="page-intro compact">
    <div><span class="eyebrow">Documenti</span><h1><?= View::e($definition['title']) ?></h1><p><?= number_format($total, 0, ',', '.') ?> documenti corrispondenti ai filtri</p></div>
    <a class="button primary" href="/documents/<?= View::e($type) ?>/create"><?= View::icon('plus') ?> Nuovo <?= View::e(mb_strtolower($definition['singular'])) ?></a>
</section>

<section class="filter-toolbar-card">
    <div class="filter-toolbar-head"><div><span class="section-kicker">Ricerca avanzata</span><strong>Filtra <?= View::e(mb_strtolower($definition['title'])) ?></strong></div><div class="export-actions" aria-label="Esporta documenti"><a class="button ghost" href="/documents/<?= View::e($type) ?>/export/pdf?<?= View::e($exportQuery) ?>">PDF</a><a class="button ghost" href="/documents/<?= View::e($type) ?>/export/xlsx?<?= View::e($exportQuery) ?>">XLSX</a><a class="button ghost" href="/documents/<?= View::e($type) ?>/export/csv?<?= View::e($exportQuery) ?>">CSV</a></div></div>
    <form method="get" class="document-filter-grid">
        <label class="field filter-search-field"><span>Testo</span><span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Numero, controparte o oggetto…" autocomplete="off"></span></label>
        <label class="field"><span>Dal</span><input type="date" name="from" value="<?= View::e($from) ?>"></label>
        <label class="field"><span>Al</span><input type="date" name="to" value="<?= View::e($to) ?>"></label>
        <label class="field"><span><?= $definition['counterparty'] === 'customer' ? 'Cliente' : 'Fornitore' ?></span><select name="counterparty_id"><option value="">Tutti</option><?php foreach ($counterparties as $party): ?><option value="<?= (int) $party['id'] ?>" <?= $counterpartyId === (int) $party['id'] ? 'selected' : '' ?>><?= View::e(trim((string) $party['code'] . ' · ' . (string) $party['business_name'], ' ·')) ?></option><?php endforeach; ?></select></label>
        <label class="field"><span>Stato</span><select name="status"><option value="">Tutti</option><?php foreach (['DRAFT','SENT','ACCEPTED','REJECTED','CONFIRMED','IN_PROGRESS','FULFILLED','ISSUED','RECEIVED','PARTIALLY_PAID','PAID','OVERDUE','CANCELLED'] as $value): ?><option value="<?= $value ?>" <?= $status === $value ? 'selected' : '' ?>><?= $value ?></option><?php endforeach; ?></select></label>
        <div class="filter-submit-actions"><button class="button primary">Applica</button><a class="button ghost" href="/documents/<?= View::e($type) ?>">Azzera</a></div>
    </form>
</section>

<section class="card data-card">
    <div class="table-wrap"><table class="server-table"><thead><tr>
        <?php foreach ($headers as $column => $label): $nextDirection = $sort === $column && $direction === 'ASC' ? 'DESC' : 'ASC'; ?>
            <th class="<?= in_array($column, ['taxable_total','vat_total','total','balance_due'], true) ? 'numeric' : '' ?>"><a class="sort-link<?= $sort === $column ? ' active' : '' ?>" href="<?= View::e($url(['sort' => $column, 'direction' => $nextDirection, 'page' => 1])) ?>"><?= View::e($label) ?><?php if ($sort === $column): ?><span aria-hidden="true"><?= $direction === 'ASC' ? '↑' : '↓' ?></span><?php endif; ?></a></th>
        <?php endforeach; ?>
    </tr></thead><tbody>
    <?php foreach ($documents as $document): ?><tr class="clickable-row" data-href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><td class="primary-cell"><a href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><?= View::e($document['number']) ?></a></td><td><?= View::e($document['counterparty_name']) ?></td><td><?= View::e($document['subject'] ?: '—') ?></td><td><?= View::date($document['document_date']) ?></td><td><?= View::date($document['due_date']) ?></td><td class="numeric"><?= View::money($document['taxable_total']) ?></td><td class="numeric"><?= View::money($document['vat_total']) ?></td><td class="numeric"><strong><?= View::money($document['total']) ?></strong></td><td class="numeric"><?= View::money($document['balance_due']) ?></td><td><span class="badge status-<?= View::e(strtolower($document['status'])) ?>"><?= View::e($document['status']) ?></span></td></tr><?php endforeach; ?>
    <?php if (!$documents): ?><tr><td colspan="10"><div class="table-empty"><span><?= View::icon('receipt') ?></span><strong>Nessun documento</strong><small>Modifica i filtri o crea il primo documento.</small></div></td></tr><?php endif; ?>
    </tbody></table></div>
</section>

<?php if ($pages > 1 || $total > 25): ?>
<nav class="pagination" aria-label="Paginazione documenti">
    <span>Pagina <?= $page ?> di <?= $pages ?> · <?= number_format($total, 0, ',', '.') ?> record</span>
    <div><a class="button ghost compact-button<?= $page <= 1 ? ' disabled' : '' ?>" <?= $page > 1 ? 'href="' . View::e($url(['page' => $page - 1])) . '"' : 'aria-disabled="true"' ?>>Precedente</a><a class="button ghost compact-button<?= $page >= $pages ? ' disabled' : '' ?>" <?= $page < $pages ? 'href="' . View::e($url(['page' => $page + 1])) . '"' : 'aria-disabled="true"' ?>>Successiva</a></div>
    <form method="get"><?php foreach ($currentQuery as $key => $value): if ($key === 'per_page') continue; ?><input type="hidden" name="<?= View::e($key) ?>" value="<?= View::e($value) ?>"><?php endforeach; ?><label>Righe <select name="per_page" onchange="this.form.submit()"><?php foreach ([25,50,100,250] as $size): ?><option value="<?= $size ?>" <?= $perPage === $size ? 'selected' : '' ?>><?= $size ?></option><?php endforeach; ?></select></label></form>
</nav>
<?php endif; ?>
