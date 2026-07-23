<?php
use Luna\Core\View;
$query = http_build_query(array_filter(['from' => $from, 'to' => $to, 'q' => $search, 'account_type' => $accountType]));
?>
<section class="page-intro compact"><div><span class="eyebrow">Contabilità generale</span><h1>Bilancio di verifica e mastrini</h1><p>Clicca sul codice di un conto per aprire il relativo mastrino analitico.</p></div><a class="button" href="/accounting/journal">Prima nota</a></section>
<section class="list-toolbar exportable-toolbar">
    <form method="get" class="search-form filter-form">
        <span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Codice o descrizione conto…"></span>
        <label>Dal <input type="date" name="from" value="<?= View::e($from) ?>"></label>
        <label>Al <input type="date" name="to" value="<?= View::e($to) ?>"></label>
        <label>Tipo <select name="account_type"><option value="">Tutti i tipi</option><?php foreach (['ASSET'=>'Attività','LIABILITY'=>'Passività','EQUITY'=>'Patrimonio netto','REVENUE'=>'Ricavi','EXPENSE'=>'Costi'] as $value => $label): ?><option value="<?= $value ?>" <?= $accountType === $value ? 'selected' : '' ?>><?= $label ?></option><?php endforeach; ?></select></label>
        <button class="button">Filtra</button><a class="button ghost" href="/accounting/trial-balance">Azzera</a>
    </form>
    <div class="export-actions"><a class="button ghost" href="/accounting/trial-balance/export/pdf?<?= View::e($query) ?>">PDF</a><a class="button ghost" href="/accounting/trial-balance/export/xlsx?<?= View::e($query) ?>">XLSX</a><a class="button ghost" href="/accounting/trial-balance/export/csv?<?= View::e($query) ?>">CSV</a></div>
</section>
<section class="card data-card"><div class="table-wrap"><table><thead><tr><th>Conto / mastrino</th><th>Descrizione</th><th>Tipo</th><th class="numeric">Dare</th><th class="numeric">Avere</th><th class="numeric">Saldo</th></tr></thead><tbody><?php foreach ($accounts as $account): ?><tr><td class="primary-cell"><a href="/accounting/ledger/<?= (int) $account['id'] ?>?from=<?= View::e($from) ?>&to=<?= View::e($to) ?>"><?= View::e($account['code']) ?></a><small>Apri mastrino</small></td><td><?= View::e($account['name']) ?></td><td><?= View::e($account['account_type']) ?></td><td class="numeric"><?= View::money($account['debit']) ?></td><td class="numeric"><?= View::money($account['credit']) ?></td><td class="numeric"><?= View::money($account['balance']) ?></td></tr><?php endforeach; ?><?php if (!$accounts): ?><tr><td colspan="6" class="muted">Nessun conto movimentato per i filtri selezionati.</td></tr><?php endif; ?></tbody><tfoot><tr><th colspan="3">Totali</th><th class="numeric"><?= View::money($totals['debit']) ?></th><th class="numeric"><?= View::money($totals['credit']) ?></th><th class="numeric <?= abs($totals['debit'] - $totals['credit']) > .005 ? 'danger-text' : '' ?>"><?= View::money($totals['debit'] - $totals['credit']) ?></th></tr></tfoot></table></div></section>
