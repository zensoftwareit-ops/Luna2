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
$workspace = $view->workspaceSummary();
$enabled = static fn (string $key): bool => (bool) ($features[$key]['enabled'] ?? false);
$active = static fn (string $prefix): string => str_starts_with($currentPath, $prefix) ? ' active' : '';
$groups = [];
$operationalSlugs = ['inventory-movements', 'projects', 'calendar-events', 'calendar-accounts', 'payroll-runs', 'payroll-configs', 'leave-balances', 'ecommerce-channels', 'ecommerce-orders', 'rental-contracts', 'rental-tickets'];
foreach ($config['modules'] as $slug => $moduleConfig) {
    if (!$enabled((string) $moduleConfig['feature']) || $moduleConfig['group'] === 'Contabilità' || in_array($slug, $operationalSlugs, true)) {
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
    <link rel="stylesheet" href="/assets/app.css?v=6.1.0">
    <script src="/assets/app.js?v=6.1.0" defer></script>
</head>
<body>
<a class="skip-link" href="#main-page">Vai al contenuto</a>
<div class="app-shell">
    <aside class="sidebar" id="sidebar">
        <div class="sidebar-head">
            <a class="brand" href="<?= $isSuperuser ? '/settings/company' : '/dashboard' ?>" aria-label="Luna2">
                <span class="brand-copy">
                    <img class="brand-logo logo-on-dark" src="/assets/logo-luna2.png?v=1" width="748" height="202" alt="">
                    <small>Business workspace</small>
                </span>
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
            <?php if ($enabled('professional')): ?><a class="nav-link<?= $active('/professional') ?>" href="/professional"><?= View::icon('lock') ?><span>Centro professionale</span></a><?php endif; ?>

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
                        <a class="<?= $active('/accounting/treasury') ?>" href="/accounting/treasury">Tesoreria e partite</a>
                        <a class="<?= $active('/accounting/trial-balance') ?>" href="/accounting/trial-balance">Bilancio di verifica</a>
                        <a class="<?= $active('/accounting/compliance') ?>" href="/accounting/compliance">Adempimenti e chiusure</a>
                        <a class="<?= $active('/accounting/setup') ?>" href="/accounting/setup">Piano dei conti e causali</a>
                        <a class="<?= $active('/r/fixed-assets') ?>" href="/r/fixed-assets">Cespiti</a>
                        <a class="<?= $active('/r/tax-deadlines') ?>" href="/r/tax-deadlines">Scadenze fiscali</a>
                        <?php if (in_array((string) (Auth::user()['role'] ?? ''), ['OWNER', 'ADMIN'], true)): ?><a class="<?= $active('/settings/endpoints') ?>" href="/settings/endpoints">Endpoint e-invoice</a><?php endif; ?>
                    </div>
                </details>
            <?php endif; ?>

            <?php if ($enabled('inventory') || $enabled('projects') || $enabled('calendar')): ?>
                <details class="nav-group" <?= str_starts_with($currentPath, '/operations/logistics') || str_starts_with($currentPath, '/operations/projects') || str_starts_with($currentPath, '/operations/calendar') ? 'open' : '' ?>>
                    <summary><?= View::icon('briefcase') ?><span>Operatività</span><?= View::icon('chevron', 'nav-chevron') ?></summary>
                    <div class="nav-children">
                        <?php if ($enabled('inventory')): ?><a class="<?= $active('/operations/logistics') ?>" href="/operations/logistics">Logistica e picking</a><?php endif; ?>
                        <?php if ($enabled('projects')): ?><a class="<?= $active('/operations/projects') ?>" href="/operations/projects">Consuntivazione commesse</a><?php endif; ?>
                        <?php if ($enabled('calendar')): ?><a class="<?= $active('/operations/calendar') ?>" href="/operations/calendar">Calendari sincronizzati</a><?php endif; ?>
                    </div>
                </details>
            <?php endif; ?>

            <?php if ($enabled('hr')): ?>
                <a class="nav-link<?= $active('/operations/hr') ?>" href="/operations/hr"><?= View::icon('id-card') ?><span>Ferie e paghe</span></a>
            <?php endif; ?>
            <?php if ($enabled('ecommerce')): ?>
                <a class="nav-link<?= $active('/operations/ecommerce') ?>" href="/operations/ecommerce"><?= View::icon('store') ?><span>Hub e-commerce</span></a>
            <?php endif; ?>
            <?php if ($enabled('rental')): ?>
                <a class="nav-link<?= $active('/operations/rental') ?>" href="/operations/rental"><?= View::icon('car') ?><span>Noleggio e ticket</span></a>
            <?php endif; ?>
            <a class="nav-link<?= $active('/operations/communications') ?>" href="/operations/communications"><?= View::icon('receipt') ?><span>Comunicazioni</span></a>
            <a class="nav-link<?= $active('/reports/management') ?>" href="/reports/management"><?= View::icon('download') ?><span>Report direzionali</span></a>

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
            <a class="nav-link<?= $active('/workspace/onboarding') ?>" href="/workspace/onboarding"><?= View::icon('check') ?><span>Prontezza operativa</span><small class="nav-progress"><?= (int) $workspace['readiness'] ?>%</small></a>
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
                <?php if (!$isSuperuser): ?>
                    <form class="topbar-search" action="/workspace/search" method="get" role="search">
                        <?= View::icon('search') ?><input type="search" name="q" placeholder="Cerca ovunque…" aria-label="Ricerca globale" autocomplete="off"><kbd>/</kbd>
                    </form>
                    <details class="topbar-popover notification-popover">
                        <summary class="icon-button" aria-label="Notifiche"><?= View::icon('bell') ?><?php if ($workspace['unread']): ?><span class="notification-count"><?= min(99, (int) $workspace['unread']) ?></span><?php endif; ?></summary>
                        <div class="popover-panel">
                            <div class="popover-head"><div><span class="section-kicker">Workspace</span><strong>Notifiche</strong></div><a href="/workspace/notifications">Vedi tutte</a></div>
                            <div class="popover-list">
                                <?php foreach ($workspace['notifications'] as $notification): ?><a href="<?= View::e($notification['action_url'] ?: '/workspace/notifications') ?>" class="<?= empty($notification['read_at']) ? 'unread' : '' ?>"><span class="notification-dot severity-<?= View::e(strtolower((string) $notification['severity'])) ?>"></span><span><strong><?= View::e($notification['title']) ?></strong><small><?= View::e($notification['message']) ?></small></span></a><?php endforeach; ?>
                                <?php if (!$workspace['notifications']): ?><div class="popover-empty"><?= View::icon('check') ?><span>Nessuna attività urgente.</span></div><?php endif; ?>
                            </div>
                        </div>
                    </details>
                    <details class="topbar-popover help-popover">
                        <summary class="icon-button" aria-label="Guida contestuale"><?= View::icon('help') ?></summary>
                        <div class="popover-panel compact-panel">
                            <div class="popover-head"><div><span class="section-kicker">Assistenza</span><strong>Guida rapida</strong></div></div>
                            <a class="help-link" href="/workspace/onboarding"><?= View::icon('check') ?><span><strong>Configurazione guidata</strong><small>Controlla la prontezza aziendale</small></span></a>
                            <a class="help-link" href="/professional#quality"><?= View::icon('alert') ?><span><strong>Qualità dati</strong><small>Verifica anomalie e quadrature</small></span></a>
                            <a class="help-link" href="/imports"><?= View::icon('upload') ?><span><strong>Migrazione Koinos</strong><small>Carica, valida e riconcilia</small></span></a>
                        </div>
                    </details>
                <?php else: ?>
                    <span class="environment-pill"><i></i> Piattaforma operativa</span>
                <?php endif; ?>
                <?php if (!$isSuperuser && $enabled('sales')): ?>
                    <a class="quick-create" href="/documents/quotes/create"><?= View::icon('plus') ?><span>Nuovo</span></a>
                <?php elseif (!$isSuperuser && $enabled('anagraphics')): ?>
                    <a class="quick-create" href="/r/customers/create"><?= View::icon('plus') ?><span>Nuovo</span></a>
                <?php endif; ?>
            </div>
        </header>
        <div class="page" id="main-page" tabindex="-1">
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
