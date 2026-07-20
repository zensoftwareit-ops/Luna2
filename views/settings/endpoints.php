<?php

use Luna\Core\Csrf;
use Luna\Core\View;

$token = View::e(Csrf::token());
$endpoint = $selectedEndpoint ?? [];
?>
<section class="page-intro compact"><div><span class="eyebrow">Integrazioni · preparazione API</span><h1>Endpoint fatturazione elettronica</h1><p>Configura gli indirizzi che il futuro servizio API userà per invio, ricezione, stato e webhook.</p></div><a class="button ghost" href="/accounting/setup">Configurazione contabile</a></section>
<div class="system-banner neutral-banner"><span class="system-banner-icon"><?= View::icon('alert') ?></span><div><strong>Configurazione senza connessione</strong><span>Luna2 salva esclusivamente URL e riferimenti sicuri alle credenziali. Non esegue chiamate, non verifica la raggiungibilità e non memorizza token o password.</span></div></div>
<section class="card">
    <div class="card-header"><div><span class="section-kicker"><?= $endpoint ? 'Modifica canale' : 'Nuovo canale' ?></span><h2>Configurazione endpoint</h2></div><?php if ($endpoint): ?><a class="button ghost" href="/settings/endpoints">Nuova configurazione</a><?php endif; ?></div>
    <form method="post" action="/settings/endpoints" class="modern-form">
        <input type="hidden" name="_token" value="<?= $token ?>">
        <input type="hidden" name="id" value="<?= (int) ($endpoint['id'] ?? 0) ?>">
        <div class="form-grid">
            <label class="field"><span>Chiave servizio</span><input name="service_key" value="<?= View::e($endpoint['service_key'] ?? 'EINVOICE_SDI') ?>" required><small>Identificatore stabile, es. EINVOICE_SDI.</small></label>
            <label class="field"><span>Nome</span><input name="display_name" value="<?= View::e($endpoint['display_name'] ?? 'Servizio fatturazione elettronica') ?>" required></label>
            <label class="field"><span>Ambiente</span><select name="environment"><?php foreach (['TEST' => 'Test', 'PRODUCTION' => 'Produzione'] as $key => $label): ?><option value="<?= $key ?>" <?= ($endpoint['environment'] ?? 'TEST') === $key ? 'selected' : '' ?>><?= $label ?></option><?php endforeach; ?></select></label>
            <label class="field full-width"><span>Base URL</span><input type="url" name="base_url" value="<?= View::e($endpoint['base_url'] ?? '') ?>" placeholder="https://api.example.it/v1" required></label>
            <label class="field"><span>Percorso invio</span><input name="send_path" value="<?= View::e($endpoint['send_path'] ?? '') ?>" placeholder="/invoices/send"></label>
            <label class="field"><span>Percorso ricezione</span><input name="receive_path" value="<?= View::e($endpoint['receive_path'] ?? '') ?>" placeholder="/invoices/receive"></label>
            <label class="field"><span>Percorso stato</span><input name="status_path" value="<?= View::e($endpoint['status_path'] ?? '') ?>" placeholder="/invoices/{id}/status"></label>
            <label class="field"><span>Percorso webhook</span><input name="webhook_path" value="<?= View::e($endpoint['webhook_path'] ?? '') ?>" placeholder="/webhooks/sdi"></label>
            <label class="field"><span>Autenticazione futura</span><select name="auth_type"><?php foreach (['NONE', 'BEARER', 'API_KEY', 'OAUTH2', 'MTLS', 'CUSTOM'] as $type): ?><option value="<?= $type ?>" <?= ($endpoint['auth_type'] ?? 'NONE') === $type ? 'selected' : '' ?>><?= $type ?></option><?php endforeach; ?></select></label>
            <label class="field"><span>Riferimento segreto</span><input name="secret_reference" value="<?= View::e($endpoint['secret_reference'] ?? '') ?>" placeholder="ENV:LUNA_EINVOICE_TOKEN"><small>Solo ENV:NOME o vault://…; mai il valore reale.</small></label>
            <label class="field"><span>Timeout secondi</span><input type="number" name="timeout_seconds" min="1" max="300" value="<?= (int) ($endpoint['timeout_seconds'] ?? 30) ?>"></label>
            <label class="field full-width"><span>Header non sensibili (JSON)</span><textarea name="header_json" rows="3" placeholder='{"Accept":"application/json"}'><?= View::e($endpoint['header_json'] ?? '') ?></textarea></label>
            <label class="field full-width"><span>Note</span><textarea name="notes" rows="3"><?= View::e($endpoint['notes'] ?? '') ?></textarea></label>
            <label class="checkbox-field"><input class="switch-input" type="checkbox" name="verify_tls" <?= !$endpoint || !empty($endpoint['verify_tls']) ? 'checked' : '' ?>><span class="switch-ui"></span> Verifica TLS</label>
            <label class="checkbox-field"><input class="switch-input" type="checkbox" name="enabled" <?= !empty($endpoint['enabled']) ? 'checked' : '' ?>><span class="switch-ui"></span> Pronto per uso futuro</label>
        </div>
        <div class="form-actions"><button class="button primary" type="submit"><?= $endpoint ? 'Aggiorna configurazione' : 'Salva configurazione' ?></button></div>
    </form>
</section>
<section class="card data-card">
    <div class="card-header"><div><span class="section-kicker">Configurazioni memorizzate</span><h2>Canali disponibili</h2></div></div>
    <div class="table-wrap"><table><thead><tr><th>Servizio</th><th>Ambiente</th><th>Base URL</th><th>Autenticazione</th><th>Segreto</th><th>TLS</th><th>Stato</th><th></th></tr></thead><tbody>
    <?php foreach ($endpoints as $row): ?><tr><td class="primary-cell"><?= View::e($row['display_name']) ?><small><?= View::e($row['service_key']) ?></small></td><td><?= View::e($row['environment']) ?></td><td class="technical-note"><?= View::e($row['base_url']) ?></td><td><?= View::e($row['auth_type']) ?></td><td><?= View::e($row['secret_reference'] ?? '—') ?></td><td><?= !empty($row['verify_tls']) ? 'Sì' : 'No' ?></td><td><span class="badge status-<?= !empty($row['enabled']) ? 'active' : 'muted' ?>"><?= !empty($row['enabled']) ? 'PRONTO' : 'DISATTIVO' ?></span></td><td><a class="table-action" href="/settings/endpoints?endpoint_id=<?= (int) $row['id'] ?>">Modifica</a></td></tr><?php endforeach; ?>
    <?php if (!$endpoints): ?><tr><td colspan="8"><div class="table-empty"><strong>Nessun endpoint</strong><small>Aggiungi la configurazione del futuro servizio API.</small></div></td></tr><?php endif; ?>
    </tbody></table></div>
</section>
