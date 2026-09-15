<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<div class="page-actions"><a class="button" href="/imports">← Importazioni</a><div><?php if (in_array($batch['status'], ['READY','ERROR'], true)): ?><form method="post" action="/imports/<?= (int) $batch['id'] ?>/commit" class="inline-form" data-confirm="Confermare l’importazione nel database operativo?"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="button primary">Conferma importazione</button></form><?php elseif (in_array($batch['status'], ['COMPLETED','COMPLETED_WITH_ERRORS'], true)): ?><form method="post" action="/imports/<?= (int) $batch['id'] ?>/rollback" class="inline-form" data-confirm="Annullare tutti i record riconducibili a questo lotto?"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="button danger">Annulla importazione</button></form><?php endif; ?></div></div>
<section class="metric-grid compact"><article class="metric"><span>Stato</span><strong class="small-text"><?= View::e(View::label($batch['status'])) ?></strong></article><article class="metric"><span>Tipo</span><strong class="small-text"><?= View::e(View::label($batch['import_type'])) ?></strong></article><article class="metric"><span>Righe</span><strong><?= (int) $batch['total_rows'] ?></strong></article><article class="metric"><span>Importate</span><strong><?= (int) $batch['imported_rows'] ?></strong></article><article class="metric <?= (int) $batch['error_rows'] > 0 ? 'danger' : '' ?>"><span>Errori</span><strong><?= (int) $batch['error_rows'] ?></strong></article></section>
<?php if ($batch['error_message']): ?><div class="alert alert-error"><?= View::e($batch['error_message']) ?></div><?php endif; ?>
<?php $fieldLabels = [
    'name' => 'Denominazione', 'business_name' => 'Ragione sociale', 'code' => 'Codice',
    'vat_number' => 'Partita IVA', 'tax_code' => 'Codice fiscale', 'email' => 'E-mail',
    'phone' => 'Telefono', 'address' => 'Indirizzo', 'city' => 'Comune',
    'postal_code' => 'CAP', 'province' => 'Provincia', 'country' => 'Paese',
    'description' => 'Descrizione', 'date' => 'Data', 'amount' => 'Importo',
    'account_code' => 'Codice conto', 'debit' => 'Dare', 'credit' => 'Avere',
    'document_number' => 'Numero documento', 'document_date' => 'Data documento',
    'due_date' => 'Scadenza', 'taxable_amount' => 'Imponibile', 'vat_amount' => 'IVA',
]; ?>
<section class="card table-wrap"><table>
    <thead><tr><th>Riga</th><th>Dati da importare</th><th>Stato</th><th>Segnalazioni</th></tr></thead>
    <tbody>
    <?php foreach ($rows as $row): ?>
        <tr>
            <td><?= (int) $row['source_row_number'] ?></td>
            <td class="import-preview-cell">
                <dl class="import-field-list">
                <?php foreach ((array) $row['data'] as $key => $value): ?>
                    <div><dt><?= View::e($fieldLabels[$key] ?? ucfirst(str_replace('_', ' ', (string) $key))) ?></dt>
                        <dd><?php if (is_array($value) || is_object($value)): ?>
                            <details class="technical-details"><summary>Visualizza dettaglio</summary><pre class="json-preview"><?= View::e(json_encode($value, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)) ?></pre></details>
                        <?php else: ?><?= View::e(is_bool($value) ? ($value ? 'Sì' : 'No') : ($value === null || $value === '' ? '—' : $value)) ?><?php endif; ?></dd>
                    </div>
                <?php endforeach; ?>
                </dl>
            </td>
            <td><span class="badge"><?= View::e(View::label($row['status'])) ?></span></td>
            <td class="danger-text"><?= View::e($row['error_message']) ?></td>
        </tr>
    <?php endforeach; ?>
    <?php if (!$rows): ?><tr><td colspan="4"><div class="table-empty"><strong>Nessuna riga disponibile</strong><small>Verifica il contenuto e il formato del file importato.</small></div></td></tr><?php endif; ?>
    </tbody>
</table></section>
