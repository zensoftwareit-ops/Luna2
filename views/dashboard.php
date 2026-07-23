<?php use Luna\Core\Auth; use Luna\Core\View; ?>
<section class="page-intro dashboard-intro">
    <div>
        <span class="eyebrow">Panoramica aziendale</span>
        <h1>Buongiorno, <?= View::e(explode(' ', trim((string) (Auth::user()['name'] ?? '')))[0] ?: 'Utente') ?></h1>
        <p>Numeri, scadenze e attività essenziali in un unico spazio.</p>
    </div>
    <div class="intro-actions">
        <?php if ($featureStates['anagraphics']['enabled'] ?? false): ?><a class="button" href="/r/customers/create"><?= View::icon('users') ?> Nuovo cliente</a><?php endif; ?>
        <?php if ($featureStates['sales']['enabled'] ?? false): ?><a class="button primary" href="/documents/quotes/create"><?= View::icon('plus') ?> Nuovo preventivo</a><?php endif; ?>
    </div>
</section>

<?php $qualityIssues = array_sum(array_map(static fn (array $row): int => $row['ok'] ? 0 : (int) $row['count'], $quality)); ?>
<section class="workspace-health-strip">
    <a href="/workspace/onboarding" class="workspace-health-item">
        <span class="health-ring" style="--progress:<?= (int) $onboarding['percentage'] ?>"><?= (int) $onboarding['percentage'] ?>%</span>
        <span><strong>Configurazione aziendale</strong><small><?= (int) $onboarding['completed'] ?> di <?= (int) $onboarding['total'] ?> passaggi verificati</small></span>
        <?= View::icon('chevron') ?>
    </a>
    <a href="/professional#quality" class="workspace-health-item <?= $qualityIssues ? 'attention' : '' ?>">
        <span class="health-symbol"><?= $qualityIssues ? View::icon('alert') : View::icon('check') ?></span>
        <span><strong>Qualità dei dati</strong><small><?= $qualityIssues ? $qualityIssues . ' elementi da controllare' : 'Nessuna anomalia rilevata' ?></small></span>
        <?= View::icon('chevron') ?>
    </a>
    <a href="/workspace/search" class="workspace-health-item">
        <span class="health-symbol"><?= View::icon('search') ?></span>
        <span><strong>Ricerca globale</strong><small>Trova rapidamente qualsiasi informazione</small></span>
        <?= View::icon('chevron') ?>
    </a>
</section>

<?php if ($migrations['pending'] !== []): ?>
    <div class="system-banner">
        <span class="system-banner-icon"><?= View::icon('alert') ?></span>
        <div><strong>Configurazione database incompleta</strong><span><?= count($migrations['pending']) ?> migrazioni da applicare. Alcuni moduli potrebbero non essere disponibili.</span></div>
        <span class="system-owner-note">Richiedi l’intervento del superuser</span>
    </div>
<?php endif; ?>

<section class="metric-grid">
    <article class="metric"><span class="metric-icon blue"><?= View::icon('users') ?></span><div><span>Clienti attivi</span><strong><?= (int) $metrics['customers'] ?></strong><small>Anagrafiche abilitate</small></div></article>
    <article class="metric"><span class="metric-icon violet"><?= View::icon('receipt') ?></span><div><span>Preventivi aperti</span><strong><?= (int) $metrics['open_quotes'] ?></strong><small>Da seguire</small></div></article>
    <article class="metric"><span class="metric-icon green">€</span><div><span>Crediti aperti</span><strong><?= View::money($metrics['receivables']) ?></strong><small>Da incassare</small></div></article>
    <article class="metric"><span class="metric-icon amber">€</span><div><span>Debiti aperti</span><strong><?= View::money($metrics['payables']) ?></strong><small>Da pagare</small></div></article>
    <article class="metric <?= (int) $metrics['overdue_tax'] > 0 ? 'danger' : '' ?>"><span class="metric-icon red"><?= View::icon('calendar') ?></span><div><span>Scadenze fiscali</span><strong><?= (int) $metrics['overdue_tax'] ?></strong><small>Scadute</small></div></article>
    <article class="metric <?= (int) $metrics['low_stock'] > 0 ? 'warning' : '' ?>"><span class="metric-icon slate"><?= View::icon('box') ?></span><div><span>Sotto scorta</span><strong><?= (int) $metrics['low_stock'] ?></strong><small>Prodotti</small></div></article>
</section>

<div class="content-grid">
    <section class="card span-7">
        <div class="card-header"><div><span class="section-kicker">Attività recente</span><h2>Ultimi documenti</h2></div><?php if (($featureStates['sales']['enabled'] ?? false) || ($featureStates['purchases']['enabled'] ?? false)): ?><a class="text-link" href="<?= ($featureStates['sales']['enabled'] ?? false) ? '/documents/invoices' : '/documents/purchase-invoices' ?>">Vedi tutti <?= View::icon('chevron') ?></a><?php endif; ?></div>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Documento</th><th>Controparte</th><th>Data</th><th class="numeric">Totale</th><th>Stato</th></tr></thead>
                <tbody>
                <?php foreach ($recentDocuments as $row): ?>
                    <tr><td><strong><?= View::e($row['number']) ?></strong><small class="cell-subtitle"><?= View::e(str_replace('_', ' ', $row['document_type'])) ?></small></td><td><?= View::e($row['counterparty_name']) ?></td><td><?= View::date($row['document_date']) ?></td><td class="numeric"><?= View::money($row['total']) ?></td><td><span class="badge status-<?= View::e(strtolower($row['status'])) ?>"><?= View::e($row['status']) ?></span></td></tr>
                <?php endforeach; ?>
                <?php if (!$recentDocuments): ?><tr><td colspan="5"><div class="table-empty">Nessun documento recente.</div></td></tr><?php endif; ?>
                </tbody>
            </table>
        </div>
    </section>
    <section class="card span-5">
        <div class="card-header"><div><span class="section-kicker">Agenda</span><h2>Prossime scadenze</h2></div><?php if ($featureStates['accounting']['enabled'] ?? false): ?><a class="text-link" href="/r/tax-deadlines">Apri <?= View::icon('chevron') ?></a><?php endif; ?></div>
        <div class="deadline-list">
            <?php foreach ($deadlines as $row): ?>
                <article class="deadline-item"><time datetime="<?= View::e($row['due_date']) ?>"><strong><?= date('d', strtotime($row['due_date'])) ?></strong><span><?= mb_strtoupper(date('M', strtotime($row['due_date']))) ?></span></time><div><strong><?= View::e($row['description']) ?></strong><span><?= View::e($row['deadline_type']) ?> · <?= $row['amount'] !== null ? View::money($row['amount']) : 'Importo non indicato' ?></span></div><span class="badge"><?= View::e($row['status']) ?></span></article>
            <?php endforeach; ?>
            <?php if (!$deadlines): ?><div class="panel-empty"><?= View::icon('check') ?><strong>Nessuna scadenza aperta</strong><span>La situazione è aggiornata.</span></div><?php endif; ?>
        </div>
    </section>
</div>
