<?php

use Luna\Core\Csrf;
use Luna\Core\View;

$error = $_SESSION['login_error'] ?? null;
$oldEmail = $_SESSION['old_email'] ?? '';
unset($_SESSION['login_error'], $_SESSION['old_email']);
?>
<!doctype html>
<html lang="it">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Accedi · Luna2</title>
    <link rel="stylesheet" href="/assets/app.css?v=3.0.0">
</head>
<body class="auth-page">
<main class="auth-card">
    <div class="auth-brand"><span class="brand-mark">L2</span><h1>Luna2</h1></div>
    <p>Gestionale, fatturazione e contabilità.</p>
    <?php if ($error): ?><div class="alert alert-error"><?= View::e($error) ?></div><?php endif; ?>
    <form method="post" action="/login" class="stack-form">
        <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
        <label>Email<input type="email" name="email" value="<?= View::e($oldEmail) ?>" autocomplete="username" required autofocus></label>
        <label>Password<input type="password" name="password" autocomplete="current-password" required></label>
        <button class="button primary" type="submit">Accedi</button>
    </form>
</main>
</body>
</html>
