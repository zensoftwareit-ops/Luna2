<?php use Luna\Core\View; ?>
<div class="page-actions">
    <form method="get" class="search-form"><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Numero, controparte, oggetto…"><button class="button">Cerca</button></form>
    <a class="button primary" href="/documents/<?= View::e($type) ?>/create">Nuovo <?= View::e($definition['singular']) ?></a>
</div>
<section class="card table-wrap">
<table><thead><tr><th>Data</th><th>Numero</th><th>Controparte</th><th>Oggetto</th><th>Imponibile</th><th>IVA</th><th>Totale</th><th>Residuo</th><th>Stato</th></tr></thead><tbody>
<?php foreach ($documents as $document): ?><tr class="clickable-row" data-href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><td><?= View::date($document['document_date']) ?></td><td><a href="/documents/<?= View::e($type) ?>/<?= (int) $document['id'] ?>"><?= View::e($document['number']) ?></a></td><td><?= View::e($document['counterparty_name']) ?></td><td><?= View::e($document['subject']) ?></td><td><?= View::money($document['taxable_total']) ?></td><td><?= View::money($document['vat_total']) ?></td><td><?= View::money($document['total']) ?></td><td><?= View::money($document['balance_due']) ?></td><td><span class="badge"><?= View::e($document['status']) ?></span></td></tr><?php endforeach; ?>
<?php if (!$documents): ?><tr><td colspan="9" class="muted">Nessun documento.</td></tr><?php endif; ?>
</tbody></table></section>
