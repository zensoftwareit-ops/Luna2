<?php
use Luna\Core\Csrf;
use Luna\Core\View;

$existingLines = array_map(static fn (array $line): array => [
    'line_id' => (int) $line['id'],
    'account_id' => (int) $line['account_id'],
    'description' => (string) ($line['description'] ?? ''),
    'debit' => $line['debit'] ?? 0,
    'credit' => $line['credit'] ?? 0,
    'customer_id' => !empty($line['customer_id']) ? (int) $line['customer_id'] : null,
    'supplier_id' => !empty($line['supplier_id']) ? (int) $line['supplier_id'] : null,
], $lines);
$accountCatalog = $accountCatalog ?? array_map(static fn (array $account): array => ['id'=>(int)$account['id'],'code'=>(string)$account['code'],'name'=>(string)$account['name'],'customer_id'=>null,'supplier_id'=>null,'kind'=>'ACCOUNT'], $accounts);
?>
<section class="page-intro compact">
    <div>
        <a class="back-link" href="/accounting/journal/<?= (int) $entry['id'] ?>">← Registrazione <?= View::e($entry['protocol_number']) ?></a>
        <span class="eyebrow">Prima nota provvisoria</span>
        <h1>Modifica righe contabili</h1>
        <p><?= View::date($entry['entry_date']) ?> · <?= View::e($entry['description']) ?></p>
    </div>
</section>

<div class="notice warning">
    <strong>La scrittura deve restare quadrata.</strong>
    <span>Puoi aggiungere, modificare o rimuovere righe finché il periodo e la registrazione non sono definitivi. Se la scrittura deriva da una fattura, questo editor modifica la partita doppia; articoli e importi fiscali si correggono separatamente nel Riepilogo IVA.</span>
</div>

<form method="post" action="/accounting/journal/<?= (int) $entry['id'] ?>/lines" data-accounting-form data-require-balanced="1" data-existing-lines="<?= View::e(json_encode($existingLines, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)) ?>">
    <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
    <section class="card accounting-lines-card">
        <div class="card-header">
            <div><span class="section-kicker">Partita doppia</span><h2>Righe Dare/Avere</h2><p class="help">Cerca il conto per codice o descrizione. Ogni riga può contenere un importo soltanto in Dare oppure in Avere.</p></div>
            <button class="button" type="button" data-add-accounting-line><?= View::icon('plus') ?> Aggiungi riga</button>
        </div>
        <div class="table-wrap">
            <table class="line-table">
                <thead><tr><th>Conto</th><th>Descrizione</th><th>Dare</th><th>Avere</th><th></th></tr></thead>
                <tbody data-accounting-lines></tbody>
                <tfoot><tr><th colspan="2">Totali</th><th data-debit-total>0,00 €</th><th data-credit-total>0,00 €</th><th data-balance-status></th></tr></tfoot>
            </table>
        </div>
    </section>
    <section class="card form-card compact-form-card">
        <div class="form-grid">
            <label class="field full-width"><span>Motivo della modifica <small>(facoltativo)</small></span><input name="reason" maxlength="500" placeholder="Es. correzione ripartizione costi richiesta dal responsabile contabile"><small>La versione precedente viene comunque conservata nello storico revisioni.</small></label>
        </div>
    </section>
    <div class="form-actions accounting-actions">
        <a class="button ghost" href="/accounting/journal/<?= (int) $entry['id'] ?>">Annulla</a>
        <button class="button primary" type="submit"><?= View::icon('check') ?> Salva righe provvisorie</button>
    </div>
    <template id="accounting-line-template"><tr><td><div class="account-lookup accounting-line-lookup" data-line-account-lookup><input type="search" data-line-account-search placeholder="Cerca conto, cliente o fornitore…" autocomplete="off" required><input type="hidden" data-account-field="account_id"><input type="hidden" data-account-field="customer_id"><input type="hidden" data-account-field="supplier_id"><input type="hidden" data-account-field="line_id"><div class="account-lookup-results" data-line-account-results hidden></div></div></td><td><input data-account-field="description" maxlength="500"></td><td><input data-account-field="debit" inputmode="decimal" value="0"></td><td><input data-account-field="credit" inputmode="decimal" value="0"></td><td><button type="button" class="table-action danger-text" data-remove-line>Rimuovi</button></td></tr></template>
    <script type="application/json" data-manual-account-catalog><?= json_encode($accountCatalog, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_HEX_TAG | JSON_HEX_AMP) ?></script>
</form>
