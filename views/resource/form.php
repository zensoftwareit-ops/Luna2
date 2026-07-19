<?php
use Luna\Core\Csrf;
use Luna\Core\View;
$errors = $_SESSION['form_errors'] ?? [];
$old = $_SESSION['form_old'] ?? [];
unset($_SESSION['form_errors'], $_SESSION['form_old']);
$row = $old ? array_merge($row, $old) : $row;
?>
<?php if ($errors): ?><div class="alert alert-error"><ul><?php foreach ($errors as $error): ?><li><?= View::e($error) ?></li><?php endforeach; ?></ul></div><?php endif; ?>
<form method="post" action="/r/<?= View::e($slug) ?>/save" class="card form-card">
    <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
    <?php if (!empty($row['id'])): ?><input type="hidden" name="id" value="<?= (int) $row['id'] ?>"><?php endif; ?>
    <div class="form-grid">
    <?php foreach ($module['fields'] as $field => $settings): ?>
        <?php $value = $row[$field] ?? ($settings['default'] ?? null); $type = $settings['type']; ?>
        <label class="<?= $type === 'textarea' ? 'full-width' : '' ?>">
            <span><?= View::e($settings['label']) ?><?= ($settings['required'] ?? false) ? ' *' : '' ?></span>
            <?php if ($type === 'textarea'): ?>
                <textarea name="<?= View::e($field) ?>" rows="4" <?= ($settings['required'] ?? false) ? 'required' : '' ?>><?= View::e($value) ?></textarea>
            <?php elseif ($type === 'select'): ?>
                <select name="<?= View::e($field) ?>" <?= ($settings['required'] ?? false) ? 'required' : '' ?>><option value="">Seleziona…</option><?php foreach ($settings['options'] as $key => $label): ?><option value="<?= View::e($key) ?>" <?= (string) $value === (string) $key ? 'selected' : '' ?>><?= View::e($label) ?></option><?php endforeach; ?></select>
            <?php elseif ($type === 'checkbox'): ?>
                <input class="checkbox" type="checkbox" name="<?= View::e($field) ?>" value="1" <?= $value ? 'checked' : '' ?>>
            <?php else: ?>
                <?php $htmlType = $type === 'decimal' ? 'text' : $type; $display = $type === 'datetime-local' && $value ? str_replace(' ', 'T', substr((string) $value, 0, 16)) : $value; ?>
                <input type="<?= View::e($htmlType) ?>" name="<?= View::e($field) ?>" value="<?= View::e($display) ?>" <?= ($settings['required'] ?? false) ? 'required' : '' ?>>
            <?php endif; ?>
        </label>
    <?php endforeach; ?>
    </div>
    <div class="form-actions"><a class="button" href="/r/<?= View::e($slug) ?>">Annulla</a><button class="button primary" type="submit">Salva</button></div>
</form>
