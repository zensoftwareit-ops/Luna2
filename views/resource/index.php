<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<section class="page-intro compact">
    <div><span class="eyebrow"><?= View::e($module['group']) ?></span><h1><?= View::e($module['title']) ?></h1><p><?= count($rows) ?> record visualizzati</p></div>
    <a class="button primary" href="/r/<?= View::e($slug) ?>/create"><?= View::icon('plus') ?> Nuovo <?= View::e(mb_strtolower($module['singular'])) ?></a>
</section>
<section class="list-toolbar">
    <form method="get" class="search-form"><span class="search-control"><?= View::icon('search') ?><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Cerca in <?= View::e(mb_strtolower($module['title'])) ?>…"></span><button class="button" type="submit">Cerca</button></form>
    <a class="button ghost" href="/r/<?= View::e($slug) ?>/export"><?= View::icon('download') ?> Esporta CSV</a>
</section>
<section class="card data-card">
    <div class="table-wrap">
        <table>
            <thead><tr><?php foreach ($module['columns'] as $column): ?><th><?= View::e($module['fields'][$column]['label'] ?? ucfirst(str_replace('_', ' ', $column))) ?></th><?php endforeach; ?><th class="actions-column">Azioni</th></tr></thead>
            <tbody>
            <?php foreach ($rows as $row): ?>
                <tr>
                    <?php foreach ($module['columns'] as $index => $column): ?>
                        <?php $field = $module['fields'][$column] ?? []; $value = $row[$column] ?? null; ?>
                        <td class="<?= $index === 0 ? 'primary-cell' : '' ?>">
                            <?php if (($field['type'] ?? '') === 'checkbox'): ?><span class="badge <?= $value ? 'status-active' : 'status-muted' ?>"><?= $value ? 'Attivo' : 'No' ?></span>
                            <?php elseif (($field['type'] ?? '') === 'decimal'): ?><?= View::money($value) ?>
                            <?php elseif (($field['type'] ?? '') === 'date'): ?><?= View::date($value) ?>
                            <?php else: ?><?= View::e($value ?? '—') ?><?php endif; ?>
                        </td>
                    <?php endforeach; ?>
                    <td class="row-actions">
                        <a class="table-action" href="/r/<?= View::e($slug) ?>/<?= (int) $row['id'] ?>/edit">Modifica</a>
                        <form method="post" action="/r/<?= View::e($slug) ?>/<?= (int) $row['id'] ?>/delete" data-confirm="Eliminare definitivamente il record?">
                            <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="table-action danger-text" type="submit">Elimina</button>
                        </form>
                    </td>
                </tr>
            <?php endforeach; ?>
            <?php if (!$rows): ?><tr><td colspan="<?= count($module['columns']) + 1 ?>"><div class="table-empty"><span><?= View::icon('search') ?></span><strong>Nessun risultato</strong><small>Prova a modificare la ricerca o inserisci il primo record.</small></div></td></tr><?php endif; ?>
            </tbody>
        </table>
    </div>
</section>
