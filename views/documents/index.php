<?php
use Luna\Core\View;
$query = http_build_query(array_filter(
    ['q' => $search, 'from' => $from, 'to' => $to, 'counterparty_id' => $counterpartyId, 'status' => $status],
    static fn ($value): bool => $value !== '' && $value !== 0,
));
?>
<section class="page-intro compact">
    <div><span class="eyebrow">Documenti</span><h1><?= View::e($definition['title']) ?></h1><p><?= count($documents) ?> documenti corrispondenti ai filtri</p></div>
    <a class="button primary" href="/documents/<?= View::e($type) ?>/create"><?= View::icon('plus') ?> Nuovo <?= View::e(mb_strtolower($definition['singular'])) ?></a>
</section>
<section class="list-toolbar exportable-toolbar">
    <form method="get" class="search-form filter-form">
        <span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Numero, controparte o oggetto…" autocomplete="off"></span>
        <label>Dal <input type="date" name="from" value="<?= View::e($from) ?>"></label>
        <label>Al <input type="date" name="to" value="<?= View::e($to) ?>"></label>
        <label><?= $definition['counterparty'] === 'customer' ? 'Cliente' : 'Fornitore' ?>
            <select name="counterparty_id"><option value="">Tutti</option><?php foreach ($counterparties as $party): ?><option value="<?= (int) $party['id'] ?>" <?= $counterpartyId === (int) $party['id'] ? 'selected' : '' ?>><?= View::e(trim((string) $party['code'] . ' · ' . (string) $party['business_name'], ' ·')) ?></option><?php endforeach; ?></select>
        </label>
        <label>Stato
            <select name="status"><option value="">Tutti</option><?php foreach (['DRAFT','SENT','ACCEPTED','REJECTED','CONFIRMED','IN_PROGRESS','FULFILLED','ISSUED','RECEIVED','PARTIALLY_PAID','PAID','OVERDUE','CANCELLED'] as $value): ?><option value="<?= $value ?>" <?= $status === $value ? 'selected' : '' ?>><?= $value ?></option><?php endforeach; ?></select>
        </label>
        <button class="button">Filtra</button><a class="button ghost" href="/documents/<?= View::e($type) ?>">Azzera</a>
    </form>
    <div class="export-actions" aria-label="Esporta documenti">
        <a class="button ghost" href="/documents/<?= View::e($type) ?>/export/pdf?<?= View::e($query) ?>">PDF</a>
        <a class="button ghost" href="/documents/<?= View::e($type) ?>/export/xlsx?<?= View::e($query) ?>">XLSX</a>
        <a class="button ghost" href="/documents/<?= View::e($type) ?>/export/csv?<?= View::e($query) ?>">CSV</a>
    </div>
</section>
<section class="card data-card"><div class="table-wrap">
<table><thead><tr><th>Documento</th><th>Controparte</th><th>Oggetto</th><th>Data</th><th>Scadenza</th><th class="numeric">Imponibile</th><th class="numeric">IVA</th><th class="numeric">Totale</th><th class="numeric">Residuo</th><th>Stato</th></tr></thead><tbody>
<?php foreach ($documents as $document): ?><tr class="clickable-row" data-href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><td class="primary-cell"><a href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><?= View::e($document['number']) ?></a></td><td><?= View::e($document['counterparty_name']) ?></td><td><?= View::e($document['subject'] ?: '—') ?></td><td><?= View::date($document['document_date']) ?></td><td><?= View::date($document['due_date']) ?></td><td class="numeric"><?= View::money($document['taxable_total']) ?></td><td class="numeric"><?= View::money($document['vat_total']) ?></td><td class="numeric"><strong><?= View::money($document['total']) ?></strong></td><td class="numeric"><?= View::money($document['balance_due']) ?></td><td><span class="badge status-<?= View::e(strtolower($document['status'])) ?>"><?= View::e($document['status']) ?></span></td></tr><?php endforeach; ?>
<?php if (!$documents): ?><tr><td colspan="10"><div class="table-empty"><span><?= View::icon('receipt') ?></span><strong>Nessun documento</strong><small>Modifica i filtri o crea il primo documento.</small></div></td></tr><?php endif; ?>
</tbody></table></div></section>
