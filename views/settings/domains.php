<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Impostazioni aziendali</span><h1>Dominio personalizzato</h1><p>Usa Luna2 da un indirizzo della tua azienda. Inserisci il sottodominio desiderato e segui le indicazioni DNS.</p></div>
    <div class="inline-actions"><a class="button ghost" href="/settings/company"><?= View::icon('briefcase') ?> Azienda e utenti</a></div>
</section>

<div class="health-summary <?= $settings['plesk_configured'] ? 'healthy' : 'attention' ?>">
    <span><?= View::icon($settings['plesk_configured'] ? 'check' : 'alert') ?></span>
    <div><strong><?= $settings['plesk_configured'] ? 'Servizio di attivazione disponibile' : 'Servizio di attivazione temporaneamente non disponibile' ?></strong>
        <p><?= $settings['plesk_configured'] ? 'Luna2 predisporrà l’indirizzo e completerà automaticamente il certificato HTTPS dopo la propagazione DNS.' : 'Contatta l’assistenza Luna2 prima di aggiungere il dominio.' ?></p>
    </div>
</div>

<div class="settings-grid">
    <section class="card settings-card">
        <div class="card-header"><div><span class="section-kicker">Indirizzo aziendale</span><h2>Aggiungi il sottodominio</h2></div><?= View::icon('globe') ?></div>
        <form method="post" action="/settings/domains" class="stack-form compact-form" autocomplete="off">
            <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
            <label><span>Sottodominio completo</span><input name="hostname" type="text" maxlength="253" placeholder="gestionale.tuaazienda.it" required inputmode="url" autocapitalize="none" spellcheck="false"><small>Inserisci soltanto il nome, senza <code>https://</code> e senza percorsi.</small></label>
            <button class="button primary" type="submit" <?= $settings['plesk_configured'] ? '' : 'disabled' ?>>Predisponi dominio</button>
        </form>
    </section>
    <section class="card settings-card">
        <div class="card-header"><div><span class="section-kicker">Procedura guidata</span><h2>Cosa succede dopo</h2></div><?= View::icon('link') ?></div>
        <div class="check-list compact-list">
            <div><span class="check-dot ok"><?= View::icon('check') ?></span><span>1. Luna2 predispone l’indirizzo</span><small>Automatico</small></div>
            <div><span class="check-dot missing">2</span><span>2. Configuri il CNAME</span><small>Nella tua zona DNS</small></div>
            <div><span class="check-dot missing">3</span><span>3. Luna2 verifica DNS e HTTPS</span><small>Automatico</small></div>
        </div>
        <div class="settings-help"><strong>Destinazione CNAME dell’istanza</strong><p><code><?= View::e($settings['cname_target']) ?></code>. Il valore preciso viene ripetuto accanto a ogni dominio configurato.</p></div>
    </section>
</div>

<section class="card data-card">
    <div class="card-header"><div><span class="section-kicker">Accessi aziendali</span><h2>Domini configurati</h2></div><span class="score"><?= count($domains) ?></span></div>
    <div class="table-wrap"><table><thead><tr><th>Dominio</th><th>Record da configurare</th><th>DNS</th><th>HTTPS</th><th>Stato</th><th>Ultimo controllo</th><th class="actions-cell">Azioni</th></tr></thead><tbody>
    <?php foreach ($domains as $domain):
        $active = $domain['status'] === 'ACTIVE';
        $pending = in_array($domain['status'], ['PENDING_DNS','DNS_VERIFIED','SSL_PENDING'], true);
    ?>
        <tr>
            <td><strong><?= View::e($domain['hostname']) ?></strong><?php if ($active): ?><small class="cell-note"><a href="https://<?= View::e($domain['hostname']) ?>" target="_blank" rel="noopener">Apri indirizzo</a></small><?php endif; ?></td>
            <td><div class="dns-record"><small><strong>CNAME</strong> · <?= View::e($domain['hostname']) ?> →</small><span><input id="cname-target-<?= (int) $domain['id'] ?>" value="<?= View::e($domain['cname_target']) ?>" readonly spellcheck="false"><button class="button ghost small" type="button" data-copy-target="cname-target-<?= (int) $domain['id'] ?>">Copia</button></span></div></td>
            <td><?= $domain['dns_verified_at'] ? '<span class="badge status-active">Verificato</span>' : '<span class="badge status-muted">In attesa</span>' ?></td>
            <td><?= $active ? '<span class="badge status-active">Valido</span><small class="cell-note">Scade ' . View::e(View::date($domain['ssl_expires_at'])) . '</small>' : '<span class="badge status-muted">In attesa</span>' ?></td>
            <td><span class="badge <?= $active ? 'status-active' : ($pending ? 'status-muted' : 'status-danger') ?>"><?= View::e(View::label($domain['status'])) ?></span><?php if ($domain['last_error']): ?><small class="cell-note"><?= View::e($domain['last_error']) ?></small><?php endif; ?></td>
            <td><?= View::e($domain['last_checked_at'] ? View::date($domain['last_checked_at']) : 'Mai') ?></td>
            <td class="actions-cell"><div class="inline-actions">
                <form method="post" action="/settings/domains/<?= (int) $domain['id'] ?>/sync" class="inline-form"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="button ghost small" type="submit">Ricontrolla</button></form>
                <details class="inline-confirm"><summary class="button danger small">Rimuovi</summary><form method="post" action="/settings/domains/<?= (int) $domain['id'] ?>/remove" class="inline-form"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><input name="confirmation" placeholder="RIMUOVI" required><button class="button danger small" type="submit">Conferma</button></form></details>
            </div></td>
        </tr>
    <?php endforeach; ?>
    <?php if (!$domains): ?><tr><td colspan="7"><div class="table-empty"><strong>Nessun dominio personalizzato</strong><small>Inserisci il sottodominio desiderato: Luna2 ti mostrerà il record CNAME esatto da configurare.</small></div></td></tr><?php endif; ?>
    </tbody></table></div>
</section>

<details class="card setup-panel">
    <summary><span class="module-icon"><?= View::icon('check') ?></span><span><strong>Cronologia attivazione</strong><small>Ultime verifiche eseguite automaticamente sui domini.</small></span><?= View::icon('chevron', 'setup-chevron') ?></summary>
    <div class="table-wrap"><table><thead><tr><th>Data</th><th>Dominio</th><th>Evento</th><th>Esito</th><th>Dettaglio</th></tr></thead><tbody>
    <?php foreach ($events as $event): ?><tr><td><?= View::e(View::date($event['created_at'])) ?></td><td><?= View::e($event['hostname']) ?></td><td><?= View::e(View::label($event['event_type'])) ?></td><td><?= View::e($event['result']) ?></td><td><?= View::e($event['detail']) ?></td></tr><?php endforeach; ?>
    <?php if (!$events): ?><tr><td colspan="5">Nessun evento.</td></tr><?php endif; ?>
    </tbody></table></div>
</details>
