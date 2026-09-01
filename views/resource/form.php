<?php
use Luna\Core\Csrf;
use Luna\Core\View;
$errors = $_SESSION['form_errors'] ?? [];
$old = $_SESSION['form_old'] ?? [];
unset($_SESSION['form_errors'], $_SESSION['form_old']);
$row = $old ? array_merge($row, $old) : $row;
?>
<section class="page-intro compact">
    <div><a class="back-link" href="/r/<?= View::e($slug) ?>">← <?= View::e($module['title']) ?></a><h1><?= !empty($row['id']) ? 'Modifica' : 'Nuovo' ?> <?= View::e(mb_strtolower($module['singular'])) ?></h1><p>Compila le informazioni e salva le modifiche.</p></div>
</section>
<?php if ($errors): ?><div class="alert alert-error"><?= View::icon('alert') ?><ul><?php foreach ($errors as $error): ?><li><?= View::e($error) ?></li><?php endforeach; ?></ul></div><?php endif; ?>
<form method="post" action="/r/<?= View::e($slug) ?>/save" class="card form-card modern-form" <?= in_array($slug, ['customers','suppliers'], true) ? 'data-party-form' : '' ?>>
    <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
    <?php if (!empty($row['id'])): ?><input type="hidden" name="id" value="<?= (int) $row['id'] ?>"><?php endif; ?>
    <div class="form-section-head"><div><span class="section-kicker">Dati principali</span><h2>Informazioni</h2></div><span class="required-note">* Campi obbligatori</span></div>
    <div class="form-grid">
    <?php if (in_array($slug, ['customers','suppliers'], true)): ?>
        <div class="field full-width vat-lookup-row"><span>Compilazione assistita</span><div class="inline-actions"><button class="button ghost" type="button" data-vat-lookup>Controlla e importa dalla Partita IVA</button><small class="muted" data-vat-lookup-status>Verifica duplicati sempre attiva al salvataggio.</small></div></div>
    <?php endif; ?>
    <?php foreach ($module['fields'] as $field => $settings): ?>
        <?php $value = $row[$field] ?? ($settings['default'] ?? null); $type = $settings['type']; ?>
        <label class="field <?= $type === 'textarea' ? 'full-width' : '' ?> <?= $type === 'checkbox' ? 'checkbox-field' : '' ?>">
            <?php if ($type === 'checkbox'): ?>
                <input class="switch-input" type="checkbox" name="<?= View::e($field) ?>" value="1" <?= $value ? 'checked' : '' ?>><span class="switch-ui"></span><span><?= View::e($settings['label']) ?></span>
            <?php else: ?>
                <span><?= View::e($settings['label']) ?><?= ($settings['required'] ?? false) ? ' *' : '' ?></span>
                <?php if ($type === 'textarea'): ?>
                    <textarea name="<?= View::e($field) ?>" rows="4" <?= ($settings['required'] ?? false) ? 'required' : '' ?>><?= View::e($value) ?></textarea>
                <?php elseif ($type === 'select'): ?>
                    <select name="<?= View::e($field) ?>" <?= ($settings['required'] ?? false) ? 'required' : '' ?>><option value="">Seleziona…</option><?php foreach ($settings['options'] as $key => $label): ?><option value="<?= View::e($key) ?>" <?= (string) $value === (string) $key ? 'selected' : '' ?>><?= View::e($label) ?></option><?php endforeach; ?></select>
                <?php else: ?>
                    <?php $htmlType = $type === 'decimal' ? 'text' : $type; $display = $type === 'datetime-local' && $value ? str_replace(' ', 'T', substr((string) $value, 0, 16)) : $value; ?>
                    <input type="<?= View::e($htmlType) ?>" name="<?= View::e($field) ?>" value="<?= View::e($display) ?>" <?= ($settings['required'] ?? false) ? 'required' : '' ?>>
                <?php endif; ?>
            <?php endif; ?>
        </label>
    <?php endforeach; ?>
    </div>
    <div class="form-actions"><a class="button ghost" href="/r/<?= View::e($slug) ?>">Annulla</a><button class="button primary" type="submit"><?= View::icon('check') ?> Salva modifiche</button></div>
</form>
