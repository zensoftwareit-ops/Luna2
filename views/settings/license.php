<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Amministrazione tecnica</span><h1>Licenza installazione</h1><p>Attivazione, sincronizzazione e diagnostica della licenza Luna Commerce.</p></div>
    <div class="inline-actions"><a class="button ghost" href="/settings/system"><?= View::icon('check') ?> Stato sistema</a><a class="button ghost" href="/settings/modules"><?= View::icon('settings') ?> Moduli</a></div>
</section>

<div class="health-summary <?= ($license['effective_status'] ?? '') === 'ACTIVE' ? 'healthy' : 'attention' ?>">
    <span><?= View::icon(($license['effective_status'] ?? '') === 'ACTIVE' ? 'check' : 'alert') ?></span>
    <div><strong>Stato <?= View::e($license['effective_status'] ?? 'UNCONFIGURED') ?></strong><p>Enforcement <?= View::e($enforcement) ?> · piano <?= View::e($license['plan_code'] ?? 'non configurato') ?> · ultimo controllo <?= View::e($license['last_validated_at'] ?? 'mai') ?>.</p></div>
</div>

<div class="settings-grid">
    <section class="card settings-card">
        <div class="card-header"><div><span class="section-kicker">Installazione</span><h2>Identità e binding</h2></div><?= View::icon('key') ?></div>
        <div class="check-list compact-list">
            <div><span>Instance ID</span><small><code><?= View::e($identity['instance_id'] ?? 'Da generare') ?></code></small></div>
            <div><span>UUID</span><small><code><?= View::e($identity['installation_uuid'] ?? 'Da generare') ?></code></small></div>
            <div><span>Dominio</span><small><?= View::e($identity['canonical_domain'] ?? 'Non configurato') ?></small></div>
            <div><span>Fingerprint</span><small><code><?= View::e($license['license_key_fingerprint'] ?? '—') ?></code></small></div>
            <div><span>Fine tolleranza</span><small><?= View::e($license['grace_until'] ?? '—') ?></small></div>
        </div>
    </section>

    <section class="card settings-card">
        <div class="card-header"><div><span class="section-kicker">Attivazione sicura</span><h2>Chiave licenza</h2></div><?= View::icon('lock') ?></div>
        <p>La chiave sarà associata a <strong><?= View::e($organizationName ?: 'nessuna azienda selezionata') ?></strong>, quindi cifrata con APP_KEY. Non comparirà nei log o nelle pagine successive.</p>
        <form method="post" action="/settings/license/activate" class="stack-form compact-form" autocomplete="off">
            <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
            <label><span>Chiave licenza</span><input type="password" name="license_key" minlength="16" maxlength="500" required autocomplete="new-password"></label>
            <button class="button primary" type="submit"><?= View::icon('key') ?> Attiva e verifica</button>
        </form>
        <?php if (!empty($license['license_key_fingerprint'])): ?>
            <form method="post" action="/settings/license/sync" class="inline-form"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="button ghost" type="submit">Sincronizza ora</button></form>
        <?php endif; ?>
    </section>
</div>

<?php if (!empty($license['license_key_fingerprint'])): ?>
<details class="card setup-panel">
    <summary><span class="module-icon"><?= View::icon('alert') ?></span><span><strong>Disattiva licenza</strong><small>Libera il binding remoto e porta questa installazione in modalità limitata.</small></span><?= View::icon('chevron', 'setup-chevron') ?></summary>
    <form method="post" action="/settings/license/deactivate" class="inline-form">
        <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
        <label class="field"><span>Conferma scrivendo DISATTIVA</span><input name="confirmation" required></label>
        <button class="button danger" type="submit">Disattiva</button>
    </form>
</details>
<?php endif; ?>

<section class="card data-card">
    <div class="card-header"><div><span class="section-kicker">Audit tecnico</span><h2>Ultime sincronizzazioni</h2></div><span class="score"><?= count($logs) ?></span></div>
    <div class="table-wrap"><table><thead><tr><th>Data</th><th>Operazione</th><th>Esito</th><th>HTTP</th><th>Request ID</th><th>Dettaglio</th></tr></thead><tbody>
    <?php foreach ($logs as $log): ?><tr><td><?= View::date($log['created_at']) ?></td><td><?= View::e($log['operation']) ?></td><td><span class="badge <?= $log['result'] === 'SUCCESS' ? 'status-active' : 'status-muted' ?>"><?= View::e($log['result']) ?></span></td><td><?= View::e($log['http_status'] ?: '—') ?></td><td><code><?= View::e($log['request_id']) ?></code></td><td><?= View::e($log['error_message'] ?? '—') ?></td></tr><?php endforeach; ?>
    <?php if (!$logs): ?><tr><td colspan="6"><div class="table-empty"><strong>Nessuna sincronizzazione</strong><small>Attiva una licenza per iniziare.</small></div></td></tr><?php endif; ?>
    </tbody></table></div>
</section>
