<?php

use Luna\Core\Auth;
use Luna\Core\Csrf;
use Luna\Core\View;

$title = $title ?? 'Luna2';
$flash = $_SESSION['flash'] ?? null;
unset($_SESSION['flash']);
$currentPath = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$isSuperuser = Auth::isSuperuser();
$features = $view->features();
$enabled = static fn (string $key): bool => (bool) ($features[$key]['enabled'] ?? false);
$active = static fn (string $prefix): string => str_starts_with($currentPath, $prefix) ? ' active' : '';
$groups = [];
foreach ($config['modules'] as $slug => $moduleConfig) {
    if (!$enabled((string) $moduleConfig['feature']) || $moduleConfig['group'] === 'Contabilità') {
        continue;
    }
    $groups[$moduleConfig['group']][$slug] = $moduleConfig;
}
$initials = '';
foreach (preg_split('/\s+/', trim((string) (Auth::user()['name'] ?? 'Utente'))) ?: [] as $part) {
    $initials .= mb_substr($part, 0, 1);
}
$initials = mb_strtoupper(mb_substr($initials, 0, 2));
?>
<!doctype html>
<html lang="it">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="<?= View::e(Csrf::token()) ?>">
    <meta name="theme-color" content="#0b1220">
    <title><?= View::e($title) ?> · Luna2</title>
    <link rel="stylesheet" href="/assets/app.css?v=3.3.0">
    <script src="/assets/app.js?v=3.3.0" defer></script>
