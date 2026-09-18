<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Amministrazione tecnica</span><h1>Domini personalizzati</h1><p>Pubblica questa istanza Luna2 su un sottodominio del cliente, con verifica DNS e HTTPS.</p></div>
    <div class="inline-actions"><a class="button ghost" href="/settings/system"><?= View::icon('check') ?> Stato sistema</a><a class="button ghost" href="/settings/license"><?= View::icon('key') ?> Licenza</a></div>
</section>

<div class="health-summary <?= $settings['plesk_configured'] ? 'healthy' : 'attention' ?>">
    <span><?= View::icon($settings['plesk_configured'] ? 'check' : 'alert') ?></span>
    <div><strong><?= $settings['plesk_configured'] ? 'Collegamento Plesk configurato' : 'Collegamento Plesk da configurare' ?></strong>
        <p>Dominio tecnico <strong><?= View::e($settings['canonical_domain']) ?></strong> · destinazione CNAME <code><?= View::e($settings['cname_target']) ?></code>.</p>
    </div>
</div>

<div class="settings-grid">
    <section class="card settings-card">
        <div class="card-header"><div><span class="section-kicker">Nuovo accesso</span><h2>Aggiungi dominio</h2></div><?= View::icon('globe') ?></div>
        <form method="post" action="/settings/domains" class="stack-form compact-form" autocomplete="off">
            <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
            <label><span>Dominio completo</span><input name="hostname" type="text" maxlength="253" placeholder="gestionale.cliente.it" required inputmode="url"></label>
            <button class="button primary" type="submit">Verifica e configura</button>
        </form>
    </section>
    <section class="card settings-card">
        <div class="card-header"><div><span class="section-kicker">DNS del cliente</span><h2>Un solo record CNAME</h2></div><?= View::icon('link') ?></div>
        <div class="check-list compact-list">
            <div><span>Tipo</span><small><code>CNAME</code></small></div>
            <div><span>Nome</span><small>Il sottodominio scelto dal cliente</small></div>
            <div><span>Destinazione</span><small><code><?= View::e($settings['cname_target']) ?></code></small></div>
        </div>
        <p class="muted">Dopo la propagazione, Luna2 crea l’alias web in Plesk. SSL It! deve avere attiva l’opzione “Mantieni protetti i siti web” per emettere e rinnovare automaticamente il certificato.</p>
    </section>
</div>

<section class="card data-card">
    <div class="card-header"><div><span class="section-kicker">Pubblicazione</span><h2>Domini configurati</h2></div><span class="score"><?= count($domains) ?></span></div>
    <div class="table-wrap"><table><thead><tr><th>Dominio</th><th>DNS</th><th>Alias Plesk</th><th>HTTPS</th><th>Stato</th><th>Ultimo controllo</th><th class="actions-cell">Azioni</th></tr></thead><tbody>
    <?php foreach ($domains as $domain):
        $active = $domain['status'] === 'ACTIVE';
        $pending = in_array($domain['status'], ['PENDING_DNS','DNS_VERIFIED','SSL_PENDING'], true);
    ?>
        <tr>
            <td><strong><?= View::e($domain['hostname']) ?></strong><?php if ($domain['last_error']): ?><small class="cell-note"><?= View::e($domain['last_error']) ?></small><?php endif; ?></td>
            <td><?= $domain['dns_verified_at'] ? '<span class="badge status-active">Verificato</span>' : '<span class="badge status-muted">In attesa</span>' ?></td>
            <td><?= $domain['alias_provisioned_at'] ? '<span class="badge status-active">Creato</span>' : '<span class="badge status-muted">In attesa</span>' ?></td>
            <td><?= $active ? '<span class="badge status-active">Valido</span><small class="cell-note">Scade ' . View::e(View::date($domain['ssl_expires_at'])) . '</small>' : '<span class="badge status-muted">In attesa</span>' ?></td>
            <td><span class="badge <?= $active ? 'status-active' : ($pending ? 'status-muted' : 'status-danger') ?>"><?= View::e(View::label($domain['status'])) ?></span></td>
            <td><?= View::e($domain['last_checked_at'] ? View::date($domain['last_checked_at']) : 'Mai') ?></td>
            <td class="actions-cell"><div class="inline-actions">
                <form method="post" action="/settings/domains/<?= (int) $domain['id'] ?>/sync" class="inline-form"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="button ghost small" type="submit">Ricontrolla</button></form>
                <details class="inline-confirm"><summary class="button danger small">Rimuovi</summary><form method="post" action="/settings/domains/<?= (int) $domain['id'] ?>/remove" class="inline-form"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><input name="confirmation" placeholder="RIMUOVI" required><button class="button danger small" type="submit">Conferma</button></form></details>
            </div></td>
        </tr>
    <?php endforeach; ?>
    <?php if (!$domains): ?><tr><td colspan="7"><div class="table-empty"><strong>Nessun dominio personalizzato</strong><small>Aggiungi il primo dominio dopo aver predisposto il CNAME.</small></div></td></tr><?php endif; ?>
    </tbody></table></div>
</section>

<details class="card setup-panel">
    <summary><span class="module-icon"><?= View::icon('check') ?></span><span><strong>Registro tecnico</strong><small>Ultime verifiche DNS, operazioni Plesk e controlli TLS.</small></span><?= View::icon('chevron', 'setup-chevron') ?></summary>
    <div class="table-wrap"><table><thead><tr><th>Data</th><th>Dominio</th><th>Evento</th><th>Esito</th><th>Dettaglio</th></tr></thead><tbody>
    <?php foreach ($events as $event): ?><tr><td><?= View::e(View::date($event['created_at'])) ?></td><td><?= View::e($event['hostname']) ?></td><td><?= View::e(View::label($event['event_type'])) ?></td><td><?= View::e($event['result']) ?></td><td><?= View::e($event['detail']) ?></td></tr><?php endforeach; ?>
    <?php if (!$events): ?><tr><td colspan="5">Nessun evento.</td></tr><?php endif; ?>
    </tbody></table></div>
</details>
