<?php use Luna\Core\View; ?>
<section class="error-panel">
    <span class="error-symbol"><?= View::icon(!empty($schemaIssue) ? 'settings' : 'alert') ?></span>
    <span class="eyebrow"><?= !empty($schemaIssue) ? 'Configurazione richiesta' : 'Operazione interrotta' ?></span>
    <h1><?= View::e($title ?? 'Errore') ?></h1>
    <p><?= View::e($message ?? 'Operazione non disponibile.') ?></p>
    <?php if (!empty($missingTables)): ?><p class="technical-note">Tabelle mancanti: <?= View::e(implode(', ', $missingTables)) ?></p><?php endif; ?>
    <?php if (!empty($reference)): ?><p class="error-reference">Codice errore <code><?= View::e($reference) ?></code></p><?php endif; ?>
    <div class="error-actions"><a class="button primary" href="<?= View::e($actionUrl ?? '/dashboard') ?>"><?= View::e($actionLabel ?? 'Torna alla dashboard') ?></a><button class="button ghost" type="button" data-history-back>Torna indietro</button></div>
</section>
