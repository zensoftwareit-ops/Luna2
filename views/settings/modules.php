<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Configurazione · <?= View::e($organizationName) ?></span><h1>Gestione moduli</h1><p>Scegli le aree operative visibili nel menu e disponibili agli utenti di questa azienda.</p></div>
    <div class="inline-actions"><a class="button ghost" href="/settings/company"><?= View::icon('briefcase') ?> Azienda e utenti</a><?php if ($isSuperuser): ?><a class="button ghost" href="/settings/system"><?= View::icon('check') ?> Stato del sistema</a><?php endif; ?></div>
</section>
<form method="post" action="/settings/modules">
    <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
    <div class="module-grid">
        <?php foreach ($features as $key => $feature): ?>
            <?php $ready = $tableStatus[$key]['ready'] ?? false; ?>
            <label class="module-card <?= $feature['enabled'] ? 'enabled' : '' ?> <?= !$feature['licensed'] ? 'not-licensed' : '' ?>">
                <input class="module-checkbox" type="checkbox" name="modules[]" value="<?= View::e($key) ?>" <?= $feature['enabled'] ? 'checked' : '' ?> <?= !$feature['licensed'] ? 'disabled' : '' ?>>
                <span class="module-icon"><?= View::icon($feature['icon']) ?></span>
                <span class="module-copy"><strong><?= View::e($feature['label']) ?></strong><small><?= View::e($feature['description']) ?></small></span>
                <span class="module-state"><span class="switch-ui"></span><small><?= ($feature['availability_reason'] ?? '') === 'HISTORICAL_READONLY' ? 'Storico in sola lettura' : (!$feature['licensed'] ? 'Non incluso nel piano' : ($feature['enabled'] ? 'Attivo' : 'Disattivato')) ?></small></span>
                <span class="readiness <?= $ready ? 'ready' : 'not-ready' ?>"><?= $ready ? View::icon('check') . ' Database pronto' : View::icon('alert') . ' Aggiornamento richiesto' ?></span>
            </label>
        <?php endforeach; ?>
    </div>
    <div class="sticky-save"><div><strong>Configurazione moduli</strong><span>Le modifiche avranno effetto immediato sul menu.</span></div><button class="button primary" type="submit"><?= View::icon('check') ?> Salva configurazione</button></div>
</form>
