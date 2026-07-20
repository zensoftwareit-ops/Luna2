<?php
use Luna\Core\Csrf;
use Luna\Core\View;
$existingLines = array_map(static fn (array $line): array => [
    'account_id' => (int) $line['account_id'], 'description' => $line['description'] ?? '',
    'debit' => $line['debit'] ?? 0, 'credit' => $line['credit'] ?? 0,
], $lines);
?>
<section class="page-intro compact"><div><a class="back-link" href="/accounting/journal">← Prima nota</a><h1><?= !empty($entry['id']) ? 'Modifica registrazione' : 'Nuova registrazione' ?></h1><p>Salva come bozza oppure contabilizza dopo aver verificato la quadratura.</p></div></section>
<form method="post" action="/accounting/journal/save" data-accounting-form data-existing-lines="<?= View::e(json_encode($existingLines, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)) ?>">
<input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><?php if (!empty($entry['id'])): ?><input type="hidden" name="id" value="<?= (int) $entry['id'] ?>"><?php endif; ?>
<section class="card form-card"><div class="form-section-head"><div><span class="section-kicker">Testata movimento</span><h2>Dati registrazione</h2></div><?php if (!empty($entry['protocol_number'])): ?><span class="badge status-draft"><?= View::e($entry['protocol_number']) ?></span><?php endif; ?></div><div class="form-grid">
<label class="field"><span>Data registrazione *</span><input type="date" name="entry_date" value="<?= View::e($entry['entry_date'] ?? date('Y-m-d')) ?>" required></label>
<label class="field"><span>Data competenza</span><input type="date" name="competence_date" value="<?= View::e($entry['competence_date'] ?? date('Y-m-d')) ?>"></label>
<label class="field"><span>Causale</span><select name="entry_type"><option value="MANUAL" <?= ($entry['entry_type'] ?? '') === 'MANUAL' ? 'selected' : '' ?>>Operazione manuale</option><option value="OPENING" <?= ($entry['entry_type'] ?? '') === 'OPENING' ? 'selected' : '' ?>>Apertura conti</option><option value="ADJUSTMENT" <?= ($entry['entry_type'] ?? '') === 'ADJUSTMENT' ? 'selected' : '' ?>>Rettifica</option><option value="CLOSING" <?= ($entry['entry_type'] ?? '') === 'CLOSING' ? 'selected' : '' ?>>Chiusura conti</option><option value="PAYMENT" <?= ($entry['entry_type'] ?? '') === 'PAYMENT' ? 'selected' : '' ?>>Pagamento/incasso</option></select></label>
<label class="field"><span>Numero documento</span><input name="document_number" value="<?= View::e($entry['document_number'] ?? '') ?>"></label>
<label class="field"><span>Controparte</span><input name="counterparty" value="<?= View::e($entry['counterparty'] ?? '') ?>"></label>
<label class="field full-width"><span>Descrizione *</span><input name="description" value="<?= View::e($entry['description'] ?? '') ?>" required></label>
<label class="field full-width"><span>Note</span><textarea name="notes" rows="3"><?= View::e($entry['notes'] ?? '') ?></textarea></label>
</div></section>
<section class="card accounting-lines-card"><div class="card-header"><div><span class="section-kicker">Partita doppia</span><h2>Righe Dare/Avere</h2></div><button class="button" type="button" data-add-accounting-line><?= View::icon('plus') ?> Aggiungi riga</button></div><div class="table-wrap"><table class="line-table"><thead><tr><th>Conto</th><th>Descrizione</th><th>Dare</th><th>Avere</th><th></th></tr></thead><tbody data-accounting-lines></tbody><tfoot><tr><th colspan="2">Totali</th><th data-debit-total>0,00 €</th><th data-credit-total>0,00 €</th><th data-balance-status></th></tr></tfoot></table></div></section>
<div class="form-actions accounting-actions"><a class="button ghost" href="/accounting/journal">Annulla</a><button class="button" type="submit" name="intent" value="draft">Salva bozza</button><button class="button primary" type="submit" name="intent" value="post"><?= View::icon('check') ?> Contabilizza</button></div>
<template id="accounting-line-template"><tr><td><select data-account-field="account_id" required><option value="">Seleziona conto…</option><?php foreach ($accounts as $account): ?><option value="<?= (int) $account['id'] ?>"><?= View::e($account['code'] . ' · ' . $account['name']) ?></option><?php endforeach; ?></select></td><td><input data-account-field="description"></td><td><input data-account-field="debit" inputmode="decimal" value="0"></td><td><input data-account-field="credit" inputmode="decimal" value="0"></td><td><button type="button" class="table-action danger-text" data-remove-line>Rimuovi</button></td></tr></template>
</form>
