<?php

declare(strict_types=1);

return [
    'enforcement' => (string) \Luna\Core\Env::get('LUNA_AUTHORIZATION_ENFORCEMENT', 'shadow'),
    'role_families' => [
        'SUPERUSER' => 'ROLE_SUPERUSER',
        'OWNER' => 'ROLE_COMPANY_ADMIN',
        'ADMIN' => 'ROLE_COMPANY_ADMIN',
        'MANAGER' => 'ROLE_MANAGER',
        'OPERATOR' => 'ROLE_OPERATOR',
        'ACCOUNTANT' => 'ROLE_OPERATOR',
        'SALES' => 'ROLE_OPERATOR',
        'WAREHOUSE' => 'ROLE_OPERATOR',
        'HR' => 'ROLE_OPERATOR',
        'VIEWER' => 'ROLE_READONLY',
    ],
    'roles' => [
        'SUPERUSER' => ['*'],
        'OWNER' => ['*'],
        'ADMIN' => ['*'],
        'MANAGER' => ['*.read', '*.export', '*.approve', 'dashboard.read'],
        'OPERATOR' => ['dashboard.read', 'workspace.*', 'anagraphics.read', 'anagraphics.write'],
        'ACCOUNTANT' => ['dashboard.read', 'workspace.*', 'anagraphics.*', 'sales.*', 'purchases.*', 'accounting.*', 'professional.*', 'imports.*', 'management_reports.*'],
        'SALES' => ['dashboard.read', 'workspace.*', 'anagraphics.*', 'sales.*', 'crm.*', 'projects.*', 'communications.*', 'ecommerce.*', 'rental.*'],
        'WAREHOUSE' => ['dashboard.read', 'workspace.*', 'anagraphics.read', 'sales.read', 'sales.export', 'purchases.read', 'purchases.export', 'inventory.*'],
        'HR' => ['dashboard.read', 'workspace.*', 'hr.*', 'leave_payroll.*', 'calendar.*'],
        'VIEWER' => ['*.read', '*.export', 'dashboard.read', 'workspace.read'],
    ],
];
