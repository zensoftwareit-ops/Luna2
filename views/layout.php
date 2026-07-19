<?php

use Luna\Core\Auth;
use Luna\Core\Csrf;
use Luna\Core\View;

$title = $title ?? 'Luna2';
$flash = $_SESSION['flash'] ?? null;
unset($_SESSION['flash']);
$groups = [];
foreach ($config['modules'] as $slug => $moduleConfig) {
    $groups[$moduleConfig['group']][$slug] = $moduleConfig;
}
?>
<!doctype html>
<html lang="it">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="<?= View::e(Csrf::token()) ?>">
    <title><?= View::e($title) ?> · Luna2</title>
    <link rel="stylesheet" href="/assets/app.css?v=3.0.0">
    <script src="/assets/app.js?v=3.0.0" defer></script>
</head>
<body>
<div class="app-shell">
    <aside class="sidebar" id="sidebar">
        <a class="brand" href="/dashboard"><span class="brand-mark">L2</span><span>Luna2</span></a>
        <nav aria-label="Menu principale">
            <a href="/dashboard">Dashboard</a>

            <details open>
                <summary>Documenti</summary>
                <a href="/documents/quotes">Preventivi</a>
                <a href="/documents/orders">Ordini clienti</a>
                <a href="/documents/ddt">DDT</a>
                <a href="/documents/proformas">Proforma</a>
                <a href="/documents/invoices">Fatture attive</a>
                <a href="/documents/credit-notes">Note di credito</a>
                <a href="/documents/purchase-orders">Ordini fornitori</a>
                <a href="/documents/purchase-invoices">Fatture passive</a>
            </details>

            <details open>
                <summary>Contabilità</summary>
                <a href="/accounting/journal">Libro giornale</a>
                <a href="/accounting/trial-balance">Bilancio di verifica</a>
                <a href="/r/tax-deadlines">Scadenzario fiscale</a>
                <a href="/r/fixed-assets">Cespiti</a>
                <a href="/imports">Import DATEV Koinos</a>
            </details>

            <?php foreach ($groups as $group => $modules): ?>
                <?php if ($group === 'Contabilità'): continue; endif; ?>
                <details>
                    <summary><?= View::e($group) ?></summary>
                    <?php foreach ($modules as $slug => $moduleConfig): ?>
                        <a href="/r/<?= View::e($slug) ?>"><?= View::e($moduleConfig['title']) ?></a>
                    <?php endforeach; ?>
                </details>
            <?php endforeach; ?>
        </nav>
        <div class="sidebar-user">
            <span><?= View::e(Auth::user()['name'] ?? '') ?></span>
            <small><?= View::e(Auth::user()['role'] ?? '') ?></small>
            <form method="post" action="/logout">
                <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
                <button class="link-button" type="submit">Esci</button>
            </form>
        </div>
    </aside>
    <main class="main-content">
        <header class="topbar">
            <button class="menu-toggle" type="button" data-menu-toggle aria-label="Apri menu">☰</button>
            <div><strong><?= View::e($title) ?></strong></div>
        </header>
        <div class="page">
            <?php if ($flash): ?>
                <div class="alert alert-<?= View::e($flash['type'] ?? 'success') ?>" role="status"><?= View::e($flash['message'] ?? '') ?></div>
            <?php endif; ?>
            <?= $content ?>
        </div>
    </main>
</div>
</body>
</html>
