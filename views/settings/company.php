<?php

use Luna\Core\Csrf;
use Luna\Core\View;

$roleLabels = [
    'OWNER' => 'Titolare', 'ADMIN' => 'Amministratore', 'ACCOUNTANT' => 'Contabile',
    'MANAGER' => 'Responsabile', 'OPERATOR' => 'Operatore', 'SALES' => 'Commerciale',
    'WAREHOUSE' => 'Magazzino', 'HR' => 'Risorse umane', 'VIEWER' => 'Solo lettura',
];
?>
<section class="page-intro compact">
    <div><span class="eyebrow"><?= $isSuperuser ? 'Amministrazione piattaforma' : 'Amministrazione aziendale' ?></span><h1>Azienda e utenti</h1><p>Configura l’azienda operativa e gestisci gli accessi senza utilizzare comandi esterni.</p></div>
    <?php if ($organization): ?><div class="inline-actions"><a class="button ghost" href="/settings/modules"><?= View::icon('settings') ?> Moduli</a><?php if ($isSuperuser): ?><a class="button ghost" href="/settings/system"><?= View::icon('check') ?> Stato sistema</a><?php endif; ?></div><?php endif; ?>
</section>

<?php if ($credentials): ?>
    <section class="credentials-card" role="status">
        <span class="credentials-icon"><?= View::icon('key') ?></span>
        <div><span class="section-kicker">Credenziali mostrate una sola volta</span><h2><?= View::e($credentials['name']) ?></h2><p>Copiale ora in un password manager e consegnale all’utente tramite un canale sicuro.</p></div>
        <dl><div><dt>Email</dt><dd><code><?= View::e($credentials['email']) ?></code></dd></div><div><dt>Password temporanea</dt><dd><code><?= View::e($credentials['password']) ?></code></dd></div></dl>
    </section>
<?php endif; ?>

<?php if ($isSuperuser && $organizations): ?>
    <section class="card organization-switcher">
        <div><span class="section-kicker">Azienda attiva</span><h2><?= View::e($organization['business_name'] ?? 'Seleziona un’azienda') ?></h2><p>Moduli, utenti e dati visualizzati si riferiscono all’azienda selezionata.</p></div>
        <div class="organization-list">
            <?php foreach ($organizations as $item): ?>
                <form method="post" action="/settings/company/<?= (int) $item['id'] ?>/select">
                    <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
                    <button class="organization-chip <?= (int) ($organization['id'] ?? 0) === (int) $item['id'] ? 'active' : '' ?>" type="submit">
                        <span><?= View::e($item['business_name']) ?></span><small><?= View::e($item['vat_number'] ?: ($item['city'] ?: 'Azienda')) ?></small>
                    </button>
                </form>
            <?php endforeach; ?>
        </div>
    </section>
<?php endif; ?>

<?php if ($isSuperuser): ?><details class="card setup-panel" <?= !$organization ? 'open' : '' ?>>
    <summary><span class="module-icon"><?= View::icon('briefcase') ?></span><span><strong><?= $organization ? 'Aggiungi un’altra azienda' : 'Configura la prima azienda' ?></strong><small>Il sistema creerà automaticamente piano dei conti, codici IVA, magazzino e pipeline CRM.</small></span><?= View::icon('chevron', 'setup-chevron') ?></summary>
    <form method="post" action="/settings/company" class="modern-form setup-form">
        <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
        <div class="form-grid">
            <label class="field"><span>Ragione sociale *</span><input name="business_name" required></label>
            <label class="field"><span>Partita IVA</span><input name="vat_number" maxlength="32"></label>
            <label class="field"><span>Codice fiscale</span><input name="tax_code" maxlength="32"></label>
            <label class="field"><span>Regime fiscale</span><input name="fiscal_regime" value="RF01" maxlength="8"></label>
            <label class="field"><span>Codice SDI</span><input name="sdi_code" maxlength="16"></label>
            <label class="field"><span>PEC</span><input type="email" name="pec"></label>
            <label class="field"><span>Email</span><input type="email" name="email"></label>
            <label class="field"><span>Telefono</span><input name="phone"></label>
            <label class="field full-width"><span>Indirizzo</span><input name="address"></label>
            <label class="field"><span>CAP</span><input name="postal_code"></label>
            <label class="field"><span>Città</span><input name="city"></label>
            <label class="field"><span>Provincia</span><input name="province" maxlength="8"></label>
            <label class="field"><span>Paese</span><input name="country_code" value="IT" maxlength="2"></label>
            <label class="field full-width"><span>IBAN</span><input name="iban" maxlength="34"></label>
            <label class="field checkbox-field"><input class="switch-input" type="checkbox" name="withholding_enabled" value="1"><span class="switch-ui"></span><span>Ritenuta d’acconto sulle fatture emesse</span></label>
            <label class="field"><span>Tipo ritenuta</span><input name="withholding_type" value="RT01"></label>
            <label class="field"><span>Aliquota ritenuta %</span><input name="withholding_rate" value="20"></label>
            <label class="field"><span>Imponibile ritenuta %</span><input name="withholding_taxable_percent" value="100"></label>
            <label class="field"><span>Causale pagamento</span><input name="withholding_cause"></label>
        </div>
        <div class="form-actions"><button class="button primary" type="submit"><?= View::icon('check') ?> Crea e inizializza azienda</button></div>
    </form>
