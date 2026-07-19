<?php use Luna\Core\Csrf; use Luna\Core\View; ?>
<div class="page-actions">
    <form method="get" class="search-form"><input type="search" name="q" value="<?= View::e($search) ?>" placeholder="Cerca…"><button class="button" type="submit">Cerca</button></form>
    <div><a class="button" href="/r/<?= View::e($slug) ?>/export">Esporta CSV</a> <a class="button primary" href="/r/<?= View::e($slug) ?>/create">Nuovo</a></div>
</div>
<section class="card table-wrap">
    <table>
        <thead><tr><?php foreach ($module['columns'] as $column): ?><th><?= View::e($module['fields'][$column]['label'] ?? ucfirst(str_replace('_', ' ', $column))) ?></th><?php endforeach; ?><th></th></tr></thead>
        <tbody>
        <?php foreach ($rows as $row): ?>
            <tr>
                <?php foreach ($module['columns'] as $column): ?>
                    <?php $field = $module['fields'][$column] ?? []; $value = $row[$column] ?? null; ?>
                    <td>
                        <?php if (($field['type'] ?? '') === 'checkbox'): ?><span class="badge"><?= $value ? 'Sì' : 'No' ?></span>
                        <?php elseif (($field['type'] ?? '') === 'decimal'): ?><?= View::money($value) ?>
                        <?php elseif (($field['type'] ?? '') === 'date'): ?><?= View::date($value) ?>
                        <?php else: ?><?= View::e($value ?? '—') ?><?php endif; ?>
                    </td>
                <?php endforeach; ?>
                <td class="row-actions">
                    <a href="/r/<?= View::e($slug) ?>/<?= (int) $row['id'] ?>/edit">Modifica</a>
                    <form method="post" action="/r/<?= View::e($slug) ?>/<?= (int) $row['id'] ?>/delete" data-confirm="Eliminare definitivamente il record?">
                        <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>"><button class="link-button danger-text" type="submit">Elimina</button>
                    </form>
                </td>
            </tr>
        <?php endforeach; ?>
        <?php if (!$rows): ?><tr><td colspan="<?= count($module['columns']) + 1 ?>" class="muted">Nessun record trovato.</td></tr><?php endif; ?>
        </tbody>
    </table>
</section>
