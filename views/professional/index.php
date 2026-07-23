<?php
use Luna\Core\Csrf;
use Luna\Core\View;
$token = View::e(Csrf::token());
$openQuality = count(array_filter($quality, static fn (array $row): bool => !$row['ok']));
$proposed = count($suggestions);
?>
<section class="page-intro professional-intro">
    <div><span class="eyebrow">Controllo amministrativo</span><h1>Centro professionale</h1><p>Stampe numerate, fascicoli fiscali, qualità dati e riconciliazione bancaria in un’unica area controllata.</p></div>
    <div class="intro-status-stack"><span class="status-chip <?= $openQuality ? 'warning' : 'success' ?>"><?= $openQuality ? $openQuality . ' controlli aperti' : 'Dati coerenti' ?></span><span class="status-chip neutral"><?= $proposed ?> riconciliazioni proposte</span></div>
</section>
<nav class="section-tabs sticky-tabs" aria-label="Sezioni centro professionale">
    <a href="#quality">Qualità dati</a><a href="#prints">Stampe ufficiali</a><a href="#filings">Adempimenti</a><a href="#banking">Banche</a>
</nav>

<section class="card" id="quality">
    <div class="card-header"><div><span class="section-kicker">Pre-flight</span><h2>Controlli di coerenza</h2></div><a class="text-link" href="/workspace/onboarding">Checklist completa <?= View::icon('chevron') ?></a></div>
    <div class="quality-grid">
        <?php foreach ($quality as $check): ?><a href="<?= View::e($check['url']) ?>" class="quality-tile <?= $check['ok'] ? 'ok' : 'attention' ?>"><span><?= $check['ok'] ? View::icon('check') : View::icon('alert') ?></span><div><strong><?= View::e($check['label']) ?></strong><small><?= $check['ok'] ? 'OK' : $check['count'] . ' da verificare' ?></small></div></a><?php endforeach; ?>
    </div>
</section>

<section class="professional-section" id="prints">
    <div class="section-heading"><div><span class="section-kicker">Archivio immutabile</span><h2>Stampe numerate e validate</h2><p>PDF con progressivo, hash SHA-256, validazione e blocco definitivo.</p></div></div>
    <div class="content-grid">
        <section class="card span-4">
            <div class="card-header"><div><h3>Genera stampa</h3><p class="card-description">Il contenuto viene prodotto dai dati contabilizzati.</p></div></div>
            <form method="post" action="/professional/prints" class="modern-form">
                <input type="hidden" name="_token" value="<?= $token ?>">
                <label class="field"><span>Tipo registro</span><select name="print_type" required><?php foreach ($printTypes as $key => $label): ?><option value="<?= View::e($key) ?>"><?= View::e($label) ?></option><?php endforeach; ?></select></label>
                <div class="form-grid"><label class="field"><span>Dal</span><input type="date" name="period_start" value="<?= date('Y-01-01') ?>" required></label><label class="field"><span>Al</span><input type="date" name="period_end" value="<?= date('Y-12-31') ?>" required></label></div>
                <label class="field"><span>Note interne</span><textarea name="notes" rows="3"></textarea></label>
                <button class="button primary full-button" type="submit">Genera PDF numerato</button>
            </form>
        </section>
        <section class="card data-card span-8">
            <div class="card-header"><div><h3>Archivio stampe</h3><p class="card-description"><?= count($prints) ?> elaborazioni recenti.</p></div></div>
            <div class="table-wrap"><table><thead><tr><th>Progressivo</th><th>Stampa</th><th>Periodo</th><th>Righe</th><th>Stato</th><th>Azioni</th></tr></thead><tbody>
                <?php foreach ($prints as $row): ?><tr>
                    <td class="primary-cell">#<?= str_pad((string) $row['sequence_number'], 6, '0', STR_PAD_LEFT) ?></td><td><?= View::e($row['title']) ?><small class="cell-subtitle"><?= View::e($row['file_sha256'] ? substr((string) $row['file_sha256'], 0, 12) . '…' : 'PDF non disponibile') ?></small></td><td><?= View::date($row['period_start']) ?> – <?= View::date($row['period_end']) ?></td><td><?= (int) $row['row_count'] ?></td><td><span class="badge status-<?= View::e(strtolower((string) $row['status'])) ?>"><?= View::e($row['status']) ?></span></td>
                    <td class="row-actions"><?php if ($row['file_path']): ?><a class="table-action" href="/professional/prints/<?= (int) $row['id'] ?>/download">PDF</a><?php endif; ?>
                        <?php if ($row['status'] === 'GENERATED'): ?><form method="post" action="/professional/prints/<?= (int) $row['id'] ?>/validate" class="micro-form"><input type="hidden" name="_token" value="<?= $token ?>"><input name="professional_validation_reference" placeholder="Rif. validazione" aria-label="Riferimento validazione stampa" required><button class="table-action" type="submit">Valida</button></form><?php endif; ?>
                        <?php if ($row['status'] === 'VALIDATED'): ?><form method="post" action="/professional/prints/<?= (int) $row['id'] ?>/lock" data-confirm="Dopo il blocco la stampa resterà immutabile. Continuare?"><input type="hidden" name="_token" value="<?= $token ?>"><button class="table-action" type="submit">Blocca</button></form><?php endif; ?>
                    </td>
                </tr><?php endforeach; ?>
                <?php if (!$prints): ?><tr><td colspan="6"><div class="table-empty"><strong>Nessuna stampa archiviata</strong><small>Genera il primo registro numerato.</small></div></td></tr><?php endif; ?>
            </tbody></table></div>
        </section>
    </div>