</details>
<?php endif; ?>

<?php if ($organization): ?>
<section class="health-summary <?= ($license['effective_status'] ?? '') === 'ACTIVE' ? 'healthy' : 'attention' ?>">
    <span><?= View::icon(($license['effective_status'] ?? '') === 'ACTIVE' ? 'check' : 'alert') ?></span>
    <div><strong>Licenza <?= View::e($license['effective_status'] ?? 'UNCONFIGURED') ?></strong><p>Piano <?= View::e($license['plan_code'] ?? 'non configurato') ?> · <?= (int) $userUsage['used'] ?> utenti attivi<?= $userUsage['max'] !== null ? ' su ' . (int) $userUsage['max'] : '' ?>.</p></div>
</section>
<details class="card setup-panel">
    <summary><span class="module-icon"><?= View::icon('briefcase') ?></span><span><strong>Dati aziendali</strong><small>Aggiorna dati fiscali, recapiti e coordinate dell’azienda.</small></span><?= View::icon('chevron', 'setup-chevron') ?></summary>
    <form method="post" action="/settings/company/update" class="modern-form setup-form">
        <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
        <div class="form-grid">
            <?php foreach ([
                'business_name' => 'Ragione sociale *', 'vat_number' => 'Partita IVA', 'tax_code' => 'Codice fiscale',
                'fiscal_regime' => 'Regime fiscale', 'sdi_code' => 'Codice SDI', 'pec' => 'PEC', 'email' => 'Email',
                'phone' => 'Telefono', 'address' => 'Indirizzo', 'postal_code' => 'CAP', 'city' => 'Città',
                'province' => 'Provincia', 'country_code' => 'Paese', 'iban' => 'IBAN',
            ] as $field => $label): ?>
                <label class="field <?= in_array($field, ['address', 'iban'], true) ? 'full-width' : '' ?>"><span><?= View::e($label) ?></span><input name="<?= View::e($field) ?>" value="<?= View::e($organization[$field] ?? '') ?>" <?= $field === 'business_name' ? 'required' : '' ?>></label>
            <?php endforeach; ?>
            <label class="field checkbox-field"><input class="switch-input" type="checkbox" name="withholding_enabled" value="1" <?= !empty($organization['withholding_enabled']) ? 'checked' : '' ?>><span class="switch-ui"></span><span>Ritenuta d’acconto sulle fatture emesse</span></label>
            <label class="field"><span>Tipo ritenuta</span><input name="withholding_type" value="<?= View::e($organization['withholding_type'] ?? 'RT01') ?>"></label>
            <label class="field"><span>Aliquota ritenuta %</span><input name="withholding_rate" value="<?= View::e($organization['withholding_rate'] ?? '20') ?>"></label>
            <label class="field"><span>Imponibile ritenuta %</span><input name="withholding_taxable_percent" value="<?= View::e($organization['withholding_taxable_percent'] ?? '100') ?>"></label>
            <label class="field"><span>Causale pagamento</span><input name="withholding_cause" value="<?= View::e($organization['withholding_cause'] ?? '') ?>"></label>
        </div>
        <div class="form-actions"><button class="button primary" type="submit"><?= View::icon('check') ?> Salva dati aziendali</button></div>
    </form>
