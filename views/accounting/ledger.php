<?php
use Luna\Core\View;
$query = http_build_query(array_filter(['from' => $from, 'to' => $to, 'q' => $search]));
?>
<section class="page-intro compact"><div><span class="eyebrow">Mastrino contabile</span><h1><?= View::e($account['code'] . ' · ' . $account['name']) ?></h1><p>Saldo iniziale al <?= View::date($from) ?>: <strong><?= View::money($account['opening_balance']) ?></strong></p></div><a class="button" href="/accounting/trial-balance?from=<?= View::e($from) ?>&to=<?= View::e($to) ?>">← Bilancio di verifica</a></section>
<section class="list-toolbar exportable-toolbar">
    <form method="get" class="search-form filter-form">
        <span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Protocollo, documento, controparte…"></span>
        <label>Conto <select onchange="if(this.value)location.href='/accounting/ledger/'+this.value+'?from=<?= View::e($from) ?>&to=<?= View::e($to) ?>'"><?php foreach ($accounts as $option): ?><option value="<?= (int) $option['id'] ?>" <?= (int) $option['id'] === (int) $account['id'] ? 'selected' : '' ?>><?= View::e($option['code'] . ' · ' . $option['name']) ?></option><?php endforeach; ?></select></label>
        <label>Dal <input type="date" name="from" value="<?= View::e($from) ?>"></label><label>Al <input type="date" name="to" value="<?= View::e($to) ?>"></label>
        <button class="button">Filtra</button>
    </form>
    <div class="export-actions"><a class="button ghost" href="/accounting/ledger/<?= (int) $account['id'] ?>/export/pdf?<?= View::e($query) ?>">PDF</a><a class="button ghost" href="/accounting/ledger/<?= (int) $account['id'] ?>/export/xlsx?<?= View::e($query) ?>">XLSX</a><a class="button ghost" href="/accounting/ledger/<?= (int) $account['id'] ?>/export/csv?<?= View::e($query) ?>">CSV</a></div>
</section>
<section class="card data-card"><div class="table-wrap"><table><thead><tr><th>Data</th><th>Protocollo</th><th>Descrizione</th><th>Documento / controparte</th><th class="numeric">Dare</th><th class="numeric">Avere</th><th class="numeric">Saldo</th></tr></thead><tbody><?php foreach ($lines as $line): ?><tr><td><?= View::date($line['entry_date']) ?></td><td class="primary-cell"><a href="/accounting/journal/<?= (int) $line['entry_id'] ?>"><?= View::e($line['protocol_number']) ?></a></td><td><?= View::e($line['description'] ?: $line['entry_description']) ?></td><td><?= View::e($line['document_number'] ?: '—') ?><small class="cell-subtitle"><?= View::e($line['counterparty'] ?: '') ?></small></td><td class="numeric"><?= View::money($line['debit']) ?></td><td class="numeric"><?= View::money($line['credit']) ?></td><td class="numeric"><strong><?= View::money($line['running_balance']) ?></strong></td></tr><?php endforeach; ?><?php if (!$lines): ?><tr><td colspan="7" class="muted">Nessun movimento per i filtri selezionati.</td></tr><?php endif; ?></tbody></table></div></section>
