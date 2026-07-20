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
    <meta name="theme-color" content="#0b1220">
    <title>Accedi · Luna2</title>
    <link rel="stylesheet" href="/assets/app.css?v=3.3.0">
</head>
<body class="auth-page">
<main class="auth-shell">
    <section class="auth-visual">
        <div class="auth-visual-grid"></div>
        <div class="auth-visual-content">
            <span class="auth-logo"><span class="brand-mark">L</span><strong>Luna</strong></span>
            <div><span class="auth-overline">Business workspace</span><h1>Il lavoro quotidiano,<br>finalmente ordinato.</h1><p>Vendite, contabilità e operatività in un’unica piattaforma essenziale.</p></div>
            <small>© <?= date('Y') ?> Luna2 · Zen Software</small>
        </div>
    </section>
    <section class="auth-form-panel">
        <div class="auth-card">
            <div class="auth-card-head"><span class="mobile-auth-logo"><span class="brand-mark">L</span> Luna</span><span class="eyebrow">Area riservata</span><h2>Bentornato</h2><p>Inserisci le credenziali per accedere al workspace.</p></div>
            <?php if ($error): ?><div class="alert alert-error"><?= View::icon('alert') ?><span><?= View::e($error) ?></span></div><?php endif; ?>
            <form method="post" action="/login" class="stack-form">
                <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
                <label><span>Indirizzo email</span><input type="email" name="email" value="<?= View::e($oldEmail) ?>" autocomplete="username" placeholder="nome@azienda.it" required autofocus></label>
                <label><span>Password</span><input type="password" name="password" autocomplete="current-password" placeholder="La tua password" required></label>
                <button class="button primary auth-submit" type="submit">Accedi al gestionale <?= View::icon('chevron') ?></button>
            </form>
            <p class="auth-security">Connessione protetta · Accesso registrato</p>
        </div>
    </section>
</main>
</body>
</html>