</details>
<?php endif; ?>

<?php if ($organization): ?>
    <div class="settings-grid platform-grid">
        <section class="card settings-card user-create-card">
            <div class="card-header"><div><span class="section-kicker">Nuovo accesso</span><h2>Crea utente</h2></div><?= View::icon('users') ?></div>
            <p>La password temporanea viene generata automaticamente e mostrata una sola volta.</p>
            <form method="post" action="/settings/users" class="stack-form compact-form">
                <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
                <label><span>Nome e cognome</span><input name="name" required></label>
                <label><span>Email</span><input type="email" name="email" required></label>
                <label><span>Ruolo</span><select name="role" required><?php foreach ($roles as $role): ?><option value="<?= View::e($role) ?>"><?= View::e($roleLabels[$role] ?? $role) ?></option><?php endforeach; ?></select></label>
                <button class="button primary" type="submit"><?= View::icon('plus') ?> Crea utente</button>
            </form>
        </section>

        <?php if ($isSuperuser): ?><section class="card settings-card password-card">
            <div class="card-header"><div><span class="section-kicker">Sicurezza</span><h2>Password superuser</h2></div><?= View::icon('key') ?></div>
            <p>Sostituisci la password generata durante la migrazione dopo aver completato il primo accesso.</p>
            <form method="post" action="/settings/security/password" class="stack-form compact-form">
                <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
                <label><span>Password attuale</span><input type="password" name="current_password" autocomplete="current-password" required></label>
                <label><span>Nuova password</span><input type="password" name="password" minlength="14" autocomplete="new-password" required></label>
                <label><span>Conferma password</span><input type="password" name="password_confirmation" minlength="14" autocomplete="new-password" required></label>
                <button class="button ghost" type="submit">Aggiorna password</button>
            </form>
        </section><?php endif; ?>
    </div>

    <section class="card data-card user-management">
        <div class="card-header"><div><span class="section-kicker"><?= View::e($organization['business_name']) ?></span><h2>Utenti aziendali</h2></div><span class="score"><?= (int) $userUsage['used'] ?><?= $userUsage['max'] !== null ? ' / ' . (int) $userUsage['max'] : '' ?></span></div>
        <div class="table-wrap"><table><thead><tr><th>Utente</th><th>Ruolo</th><th>Stato</th><th>Ultimo accesso</th><th class="actions-column">Azioni</th></tr></thead><tbody>
        <?php foreach ($users as $user): ?>
            <tr><td class="primary-cell"><strong><?= View::e($user['name']) ?></strong><small><?= View::e($user['email']) ?></small></td><td><?= View::e($roleLabels[$user['role']] ?? $user['role']) ?></td><td><span class="badge <?= $user['active'] ? 'status-active' : 'status-muted' ?>"><?= $user['active'] ? 'Attivo' : 'Disattivato' ?></span></td><td><?= $user['last_login_at'] ? View::date($user['last_login_at']) : 'Mai' ?></td><td class="row-actions">
                <form method="post" action="/settings/users/<?= (int) $user['id'] ?>/reset-password"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="table-action" type="submit">Nuova password</button></form>
                <form method="post" action="/settings/users/<?= (int) $user['id'] ?>/toggle"><input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="table-action <?= $user['active'] ? 'danger-text' : '' ?>" type="submit"><?= $user['active'] ? 'Disattiva' : 'Riattiva' ?></button></form>
            </td></tr>
        <?php endforeach; ?>
        <?php if (!$users): ?><tr><td colspan="5"><div class="table-empty"><span><?= View::icon('users') ?></span><strong>Nessun utente aziendale</strong><small>Crea il primo titolare o amministratore usando il modulo qui sopra.</small></div></td></tr><?php endif; ?>
        </tbody></table></div>
    </section>
<?php endif; ?>
