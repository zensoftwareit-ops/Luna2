<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<form method="post" action="/documents/<?= View::e($type) ?>/save" class="document-form" data-document-form>
<input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
<section class="card form-card">
<div class="form-grid">
<label class="field"><span><?= $definition['counterparty'] === 'customer' ? 'Cliente' : 'Fornitore' ?> *</span><select name="counterparty_id" required data-counterparty><option value="">Seleziona…</option><?php foreach ($counterparties as $party): ?><option value="<?= (int) $party['id'] ?>" data-payment-days="<?= (int) ($party['payment_days'] ?? 30) ?>" data-month-end="<?= !empty($party['payment_month_end']) ? '1' : '0' ?>" data-method="<?= View::e($party['payment_method_code'] ?? '') ?>" data-withholding="<?= !empty($party['withholding_enabled']) ? '1' : '0' ?>" data-withholding-type="<?= View::e($party['withholding_type'] ?? '') ?>" data-withholding-rate="<?= View::e($party['withholding_rate'] ?? '') ?>" data-withholding-taxable="<?= View::e($party['withholding_taxable_percent'] ?? '') ?>" data-withholding-cause="<?= View::e($party['withholding_cause'] ?? '') ?>"><?= View::e(($party['code'] ? $party['code'] . ' · ' : '') . $party['business_name']) ?></option><?php endforeach; ?></select></label>
<label class="field"><span>Data documento *</span><input type="date" name="document_date" value="<?= View::e($document['document_date']) ?>" required></label>
<label class="field"><span>Scadenza</span><input type="date" name="due_date" value="<?= View::e($document['due_date']) ?>"></label>
<label class="field"><span>Valuta</span><input name="currency" value="<?= View::e($document['currency']) ?>" maxlength="3"></label>
<label class="field full-width"><span>Oggetto</span><input name="subject"></label>
<?php if (in_array($definition['code'], ['SALES_INVOICE','CREDIT_NOTE'], true)): ?>
<label class="field"><span>Tipo FatturaPA</span><input name="fatturapa_type" value="<?= View::e($document['fatturapa_type']) ?>"></label>
<label class="field"><span>Esigibilità IVA</span><select name="vat_collectability"><option value="I">Immediata</option><option value="D">Differita</option><option value="S">Scissione pagamenti</option></select></label>
<?php endif; ?>
<?php if (in_array($definition['code'], ['SALES_INVOICE','PURCHASE_INVOICE','CREDIT_NOTE'], true)): ?>
<label class="field"><span>Modalità pagamento</span><select name="payment_method_code"><option value="MP05">Bonifico</option><option value="MP01">Contanti</option><option value="MP12">Ri.Ba.</option><option value="MP08">Carta</option></select></label>
<?php endif; ?>
<?php if (in_array($definition['code'], ['SALES_INVOICE','PURCHASE_INVOICE'], true)): $withholdingSource = $definition['counterparty'] === 'supplier' ? [] : $organizationFiscal; ?>
<label class="field checkbox-field"><input class="switch-input" type="checkbox" name="withholding_enabled" value="1" data-withholding-enabled <?= !empty($withholdingSource['withholding_enabled']) ? 'checked' : '' ?>><span class="switch-ui"></span><span>Applica ritenuta</span></label>
<label class="field"><span>Tipo ritenuta</span><input name="withholding_type" value="<?= View::e($withholdingSource['withholding_type'] ?? 'RT01') ?>"></label>
<label class="field"><span>Aliquota ritenuta %</span><input name="withholding_rate" inputmode="decimal" value="<?= View::e($withholdingSource['withholding_rate'] ?? '20') ?>"></label>
<label class="field"><span>Imponibile ritenuta %</span><input name="withholding_taxable_percent" inputmode="decimal" value="<?= View::e($withholdingSource['withholding_taxable_percent'] ?? '100') ?>"></label>
<label class="field"><span>Causale pagamento</span><input name="withholding_cause" value="<?= View::e($withholdingSource['withholding_cause'] ?? '') ?>"></label>
<?php endif; ?>
</div>
</section>

<section class="card">
<div class="card-header"><h2>Righe</h2><button class="button" type="button" data-add-line>Aggiungi riga</button></div>
<div class="table-wrap"><table class="line-table"><thead><tr><th>Prodotto</th><th>Descrizione</th><th>Q.tà</th><th>U.M.</th><th>Prezzo</th><th>Sconto %</th><th>IVA %</th><th>Natura</th><th></th></tr></thead><tbody data-lines></tbody></table></div>
</section>

<section class="card form-card"><label class="field"><span>Note</span><textarea name="notes" rows="4"></textarea></label><div class="form-actions"><a class="button" href="/documents/<?= View::e($type) ?>">Annulla</a><button class="button primary" type="submit">Salva documento</button></div></section>

<template id="line-template"><tr>
<td><select data-product-select><option value="">Riga libera</option><?php foreach ($products as $product): ?><option value="<?= (int) $product['id'] ?>" data-code="<?= View::e($product['code']) ?>" data-name="<?= View::e($product['name']) ?>" data-unit="<?= View::e($product['unit']) ?>" data-price="<?= View::e($definition['counterparty'] === 'supplier' ? $product['purchase_cost'] : $product['sale_price']) ?>" data-vat="<?= View::e($product['vat_rate']) ?>"><?= View::e($product['code'] . ' · ' . $product['name']) ?></option><?php endforeach; ?></select><input type="hidden" data-field="product_code"></td>
<td><input data-field="description" required></td><td><input data-field="quantity" inputmode="decimal" value="1" required></td><td><input data-field="unit" value="NR"></td><td><input data-field="unit_price" inputmode="decimal" value="0"></td><td><input data-field="discount_percent" inputmode="decimal" value="0"></td><td><input data-field="vat_rate" inputmode="decimal" value="22"></td><td><input data-field="vat_nature"></td><td><button type="button" class="link-button danger-text" data-remove-line>Rimuovi</button></td>
</tr></template>
</form>
