<?php use Luna\Core\View; ?>
<section class="metric-grid">
    <article class="metric"><span>Clienti attivi</span><strong><?= (int) $metrics['customers'] ?></strong></article>
    <article class="metric"><span>Preventivi aperti</span><strong><?= (int) $metrics['open_quotes'] ?></strong></article>
    <article class="metric"><span>Crediti da incassare</span><strong><?= View::money($metrics['receivables']) ?></strong></article>
    <article class="metric"><span>Debiti da pagare</span><strong><?= View::money($metrics['payables']) ?></strong></article>
    <article class="metric danger"><span>Scadenze fiscali scadute</span><strong><?= (int) $metrics['overdue_tax'] ?></strong></article>
    <article class="metric warning"><span>Prodotti sotto scorta</span><strong><?= (int) $metrics['low_stock'] ?></strong></article>
</section>

<div class="two-columns">
    <section class="card">
        <div class="card-header"><h2>Documenti recenti</h2><a href="/documents/invoices">Apri fatture</a></div>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Data</th><th>Numero</th><th>Controparte</th><th>Totale</th><th>Stato</th></tr></thead>
                <tbody>
                <?php foreach ($recentDocuments as $row): ?>
                    <tr><td><?= View::date($row['document_date']) ?></td><td><?= View::e($row['number']) ?></td><td><?= View::e($row['counterparty_name']) ?></td><td><?= View::money($row['total']) ?></td><td><span class="badge"><?= View::e($row['status']) ?></span></td></tr>
                <?php endforeach; ?>
                <?php if (!$recentDocuments): ?><tr><td colspan="5" class="muted">Nessun documento.</td></tr><?php endif; ?>
                </tbody>
            </table>
        </div>
    </section>
    <section class="card">
        <div class="card-header"><h2>Prossime scadenze</h2><a href="/r/tax-deadlines">Scadenzario</a></div>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Data</th><th>Tipo</th><th>Descrizione</th><th>Importo</th></tr></thead>
                <tbody>
                <?php foreach ($deadlines as $row): ?>
                    <tr><td><?= View::date($row['due_date']) ?></td><td><?= View::e($row['deadline_type']) ?></td><td><?= View::e($row['description']) ?></td><td><?= $row['amount'] !== null ? View::money($row['amount']) : '—' ?></td></tr>
                <?php endforeach; ?>
                <?php if (!$deadlines): ?><tr><td colspan="4" class="muted">Nessuna scadenza aperta.</td></tr><?php endif; ?>
                </tbody>
            </table>
        </div>
    </section>
</div>
