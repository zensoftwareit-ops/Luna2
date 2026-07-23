<?php use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Contabilità generale</span><h1>Prima nota</h1><p>Scritture manuali e automatiche, bozze, protocolli e quadratura Dare/Avere.</p></div>
    <a class="button primary" href="/accounting/journal/create"><?= View::icon('plus') ?> Nuova registrazione</a>
</section>

<section class="accounting-kpis">
    <article><span>Movimenti</span><strong><?= count($entries) ?></strong></article>
    <article><span>Totale Dare</span><strong><?= View::money($totals['debit']) ?></strong></article>
    <article><span>Totale Avere</span><strong><?= View::money($totals['credit']) ?></strong></article>
    <article class="<?= $totals['drafts'] ? 'attention' : '' ?>"><span>Bozze da completare</span><strong><?= (int) $totals['drafts'] ?></strong></article>
</section>

<?php $exportQuery = http_build_query(array_filter(['q'=>$search,'from'=>$from,'to'=>$to,'status'=>$status,'type'=>$type,'account_id'=>$accountId])); ?>
<section class="list-toolbar accounting-toolbar exportable-toolbar">
    <form method="get" class="search-form filter-form">
        <span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Protocollo, descrizione, documento…"></span>
        <label>Dal <input type="date" name="from" value="<?= View::e($from) ?>"></label>
        <label>Al <input type="date" name="to" value="<?= View::e($to) ?>"></label>
        <select name="status"><option value="">Tutti gli stati</option><option value="DRAFT" <?= $status === 'DRAFT' ? 'selected' : '' ?>>Bozze</option><option value="POSTED" <?= $status === 'POSTED' ? 'selected' : '' ?>>Contabilizzate</option><option value="REVERSED" <?= $status === 'REVERSED' ? 'selected' : '' ?>>Stornate</option></select>
        <select name="account_id"><option value="">Tutti i conti</option><?php foreach ($accounts as $account): ?><option value="<?= (int) $account['id'] ?>" <?= $accountId === (int) $account['id'] ? 'selected' : '' ?>><?= View::e($account['code'] . ' · ' . $account['name']) ?></option><?php endforeach; ?></select>
        <button class="button" type="submit">Filtra</button>
    </form>
    <div class="export-actions"><a class="button ghost" href="/accounting/journal/export/pdf?<?= View::e($exportQuery) ?>">PDF</a><a class="button ghost" href="/accounting/journal/export/xlsx?<?= View::e($exportQuery) ?>">XLSX</a><a class="button ghost" href="/accounting/journal/export/csv?<?= View::e($exportQuery) ?>">CSV</a></div>
</section>

<section class="card data-card">
    <div class="table-wrap"><table><thead><tr><th>Data</th><th>Protocollo</th><th>Tipo</th><th>Descrizione</th><th>Documento / controparte</th><th>Dare</th><th>Avere</th><th>Stato</th><th class="actions-column">Azioni</th></tr></thead><tbody>
    <?php foreach ($entries as $entry): ?>
        <tr>
            <td><?= View::date($entry['entry_date']) ?><small class="cell-subtitle">Comp. <?= View::date($entry['competence_date']) ?></small></td>
            <td class="primary-cell"><a href="/accounting/journal/<?= (int) $entry['id'] ?>"><?= View::e($entry['protocol_number']) ?></a><small><?= View::e($entry['source_type']) ?></small></td>
            <td><?= View::e($entry['entry_type']) ?></td><td><?= View::e($entry['description']) ?></td>
            <td><?= View::e($entry['document_number'] ?: '—') ?><small class="cell-subtitle"><?= View::e($entry['counterparty'] ?: '') ?></small></td>
            <td><?= View::money($entry['total_debit']) ?></td><td><?= View::money($entry['total_credit']) ?></td>
            <td><span class="badge status-<?= strtolower((string) $entry['status']) ?>"><?= match ($entry['status']) { 'DRAFT' => 'Bozza', 'POSTED' => 'Contabilizzata', 'REVERSED' => 'Stornata', default => View::e($entry['status']) } ?></span></td>
            <td class="row-actions"><a class="table-action" href="/accounting/journal/<?= (int) $entry['id'] ?>">Apri</a><?php if ($entry['status'] === 'DRAFT' && $entry['source_type'] === 'MANUAL'): ?><a class="table-action" href="/accounting/journal/<?= (int) $entry['id'] ?>/edit">Modifica</a><?php endif; ?></td>
        </tr>
    <?php endforeach; ?>
    <?php if (!$entries): ?><tr><td colspan="9"><div class="table-empty"><span><?= View::icon('calculator') ?></span><strong>Nessuna registrazione</strong><small>Modifica i filtri o inserisci la prima scrittura.</small></div></td></tr><?php endif; ?>
    </tbody></table></div>
</section>
