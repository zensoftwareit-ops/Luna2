<?php use Luna\Core\View; ?>
<section class="empty-state">
    <span class="empty-icon">!</span>
    <h1><?= View::e($title ?? 'Errore') ?></h1>
    <p><?= View::e($message ?? 'Operazione non disponibile.') ?></p>
    <a class="button" href="/dashboard">Torna alla dashboard</a>
</section>
