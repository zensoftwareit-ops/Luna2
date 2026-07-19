<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<form method="post" action="/documents/<?= View::e($type) ?>/save" class="document-form" data-document-form>
<input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
<section class="card form-card">
<div class="form-grid">
<label><span><?= $definition['counterparty'] === 'customer' ? 'Cliente' : 'Fornitore' ?> *</span><select name="counterparty_id" required><option value="">Seleziona…</option><?php foreach ($counterparties as $party): ?><option value="<?= (int) $party['id'] ?>"><?= View::e(($party['code'] ? $party['code'] . ' · ' : '') . $party['business_name']) ?></option><?php endforeach; ?></select></label>
<label><span>Data documento *</span><input type="date" name="document_date" value="<?= View::e($document['document_date']) ?>" required></label>
<label><span>Scadenza</span><input type="date" name="due_date" value="<?= View::e($document['due_date']) ?>"></label>
<label><span>Valuta</span><input name="currency" value="<?= View::e($document['currency']) ?>" maxlength="3"></label>
<label class="full-width"><span>Oggetto</span><input name="subject"></label>
<?php if (in_array($definition['code'], ['SALES_INVOICE','CREDIT_NOTE'], true)): ?>
<label><span>Tipo FatturaPA</span><input name="fatturapa_type" value="<?= View::e($document['fatturapa_type']) ?>"></label>
<label><span>Esigibilità IVA</span><select name="vat_collectability"><option value="I">Immediata</option><option value="D">Differita</option><option value="S">Scissione pagamenti</option></select></label>
<label><span>Modalità pagamento</span><input name="payment_method_code" value="<?= View::e($document['payment_method_code']) ?>"></label>
<?php endif; ?>
</div>
</section>

<section class="card">
<div class="card-header"><h2>Righe</h2><button class="button" type="button" data-add-line>Aggiungi riga</button></div>
<div class="table-wrap"><table class="line-table"><thead><tr><th>Prodotto</th><th>Descrizione</th><th>Q.tà</th><th>U.M.</th><th>Prezzo</th><th>Sconto %</th><th>IVA %</th><th>Natura</th><th></th></tr></thead><tbody data-lines></tbody></table></div>
</section>

<section class="card form-card"><label><span>Note</span><textarea name="notes" rows="4"></textarea></label><div class="form-actions"><a class="button" href="/documents/<?= View::e($type) ?>">Annulla</a><button class="button primary" type="submit">Salva documento</button></div></section>

<template id="line-template"><tr>
<td><select data-product-select><option value="">Riga libera</option><?php foreach ($products as $product): ?><option value="<?= (int) $product['id'] ?>" data-code="<?= View::e($product['code']) ?>" data-name="<?= View::e($product['name']) ?>" data-unit="<?= View::e($product['unit']) ?>" data-price="<?= View::e($definition['counterparty'] === 'supplier' ? $product['purchase_cost'] : $product['sale_price']) ?>" data-vat="<?= View::e($product['vat_rate']) ?>"><?= View::e($product['code'] . ' · ' . $product['name']) ?></option><?php endforeach; ?></select><input type="hidden" data-field="product_code"></td>
<td><input data-field="description" required></td><td><input data-field="quantity" inputmode="decimal" value="1" required></td><td><input data-field="unit" value="NR"></td><td><input data-field="unit_price" inputmode="decimal" value="0"></td><td><input data-field="discount_percent" inputmode="decimal" value="0"></td><td><input data-field="vat_rate" inputmode="decimal" value="22"></td><td><input data-field="vat_nature"></td><td><button type="button" class="link-button danger-text" data-remove-line>Rimuovi</button></td>
</tr></template>
</form>
