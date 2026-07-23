<?php use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow">Workspace</span><h1>Ricerca globale</h1><p>Clienti, fornitori, prodotti, documenti, registrazioni e attività in un solo punto.</p></div>
</section>
<form method="get" action="/workspace/search" class="global-search-page" role="search">
    <?= View::icon('search') ?>
    <input type="search" name="q" value="<?= View::e($query) ?>" placeholder="Cerca numero documento, ragione sociale, codice, descrizione…" autofocus minlength="2" autocomplete="off">
    <button class="button primary" type="submit">Cerca</button>
</form>
<?php if ($query !== '' && mb_strlen($query) < 2): ?><div class="alert alert-warning"><?= View::icon('alert') ?><span>Inserisci almeno due caratteri.</span></div><?php endif; ?>
<?php if ($results): ?>
    <div class="search-result-grid">
        <?php foreach ($results as $area): ?>
            <section class="card search-result-card">
                <div class="card-header"><div><span class="section-kicker"><?= View::icon($area['icon']) ?> Archivio</span><h2><?= View::e($area['label']) ?></h2></div><span class="result-count"><?= count($area['rows']) ?></span></div>
                <div class="search-result-list">
                    <?php foreach ($area['rows'] as $row): ?>
                        <a href="<?= View::e($row['url']) ?>"><span><strong><?= View::e($row['title']) ?></strong><small><?= View::e($row['subtitle'] ?: 'Nessun dettaglio aggiuntivo') ?></small></span><?= View::icon('chevron') ?></a>
                    <?php endforeach; ?>
                </div>
            </section>
        <?php endforeach; ?>
    </div>
<?php elseif (mb_strlen($query) >= 2): ?>
    <section class="card empty-state-large"><?= View::icon('search') ?><h2>Nessun risultato</h2><p>Controlla la ricerca o prova con un codice, un numero documento o una ragione sociale.</p></section>
<?php endif; ?>
