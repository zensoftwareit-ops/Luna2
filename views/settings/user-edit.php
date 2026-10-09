<?php

use Luna\Core\Csrf;
use Luna\Core\View;

$roleLabels = [
    'OWNER' => 'Titolare', 'ADMIN' => 'Amministratore', 'ACCOUNTANT' => 'Contabile',
    'MANAGER' => 'Responsabile', 'OPERATOR' => 'Operatore', 'SALES' => 'Commerciale',
    'WAREHOUSE' => 'Magazzino', 'HR' => 'Risorse umane', 'VIEWER' => 'Solo lettura',
];
$roleDescriptions = [
    'OWNER' => 'Controllo completo dell’azienda, inclusi utenti e configurazione.',
    'ADMIN' => 'Amministrazione operativa, utenti e configurazione aziendale.',
    'MANAGER' => 'Gestione dei processi aziendali autorizzati.',
    'OPERATOR' => 'Operatività quotidiana secondo i permessi configurati.',
    'ACCOUNTANT' => 'Contabilità, IVA, prima nota e relativi adempimenti.',
    'SALES' => 'Clienti, offerte, ordini e attività commerciali.',
    'WAREHOUSE' => 'Magazzino, prodotti, movimenti e logistica.',
    'HR' => 'Personale, presenze, ferie e paghe.',
    'VIEWER' => 'Consultazione senza funzioni amministrative.',
];
?>
<section class="page-intro compact">
    <div><a class="back-link" href="/settings/company">← Azienda e utenti</a><span class="eyebrow">Amministrazione aziendale</span><h1>Modifica utente</h1><p>Aggiorna i dati di accesso e assegna il ruolo corretto.</p></div>
</section>

<section class="card form-card">
    <div class="form-section-head">
        <div><h2><?= View::e($user['name']) ?></h2><p><?= $user['active'] ? 'Account attivo' : 'Account disattivato' ?> · ultimo accesso <?= $user['last_login_at'] ? View::date($user['last_login_at']) : 'mai' ?></p></div>
        <span class="badge <?= $user['active'] ? 'status-active' : 'status-muted' ?>"><?= $user['active'] ? 'Attivo' : 'Disattivato' ?></span>
    </div>
    <form method="post" action="/settings/users/<?= (int) $user['id'] ?>/update" class="modern-form">
        <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
        <div class="form-grid">
            <label class="field"><span>Nome e cognome *</span><input name="name" value="<?= View::e($user['name']) ?>" required></label>
            <label class="field"><span>Email *</span><input type="email" name="email" value="<?= View::e($user['email']) ?>" required></label>
            <label class="field"><span>Ruolo *</span>
                <?php if ($isCurrentUser): ?>
                    <select disabled><option><?= View::e($roleLabels[$user['role']] ?? $user['role']) ?></option></select>
                    <input type="hidden" name="role" value="<?= View::e($user['role']) ?>">
                    <small>Il proprio ruolo deve essere modificato da un altro Titolare o Amministratore.</small>
                <?php else: ?>
                    <select name="role" required>
                        <?php foreach ($roles as $role): ?><option value="<?= View::e($role) ?>" <?= $role === $user['role'] ? 'selected' : '' ?>><?= View::e($roleLabels[$role] ?? $role) ?></option><?php endforeach; ?>
                    </select>
                <?php endif; ?>
            </label>
            <div class="full-width">
                <span class="section-kicker">Significato dei ruoli</span>
                <div class="role-help-grid">
                    <?php foreach ($roles as $role): ?><p><strong><?= View::e($roleLabels[$role] ?? $role) ?>:</strong> <?= View::e($roleDescriptions[$role] ?? '') ?></p><?php endforeach; ?>
                </div>
            </div>
        </div>
        <div class="form-actions"><a class="button ghost" href="/settings/company">Annulla</a><button class="button primary" type="submit"><?= View::icon('check') ?> Salva modifiche</button></div>
    </form>
</section>