</head>
<body>
<div class="app-shell">
    <aside class="sidebar" id="sidebar">
        <div class="sidebar-head">
            <a class="brand" href="<?= $isSuperuser ? '/settings/company' : '/dashboard' ?>" aria-label="Luna2">
                <span class="brand-mark"><span>L</span></span>
                <span class="brand-copy"><strong>Luna</strong><small>Gestionale</small></span>
            </a>
            <button class="sidebar-close" type="button" data-menu-close aria-label="Chiudi menu">×</button>
        </div>

        <nav class="main-nav" aria-label="Menu principale">
            <?php if ($isSuperuser): ?>
                <span class="nav-section-label">Piattaforma</span>
                <a class="nav-link<?= $active('/settings/company') ?>" href="/settings/company"><?= View::icon('briefcase') ?><span>Aziende e utenti</span></a>
                <a class="nav-link<?= $active('/settings/modules') ?>" href="/settings/modules"><?= View::icon('settings') ?><span>Gestione moduli</span></a>
                <a class="nav-link<?= $active('/settings/system') ?>" href="/settings/system"><?= View::icon('check') ?><span>Stato del sistema</span></a>
            <?php else: ?>
            <a class="nav-link<?= $active('/dashboard') ?>" href="/dashboard"><?= View::icon('home') ?><span>Dashboard</span></a>

            <?php if ($enabled('sales')): ?>
                <details class="nav-group" <?= str_starts_with($currentPath, '/documents/') && !str_contains($currentPath, 'purchase-') ? 'open' : '' ?>>
                    <summary><?= View::icon('receipt') ?><span>Vendite</span><?= View::icon('chevron', 'nav-chevron') ?></summary>
                    <div class="nav-children">
                        <a class="<?= $active('/documents/quotes') ?>" href="/documents/quotes">Preventivi</a>
                        <a class="<?= $active('/documents/orders') ?>" href="/documents/orders">Ordini clienti</a>
                        <a class="<?= $active('/documents/ddt') ?>" href="/documents/ddt">DDT</a>
                        <a class="<?= $active('/documents/invoices') ?>" href="/documents/invoices">Fatture attive</a>
                        <a class="<?= $active('/documents/credit-notes') ?>" href="/documents/credit-notes">Note di credito</a>
                        <a class="<?= $active('/documents/proformas') ?>" href="/documents/proformas">Proforma</a>
                    </div>
                </details>
            <?php endif; ?>

            <?php if ($enabled('purchases')): ?>
                <details class="nav-group" <?= str_contains($currentPath, '/documents/purchase-') ? 'open' : '' ?>>
                    <summary><?= View::icon('bag') ?><span>Acquisti</span><?= View::icon('chevron', 'nav-chevron') ?></summary>
                    <div class="nav-children">
                        <a class="<?= $active('/documents/purchase-orders') ?>" href="/documents/purchase-orders">Ordini fornitori</a>
                        <a class="<?= $active('/documents/purchase-invoices') ?>" href="/documents/purchase-invoices">Fatture passive</a>
                    </div>
                </details>
            <?php endif; ?>

            <?php if ($enabled('accounting')): ?>
                <details class="nav-group" <?= str_starts_with($currentPath, '/accounting/') || str_starts_with($currentPath, '/r/tax-') || str_starts_with($currentPath, '/r/fixed-') ? 'open' : '' ?>>
                    <summary><?= View::icon('calculator') ?><span>Contabilità</span><?= View::icon('chevron', 'nav-chevron') ?></summary>
                    <div class="nav-children">
                        <a class="<?= $active('/accounting/journal') ?>" href="/accounting/journal">Prima nota</a>
                        <a class="<?= $active('/accounting/vat-registers') ?>" href="/accounting/vat-registers">Registri IVA</a>
                        <a class="<?= $active('/accounting/vat-settlements') ?>" href="/accounting/vat-settlements">Liquidazioni IVA</a>
                        <a class="<?= $active('/accounting/trial-balance') ?>" href="/accounting/trial-balance">Bilancio di verifica</a>
                        <a class="<?= $active('/r/tax-deadlines') ?>" href="/r/tax-deadlines">Scadenze fiscali</a>
                        <a class="<?= $active('/r/fixed-assets') ?>" href="/r/fixed-assets">Cespiti</a>
                    </div>
                </details>
            <?php endif; ?>

            <?php foreach ($groups as $group => $modules): ?>
                <?php
                $first = reset($modules);
                $groupIcon = $features[$first['feature']]['icon'] ?? 'box';
                $groupOpen = false;
                foreach (array_keys($modules) as $slug) {
                    $groupOpen = $groupOpen || str_starts_with($currentPath, '/r/' . $slug);
                }
                ?>
                <details class="nav-group" <?= $groupOpen ? 'open' : '' ?>>
                    <summary><?= View::icon($groupIcon) ?><span><?= View::e($group) ?></span><?= View::icon('chevron', 'nav-chevron') ?></summary>
                    <div class="nav-children">
                        <?php foreach ($modules as $slug => $moduleConfig): ?>
                            <a class="<?= $active('/r/' . $slug) ?>" href="/r/<?= View::e($slug) ?>"><?= View::e($moduleConfig['title']) ?></a>
                        <?php endforeach; ?>
                    </div>
                </details>
            <?php endforeach; ?>

            <?php if ($enabled('imports')): ?>
                <a class="nav-link<?= $active('/imports') ?>" href="/imports"><?= View::icon('upload') ?><span>Importazioni</span></a>
            <?php endif; ?>
            <?php endif; ?>
        </nav>

        <div class="sidebar-user">
            <span class="avatar"><?= View::e($initials ?: 'U') ?></span>
            <span class="user-copy"><strong><?= View::e(Auth::user()['name'] ?? '') ?></strong><small><?= $isSuperuser ? 'Superuser' : View::e(Auth::user()['role'] ?? '') ?></small></span>
            <form method="post" action="/logout">
                <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
                <button class="logout-button" type="submit" aria-label="Esci">↗</button>
            </form>
        </div>
    </aside>
    <button class="sidebar-backdrop" type="button" data-menu-close aria-label="Chiudi menu"></button>

    <main class="main-content">
        <header class="topbar">
            <button class="menu-toggle" type="button" data-menu-toggle aria-label="Apri menu"><?= View::icon('menu') ?></button>
            <div class="topbar-title"><span><?= $isSuperuser ? 'Piattaforma' : 'Workspace' ?><?= Auth::organizationName() !== '' ? ' · ' . View::e(Auth::organizationName()) : '' ?></span><strong><?= View::e($title) ?></strong></div>
            <div class="topbar-actions">
                <span class="environment-pill"><i></i> Online</span>
                <?php if (!$isSuperuser && $enabled('sales')): ?>
                    <a class="quick-create" href="/documents/quotes/create"><?= View::icon('plus') ?><span>Nuovo</span></a>
                <?php elseif (!$isSuperuser && $enabled('anagraphics')): ?>
                    <a class="quick-create" href="/r/customers/create"><?= View::icon('plus') ?><span>Nuovo</span></a>
                <?php endif; ?>
            </div>
        </header>
        <div class="page">
            <?php if ($flash): ?>
                <div class="alert alert-<?= View::e($flash['type'] ?? 'success') ?>" role="status">
                    <?= View::icon(($flash['type'] ?? 'success') === 'error' ? 'alert' : 'check') ?>
                    <span><?= View::e($flash['message'] ?? '') ?></span>
                </div>
            <?php endif; ?>
            <?= $content ?>
        </div>
    </main>
</div>
</body>
</html>