</section>

<section class="professional-section" id="filings">
    <div class="section-heading"><div><span class="section-kicker">Workflow controllato</span><h2>Fascicoli adempimenti</h2><p>Dossier verificabili; la trasmissione richiede validazione ed endpoint esterno configurato.</p></div></div>
    <div class="content-grid">
        <section class="card span-4">
            <form method="post" action="/professional/filings" class="modern-form">
                <input type="hidden" name="_token" value="<?= $token ?>">
                <label class="field"><span>Adempimento</span><select name="filing_type" required><?php foreach ($filingTypes as $key => $label): ?><option value="<?= View::e($key) ?>"><?= View::e($label) ?></option><?php endforeach; ?></select></label>
                <div class="form-grid"><label class="field"><span>Anno</span><input type="number" name="period_year" value="<?= date('Y') ?>" min="2000" max="<?= date('Y') + 1 ?>" required></label><label class="field"><span>Periodo</span><input name="period_code" placeholder="es. Q1, 01"></label></div>
                <label class="field"><span>Versione schema</span><input name="schema_version" placeholder="Versione dichiarata"></label>
                <label class="field"><span>Note</span><textarea name="notes" rows="3"></textarea></label>
                <button class="button primary full-button" type="submit">Crea dossier</button>
            </form>
        </section>
        <section class="card data-card span-8">
            <div class="table-wrap"><table><thead><tr><th>Tipo</th><th>Periodo</th><th>Anomalie</th><th>Stato</th><th>Validazione / invio</th></tr></thead><tbody>
                <?php foreach ($filings as $row): ?><tr>
                    <td class="primary-cell"><?= View::e($filingTypes[$row['filing_type']] ?? $row['filing_type']) ?><small class="cell-subtitle"><?= View::e($row['schema_version'] ?: 'Schema non indicato') ?></small></td><td><?= (int) $row['period_year'] ?><?= $row['period_code'] ? ' · ' . View::e($row['period_code']) : '' ?></td><td><span class="badge <?= (int) $row['anomaly_count'] ? 'status-overdue' : 'status-active' ?>"><?= (int) $row['anomaly_count'] ?></span></td><td><span class="badge status-<?= View::e(strtolower((string) $row['status'])) ?>"><?= View::e($row['status']) ?></span></td>
                    <td class="filing-actions"><a class="table-action" href="/professional/filings/<?= (int) $row['id'] ?>/download">Dossier</a>
                        <?php if ($row['status'] === 'DRAFT'): ?><form method="post" action="/professional/filings/<?= (int) $row['id'] ?>/status"><input type="hidden" name="_token" value="<?= $token ?>"><input type="hidden" name="target_status" value="REVIEW"><button class="table-action" type="submit">Invia in revisione</button></form><?php endif; ?>
                        <?php if ($row['status'] === 'REVIEW'): ?><form method="post" action="/professional/filings/<?= (int) $row['id'] ?>/status" class="micro-form"><input type="hidden" name="_token" value="<?= $token ?>"><input type="hidden" name="target_status" value="VALIDATED"><input name="professional_validation_reference" placeholder="Rif. professionista" aria-label="Riferimento validazione professionista" required><button class="table-action" type="submit">Valida</button></form><?php endif; ?>
                        <?php if ($row['status'] === 'VALIDATED'): ?><form method="post" action="/professional/filings/<?= (int) $row['id'] ?>/status"><input type="hidden" name="_token" value="<?= $token ?>"><input type="hidden" name="target_status" value="READY"><button class="table-action" type="submit">Rendi pronto</button></form><?php endif; ?>
                        <?php if ($row['status'] === 'READY'): ?><form method="post" action="/professional/filings/<?= (int) $row['id'] ?>/status" class="micro-form stack"><input type="hidden" name="_token" value="<?= $token ?>"><input type="hidden" name="target_status" value="SUBMITTED"><select name="submission_endpoint_id" aria-label="Endpoint di trasmissione" required><option value="">Endpoint…</option><?php foreach ($endpoints as $endpoint): ?><option value="<?= (int) $endpoint['id'] ?>"><?= View::e($endpoint['name']) ?></option><?php endforeach; ?></select><input name="external_reference" placeholder="Riferimento invio" aria-label="Riferimento esterno invio" required><button class="table-action" type="submit">Registra invio</button></form><?php endif; ?>
                        <?php if ($row['status'] === 'SUBMITTED'): ?><form method="post" action="/professional/filings/<?= (int) $row['id'] ?>/status" class="micro-form"><input type="hidden" name="_token" value="<?= $token ?>"><input name="external_reference" placeholder="Protocollo esito" aria-label="Protocollo esito esterno"><button class="table-action" name="target_status" value="ACCEPTED" type="submit">Accettato</button><button class="table-action danger-text" name="target_status" value="REJECTED" type="submit">Scartato</button></form><?php endif; ?>
                    </td>
                </tr><?php endforeach; ?>
                <?php if (!$filings): ?><tr><td colspan="5"><div class="table-empty"><strong>Nessun fascicolo</strong><small>Crea un dossier di controllo per iniziare.</small></div></td></tr><?php endif; ?>
            </tbody></table></div>
        </section>
    </div>
