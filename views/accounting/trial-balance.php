<?php
use Luna\Core\View;
$query = http_build_query(array_filter(['from' => $from, 'to' => $to, 'q' => $search, 'account_type' => $accountType]));
$renderSection = static function (array $rows, string $from, string $to): void {
    foreach ($rows as $row) {
        $isGroup = empty($row['is_postable']);
        ?><tr class="<?= $isGroup ? 'statement-group' : 'statement-account' ?>">
            <td style="--statement-level:<?= (int) $row['level'] ?>">
                <?php if (!$isGroup && (int) $row['id'] > 0): ?>
                    <a class="statement-account-link" href="/accounting/ledger/<?= (int) $row['id'] ?>?from=<?= View::e($from) ?>&amp;to=<?= View::e($to) ?>" title="Apri mastrino <?= View::e($row['code']) ?>"><?= View::e($row['code']) ?></a>
                <?php else: ?><span><?= View::e($row['code']) ?></span><?php endif; ?>
                <span class="statement-name"><?= View::e($row['name']) ?></span>
            </td>
            <td class="numeric"><?= View::money($row['amount']) ?></td>
        </tr><?php
    }
    if (!$rows): ?><tr><td colspan="2" class="muted">Nessun saldo nella sezione.</td></tr><?php endif;
};
?>
<section class="page-intro compact"><div><span class="eyebrow">Contabilità generale</span><h1>Situazione contabile e mastrini</h1><p>Prospetto gerarchico a sezioni contrapposte. Clicca sul codice di un conto per consultarne la scheda contabile.</p></div><a class="button" href="/accounting/journal">Prima nota</a></section>

<?php if (!empty($opening['posted'])): ?>
<div class="alert alert-success"><?= View::icon('check') ?><div><strong>Saldo di apertura contabilizzato</strong><br>Il periodo contiene una registrazione di apertura in stato definitivo.</div></div>
<?php elseif (!empty($opening['carried'])): ?>
<div class="alert alert-warning"><?= View::icon('alert') ?><div><strong>Apertura non contabilizzata</strong><br>Per consentire il controllo, il prospetto riprende automaticamente i saldi patrimoniali precedenti e azzera Costi e Ricavi. Questa ripresa non sostituisce la registrazione contabile di apertura. Vai in <a href="/accounting/compliance#closing">Adempimenti → Chiusura e apertura esercizio</a> per regolarizzarla.</div></div>
<?php else: ?>
<div class="alert neutral-banner"><?= View::icon('help') ?><div><strong>Nessun saldo precedente da riprendere</strong><br>Il prospetto usa esclusivamente i movimenti del periodo selezionato.</div></div>
<?php endif; ?>

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

<section class="statement-report">
    <header><span>Situazione contabile a sezioni per competenza</span><strong><?= View::date($from) ?> – <?= View::date($to) ?></strong></header>
    <h2>Stato patrimoniale</h2>
    <div class="statement-columns">
        <section class="card statement-section"><h3>Attività</h3><div class="table-wrap"><table><thead><tr><th>Conto</th><th class="numeric">Saldo</th></tr></thead><tbody><?php $renderSection($sections['ASSET'], $from, $to); ?></tbody><tfoot><tr><th>Totale attività</th><th class="numeric"><?= View::money($totals['assets']) ?></th></tr><?php if ($totals['profit_loss'] < 0): ?><tr><th>Perdita d’esercizio</th><th class="numeric"><?= View::money(abs($totals['profit_loss'])) ?></th></tr><?php endif; ?><tr class="statement-grand-total"><th>Totale a pareggio</th><th class="numeric"><?= View::money($totals['balance_assets']) ?></th></tr></tfoot></table></div></section>
        <section class="card statement-section"><h3>Passività e patrimonio netto</h3><div class="table-wrap"><table><thead><tr><th>Conto</th><th class="numeric">Saldo</th></tr></thead><tbody><?php $renderSection(array_merge($sections['EQUITY'], $sections['LIABILITY']), $from, $to); ?></tbody><tfoot><tr><th>Totale passività e patrimonio netto</th><th class="numeric"><?= View::money($totals['liabilities']) ?></th></tr><?php if ($totals['profit_loss'] >= 0): ?><tr><th>Utile d’esercizio</th><th class="numeric"><?= View::money($totals['profit_loss']) ?></th></tr><?php endif; ?><tr class="statement-grand-total"><th>Totale a pareggio</th><th class="numeric"><?= View::money($totals['balance_liabilities']) ?></th></tr></tfoot></table></div></section>
    </div>
    <h2>Conto economico</h2>
    <div class="statement-columns">
        <section class="card statement-section"><h3>Costi</h3><div class="table-wrap"><table><thead><tr><th>Conto</th><th class="numeric">Saldo</th></tr></thead><tbody><?php $renderSection($sections['EXPENSE'], $from, $to); ?></tbody><tfoot><tr><th>Totale costi</th><th class="numeric"><?= View::money($totals['expenses']) ?></th></tr><?php if ($totals['profit_loss'] >= 0): ?><tr><th>Utile d’esercizio</th><th class="numeric"><?= View::money($totals['profit_loss']) ?></th></tr><?php endif; ?><tr class="statement-grand-total"><th>Totale a pareggio</th><th class="numeric"><?= View::money(max($totals['expenses'], $totals['revenues'])) ?></th></tr></tfoot></table></div></section>
        <section class="card statement-section"><h3>Ricavi</h3><div class="table-wrap"><table><thead><tr><th>Conto</th><th class="numeric">Saldo</th></tr></thead><tbody><?php $renderSection($sections['REVENUE'], $from, $to); ?></tbody><tfoot><tr><th>Totale ricavi</th><th class="numeric"><?= View::money($totals['revenues']) ?></th></tr><?php if ($totals['profit_loss'] < 0): ?><tr><th>Perdita d’esercizio</th><th class="numeric"><?= View::money(abs($totals['profit_loss'])) ?></th></tr><?php endif; ?><tr class="statement-grand-total"><th>Totale a pareggio</th><th class="numeric"><?= View::money(max($totals['expenses'], $totals['revenues'])) ?></th></tr></tfoot></table></div></section>
    </div>
    <footer><span>Totali saldi Dare <?= View::money($totals['debit']) ?> · Avere <?= View::money($totals['credit']) ?></span><strong class="<?= abs($totals['debit'] - $totals['credit']) > .005 ? 'danger-text' : '' ?>">Differenza Dare / Avere: <?= View::money(abs($totals['debit'] - $totals['credit'])) ?></strong></footer>
</section>
