<?php
use Luna\Core\Csrf;
use Luna\Core\View;
$existingLines = array_map(static fn (array $line): array => [
    'account_id' => (int) $line['account_id'], 'description' => $line['description'] ?? '',
    'debit' => $line['debit'] ?? 0, 'credit' => $line['credit'] ?? 0,
    'customer_id' => !empty($line['customer_id']) ? (int) $line['customer_id'] : null,
    'supplier_id' => !empty($line['supplier_id']) ? (int) $line['supplier_id'] : null,
], $lines);
$accountCatalog = $accountCatalog ?? array_map(static fn (array $account): array => ['id'=>(int)$account['id'],'code'=>(string)$account['code'],'name'=>(string)$account['name'],'customer_id'=>null,'supplier_id'=>null,'kind'=>'ACCOUNT'], $accounts);
$causes = $causes ?? [];
$vatRegisters = $vatRegisters ?? [];
$openItems = $openItems ?? [];
$bankAccounts = $bankAccounts ?? [];
$settlementAccounts = $settlementAccounts ?? [];
$guidedPaymentItems = array_map(static function (array $item): array {
    $operation = $item['direction'] === 'RECEIVABLE' ? 'Incasso' : 'Pagamento';
    $reference = trim((string) ($item['reference'] ?? '')) ?: 'senza riferimento';
    $primary = $reference . ' · ' . (string) $item['party_name'];
    $secondary = $operation . ' · scad. ' . View::date($item['due_date']) . ' · ' . View::money($item['outstanding']);
    return [
        'id' => (int) $item['id'],
        'primary' => $primary,
        'secondary' => $secondary,
        'label' => $primary . ' · ' . $secondary,
        'outstanding' => (string) $item['outstanding'],
        'direction' => (string) $item['direction'],
    ];
}, $openItems);
$editingProvisional = !empty($entry['id']) && ($entry['status'] ?? '') === 'POSTED' && empty($entry['is_finalized']);
$isDocumentEntry = !empty($entry['id']) && ($entry['source_type'] ?? '') === 'DOCUMENT';
?>
<section class="page-intro compact"><div><a class="back-link" href="/accounting/journal">← Prima nota</a><h1><?= !empty($entry['id']) ? 'Modifica registrazione' : 'Nuova registrazione' ?></h1><p><?= $editingProvisional ? 'La scrittura è contabilizzata in modo provvisorio: la modifica sarà conservata nello storico revisioni.' : 'Salva come bozza oppure contabilizza dopo aver verificato la quadratura.' ?></p></div></section>
<?php if (empty($entry['id']) && $openItems): ?>
<section class="card guided-payment-card">
<div class="form-section-head"><div><span class="section-kicker">Flusso consigliato</span><h2>Registra da una fattura nello scadenzario</h2><p class="help">Per incassi e pagamenti seleziona la partita: Luna2 crea automaticamente Dare/Avere e aggiorna scadenza, partitario e mastrino.</p></div><span class="badge status-posted">Automatico</span></div>
<form method="post" action="/accounting/treasury/payments" class="modern-form" data-guided-payment>
<input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
<div class="form-grid"><label class="field full-width"><span>Fatture / partite *</span><div class="account-lookup guided-payment-lookup" data-guided-payment-lookup><input type="search" data-guided-payment-search placeholder="Cerca e aggiungi una o più fatture…" autocomplete="off" aria-autocomplete="list" aria-expanded="false" aria-controls="guided-payment-results"><div class="account-lookup-results" id="guided-payment-results" data-guided-payment-results role="listbox" hidden></div></div><small>Seleziona tutte le fatture da chiudere con la stessa operazione bancaria. Non è possibile mescolare incassi e pagamenti.</small></label><div class="field full-width guided-payment-selection" data-guided-payment-selection><span>Partite selezionate</span><div data-guided-payment-items><small>Nessuna partita selezionata.</small></div><strong data-guided-payment-total>Totale 0,00 €</strong></div><label class="field"><span>Data *</span><input type="date" name="payment_date" value="<?= date('Y-m-d') ?>" required></label><label class="field"><span>Metodo</span><select name="method"><option value="BANK_TRANSFER">Bonifico</option><option value="CASH">Contanti</option><option value="CARD">Carta</option><option value="DIRECT_DEBIT">Addebito</option><option value="OTHER">Altro</option></select></label><label class="field"><span>Contropartita finanziaria *</span><select name="settlement_source" required><option value="">Seleziona banca o cassa…</option><?php foreach ($bankAccounts as $bank): ?><option value="bank:<?= (int) $bank['id'] ?>">Conto bancario · <?= View::e($bank['name']) ?></option><?php endforeach; ?><?php foreach ($settlementAccounts as $account): ?><option value="account:<?= (int) $account['id'] ?>">Conto contabile · <?= View::e($account['code'] . ' · ' . $account['name']) ?></option><?php endforeach; ?></select><small>Puoi usare sia un conto bancario configurato in Tesoreria sia il conto banca/cassa del piano dei conti.</small></label><label class="field"><span>Riferimento</span><input name="reference_number"></label></div>
<script type="application/json" data-guided-payment-catalog><?= json_encode($guidedPaymentItems, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_HEX_TAG | JSON_HEX_AMP) ?></script>
<div class="form-actions"><button class="button primary" type="submit">Crea e contabilizza la scrittura</button></div>
</form></section>
<div class="section-divider"><span>oppure registra un’operazione diversa</span></div>
<?php endif; ?>
<form method="post" action="/accounting/journal/save" data-accounting-form data-existing-lines="<?= View::e(json_encode($existingLines, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)) ?>">
<input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><?php if (!empty($entry['id'])): ?><input type="hidden" name="id" value="<?= (int) $entry['id'] ?>"><?php endif; ?>
<section class="card form-card"><div class="form-section-head"><div><span class="section-kicker">Testata movimento</span><h2>Dati registrazione</h2></div><?php if (!empty($entry['protocol_number'])): ?><span class="badge status-draft"><?= View::e($entry['protocol_number']) ?></span><?php endif; ?></div><div class="form-grid">
<label class="field"><span>Data registrazione *</span><input type="date" name="entry_date" value="<?= View::e($entry['entry_date'] ?? date('Y-m-d')) ?>" required></label>
<label class="field"><span>Data competenza</span><input type="date" name="competence_date" value="<?= View::e($entry['competence_date'] ?? date('Y-m-d')) ?>"></label>
<?php if ($isDocumentEntry): ?><label class="field"><span>Tipo movimento</span><input value="<?= View::e(View::label($entry['entry_type'])) ?>" disabled><input type="hidden" name="entry_type" value="<?= View::e($entry['entry_type']) ?>"><small>Il tipo deriva dal documento collegato e non viene alterato.</small></label><?php else: ?><label class="field"><span>Tipo movimento</span><select name="entry_type"><option value="MANUAL" <?= ($entry['entry_type'] ?? '') === 'MANUAL' ? 'selected' : '' ?>>Operazione manuale</option><option value="OPENING" <?= ($entry['entry_type'] ?? '') === 'OPENING' ? 'selected' : '' ?>>Apertura conti</option><option value="ADJUSTMENT" <?= ($entry['entry_type'] ?? '') === 'ADJUSTMENT' ? 'selected' : '' ?>>Rettifica</option><option value="CLOSING" <?= ($entry['entry_type'] ?? '') === 'CLOSING' ? 'selected' : '' ?>>Chiusura conti</option><option value="PAYMENT" <?= ($entry['entry_type'] ?? '') === 'PAYMENT' ? 'selected' : '' ?>>Pagamento/incasso</option></select></label><?php endif; ?>
<label class="field"><span>Causale contabile</span><select name="cause_id"><option value="">Nessuna</option><?php foreach ($causes as $cause): ?><option value="<?= (int) $cause['id'] ?>" <?= (int) ($entry['cause_id'] ?? 0) === (int) $cause['id'] ? 'selected' : '' ?>><?= View::e($cause['code'] . ' · ' . $cause['name']) ?></option><?php endforeach; ?></select><?php if (!$causes): ?><small class="field-error">Nessuna causale attiva: esegui le migrazioni e verifica Contabilità → Piano dei conti e causali.</small><?php endif; ?></label>
<label class="field"><span>Sezionale IVA</span><select name="vat_register_id"><option value="">Nessuno</option><?php foreach ($vatRegisters as $register): ?><option value="<?= (int) $register['id'] ?>" <?= (int) ($entry['vat_register_id'] ?? 0) === (int) $register['id'] ? 'selected' : '' ?>><?= View::e($register['code'] . ' · ' . $register['name']) ?></option><?php endforeach; ?></select></label>
<label class="field"><span>Numero documento</span><input name="document_number" value="<?= View::e($entry['document_number'] ?? '') ?>"></label>
<label class="field"><span>Protocollo origine</span><input name="source_protocol" value="<?= View::e($entry['source_protocol'] ?? '') ?>"></label>
<label class="field"><span>Controparte</span><input name="counterparty" value="<?= View::e($entry['counterparty'] ?? '') ?>"></label>
<label class="field full-width"><span>Descrizione *</span><input name="description" value="<?= View::e($entry['description'] ?? '') ?>" required></label>
<label class="field full-width"><span>Note</span><textarea name="notes" rows="3"><?= View::e($entry['notes'] ?? '') ?></textarea></label>
<?php if ($editingProvisional): ?><label class="field full-width"><span>Motivo della modifica <small>(facoltativo)</small></span><input name="change_reason" maxlength="500" placeholder="Es. correzione conto o importo su indicazione del commercialista"><small>La versione precedente viene comunque conservata nel registro revisioni.</small></label><?php endif; ?>
</div></section>
<section class="card accounting-lines-card"><div class="card-header"><div><span class="section-kicker">Partita doppia</span><h2>Righe Dare/Avere</h2></div><button class="button" type="button" data-add-accounting-line><?= View::icon('plus') ?> Aggiungi riga</button></div><div class="table-wrap"><table class="line-table"><thead><tr><th>Conto</th><th>Descrizione</th><th>Dare</th><th>Avere</th><th></th></tr></thead><tbody data-accounting-lines></tbody><tfoot><tr><th colspan="2">Totali</th><th data-debit-total>0,00 €</th><th data-credit-total>0,00 €</th><th data-balance-status></th></tr></tfoot></table></div></section>
<div class="form-actions accounting-actions"><a class="button ghost" href="/accounting/journal">Annulla</a><?php if (!$editingProvisional): ?><button class="button" type="submit" name="intent" value="draft">Salva bozza</button><?php endif; ?><button class="button primary" type="submit" name="intent" value="post"><?= View::icon('check') ?> <?= $editingProvisional ? 'Salva modifica provvisoria' : 'Contabilizza' ?></button></div>
<template id="accounting-line-template"><tr><td><div class="account-lookup accounting-line-lookup" data-line-account-lookup><input type="search" data-line-account-search placeholder="Cerca conto, cliente o fornitore…" autocomplete="off" required><input type="hidden" data-account-field="account_id"><input type="hidden" data-account-field="customer_id"><input type="hidden" data-account-field="supplier_id"><div class="account-lookup-results" data-line-account-results hidden></div></div></td><td><input data-account-field="description"></td><td><input data-account-field="debit" inputmode="decimal" value="0"></td><td><input data-account-field="credit" inputmode="decimal" value="0"></td><td><button type="button" class="table-action danger-text" data-remove-line>Rimuovi</button></td></tr></template>
<script type="application/json" data-manual-account-catalog><?= json_encode($accountCatalog, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_HEX_TAG | JSON_HEX_AMP) ?></script>
</form>