</section>

<section class="professional-section" id="banking">
    <div class="section-heading"><div><span class="section-kicker">Tesoreria assistita</span><h2>Import e riconciliazione bancaria</h2><p>CSV, CAMT.053 e MT940 con checksum, deduplica e proposte motivate.</p></div></div>
    <div class="content-grid">
        <section class="card span-4">
            <form method="post" action="/professional/bank-statements" enctype="multipart/form-data" class="modern-form">
                <input type="hidden" name="_token" value="<?= $token ?>">
                <label class="field"><span>Conto bancario</span><select name="bank_account_id" required><option value="">Seleziona…</option><?php foreach ($bankAccounts as $bank): ?><option value="<?= (int) $bank['id'] ?>"><?= View::e($bank['name'] . ' · ' . $bank['iban']) ?></option><?php endforeach; ?></select></label>
                <label class="field"><span>Formato</span><select name="source_format" required><option value="CSV">CSV bancario</option><option value="CAMT053">CAMT.053 XML</option><option value="MT940">MT940</option></select></label>
                <label class="file-drop"><input type="file" name="statement" accept=".csv,.xml,.sta,.mt940,.txt" required><?= View::icon('upload') ?><strong>Seleziona estratto conto</strong><small>Il file originale sarà conservato fuori dalla webroot.</small></label>
                <button class="button primary full-button" type="submit">Importa e analizza</button>
            </form>
        </section>
        <section class="card data-card span-8">
            <div class="card-header"><div><h3>Import recenti</h3><p class="card-description">Controllo checksum e righe elaborate.</p></div><form method="post" action="/professional/reconciliation/suggest"><input type="hidden" name="_token" value="<?= $token ?>"><button class="button ghost compact-button" type="submit">Ricalcola proposte</button></form></div>
            <div class="table-wrap"><table><thead><tr><th>Data</th><th>Conto</th><th>File</th><th>Esito</th><th>Righe</th></tr></thead><tbody><?php foreach ($imports as $row): ?><tr><td><?= View::date($row['created_at']) ?></td><td><?= View::e($row['bank_name']) ?></td><td class="primary-cell"><?= View::e($row['original_filename']) ?><small class="cell-subtitle"><?= View::e($row['source_format']) ?> · <?= View::e(substr((string) $row['checksum_sha256'], 0, 10)) ?>…</small></td><td><span class="badge status-<?= strtolower((string) $row['status']) ?>"><?= View::e($row['status']) ?></span></td><td><?= (int) $row['imported_count'] ?>/<?= (int) $row['rows_count'] ?><?php if ((int) $row['duplicate_count']): ?><small class="cell-subtitle"><?= (int) $row['duplicate_count'] ?> duplicati</small><?php endif; ?></td></tr><?php endforeach; ?><?php if (!$imports): ?><tr><td colspan="5"><div class="table-empty">Nessun estratto importato.</div></td></tr><?php endif; ?></tbody></table></div>
        </section>
    </div>
    <section class="card data-card reconciliation-suggestions">
        <div class="card-header"><div><span class="section-kicker">Matching assistito</span><h3>Proposte di riconciliazione</h3></div><span class="result-count"><?= count($suggestions) ?></span></div>
        <div class="table-wrap"><table><thead><tr><th>Affidabilità</th><th>Movimento</th><th>Pagamento proposto</th><th>Importo</th><th>Decisione</th></tr></thead><tbody>
            <?php foreach ($suggestions as $row): $reasons = json_decode((string) $row['reason_json'], true) ?: []; ?><tr><td><strong class="confidence-score"><?= number_format((float) $row['confidence_score'], 0) ?>%</strong><small class="cell-subtitle"><?= View::e(implode(' · ', $reasons)) ?></small></td><td><?= View::date($row['booking_date']) ?><strong class="cell-block"><?= View::e($row['transaction_counterparty'] ?: $row['transaction_description']) ?></strong></td><td><?= View::date($row['payment_date']) ?><strong class="cell-block"><?= View::e($row['party_name'] ?: $row['reference_number']) ?></strong></td><td class="primary-cell"><?= View::money($row['transaction_amount']) ?></td><td><form method="post" action="/professional/reconciliation/<?= (int) $row['id'] ?>/review" class="inline-actions"><input type="hidden" name="_token" value="<?= $token ?>"><button class="button compact-button" name="decision" value="accept" type="submit">Accetta</button><button class="button ghost compact-button" name="decision" value="reject" type="submit">Scarta</button></form></td></tr><?php endforeach; ?>
            <?php if (!$suggestions): ?><tr><td colspan="5"><div class="table-empty"><strong>Nessuna proposta aperta</strong><small>Importa un estratto o ricalcola il matching.</small></div></td></tr><?php endif; ?>
        </tbody></table></div>
    </section>
</section>
