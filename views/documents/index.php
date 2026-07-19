<?php use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Documenti</span><h1><?= View::e($definition['title']) ?></h1><p><?= count($documents) ?> documenti visualizzati</p></div>
    <a class="button primary" href="/documents/<?= View::e($type) ?>/create"><?= View::icon('plus') ?> Nuovo <?= View::e(mb_strtolower($definition['singular'])) ?></a>
</section>
<section class="list-toolbar">
    <form method="get" class="search-form"><span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Numero, controparte o oggetto…"></span><button class="button">Cerca</button></form>
</section>
<section class="card data-card"><div class="table-wrap">
<table><thead><tr><th>Documento</th><th>Controparte</th><th>Oggetto</th><th>Data</th><th class="numeric">Imponibile</th><th class="numeric">IVA</th><th class="numeric">Totale</th><th class="numeric">Residuo</th><th>Stato</th></tr></thead><tbody>
<?php foreach ($documents as $document): ?><tr class="clickable-row" data-href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><td class="primary-cell"><a href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><?= View::e($document['number']) ?></a></td><td><?= View::e($document['counterparty_name']) ?></td><td><?= View::e($document['subject'] ?: '—') ?></td><td><?= View::date($document['document_date']) ?></td><td class="numeric"><?= View::money($document['taxable_total']) ?></td><td class="numeric"><?= View::money($document['vat_total']) ?></td><td class="numeric"><strong><?= View::money($document['total']) ?></strong></td><td class="numeric"><?= View::money($document['balance_due']) ?></td><td><span class="badge status-<?= View::e(strtolower($document['status'])) ?>"><?= View::e($document['status']) ?></span></td></tr><?php endforeach; ?>
<?php if (!$documents): ?><tr><td colspan="9"><div class="table-empty"><span><?= View::icon('receipt') ?></span><strong>Nessun documento</strong><small>Crea il primo <?= View::e(mb_strtolower($definition['singular'])) ?> per iniziare.</small></div></td></tr><?php endif; ?>
</tbody></table></div></section>
