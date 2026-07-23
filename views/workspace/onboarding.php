<?php use Luna\Core\Auth; use Luna\Core\Csrf; use Luna\Core\View; $token = View::e(Csrf::token()); $admin = Auth::isAdmin(); ?>
<section class="page-intro">
    <div><span class="eyebrow">Configurazione guidata</span><h1>Prontezza operativa</h1><p>Una checklist verificabile per portare l’azienda in produzione senza passaggi impliciti.</p></div>
    <div class="readiness-score"><strong><?= (int) $onboarding['percentage'] ?>%</strong><span><?= (int) $onboarding['completed'] ?>/<?= (int) $onboarding['total'] ?> completati</span></div>
</section>
<div class="progress-track" aria-label="Avanzamento configurazione"><span style="width:<?= (int) $onboarding['percentage'] ?>%"></span></div>
<div class="content-grid onboarding-layout">
    <section class="card span-8">
        <div class="card-header"><div><span class="section-kicker">Percorso controllato</span><h2>Checklist aziendale</h2></div></div>
        <div class="onboarding-list">
            <?php foreach ($onboarding['steps'] as $index => $step): ?>
                <article class="onboarding-step<?= $step['completed'] ? ' completed' : '' ?>">
                    <span class="step-index"><?= $step['completed'] ? View::icon('check') : str_pad((string) ($index + 1), 2, '0', STR_PAD_LEFT) ?></span>
                    <div><h3><?= View::e($step['title']) ?></h3><p><?= View::e($step['description']) ?></p><?php if ($step['notes']): ?><small>Nota: <?= View::e($step['notes']) ?></small><?php endif; ?></div>
                    <div class="step-actions">
                        <a class="button ghost compact-button" href="<?= View::e($step['url']) ?>">Apri</a>
                        <?php if ($admin && !$step['automatic']): ?>
                            <form method="post" action="/workspace/onboarding/<?= View::e($step['key']) ?>">
                                <input type="hidden" name="_token" value="<?= $token ?>">
                                <label class="plain-check"><input type="checkbox" name="completed" value="1" <?= $step['completed'] ? 'checked' : '' ?> onchange="this.form.submit()"> Verificato</label>
                            </form>
                        <?php elseif ($step['automatic']): ?><span class="automatic-check">Controllo automatico</span><?php endif; ?>
                    </div>
                </article>
            <?php endforeach; ?>
        </div>
    </section>
    <aside class="card span-4 quality-panel">
        <div class="card-header"><div><span class="section-kicker">Controlli live</span><h2>Qualità dati</h2></div></div>
        <div class="quality-list">
            <?php foreach ($quality as $check): ?>
                <a href="<?= View::e($check['url']) ?>" class="<?= $check['ok'] ? 'ok' : 'attention' ?>">
                    <span><?= $check['ok'] ? View::icon('check') : View::icon('alert') ?></span>
                    <div><strong><?= View::e($check['label']) ?></strong><small><?= $check['ok'] ? 'Nessuna anomalia' : $check['count'] . ' elementi da verificare' ?></small></div>
                    <?= View::icon('chevron') ?>
                </a>
            <?php endforeach; ?>
        </div>
    </aside>
</div>
