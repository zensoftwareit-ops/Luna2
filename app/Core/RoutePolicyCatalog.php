<?php

declare(strict_types=1);

namespace Luna\Core;

final class RoutePolicyCatalog
{
    /** @return array{module:?string,permission:string,operation:string,dynamic?:string} */
    public static function classify(string $method, string $pattern, bool $auth): array
    {
        if (!$auth) {
            return ['module' => null, 'permission' => 'public', 'operation' => 'PUBLIC'];
        }

        $operation = self::operation($method, $pattern);
        [$area, $module] = self::areaAndModule($pattern);
        $dynamic = str_starts_with($pattern, '/documents/{type}') ? 'document_type'
            : (str_starts_with($pattern, '/r/{module}') ? 'resource_module' : null);

        $policy = [
            'module' => $module,
            'permission' => $area . '.' . strtolower($operation),
            'operation' => $operation,
        ];
        if ($dynamic !== null) {
            $policy['dynamic'] = $dynamic;
        }
        return $policy;
    }

    private static function operation(string $method, string $pattern): string
    {
        if (preg_match('#/(?:export|pdf|xml|download)(?:/|$)#', $pattern)) {
            return 'EXPORT';
        }
        if (strtoupper($method) === 'GET') {
            return 'READ';
        }
        if (preg_match('#/(?:approve|validate|lock|confirm|status|post)(?:/|$)#', $pattern)) {
            return 'APPROVE';
        }
        return 'WRITE';
    }

    /** @return array{0:string,1:?string} */
    private static function areaAndModule(string $pattern): array
    {
        $rules = [
            '#^/accounting#' => ['accounting', 'accounting'],
            '#^/professional#' => ['professional', 'professional'],
            '#^/operations/logistics#' => ['inventory', 'inventory'],
            '#^/operations/projects#' => ['projects', 'projects'],
            '#^/operations/communications#' => ['communications', 'communications'],
            '#^/operations/hr#' => ['hr', 'hr'],
            '#^/operations/ecommerce#' => ['ecommerce', 'ecommerce'],
            '#^/operations/rental#' => ['rental', 'rental'],
            '#^/operations/calendar#' => ['calendar', 'calendar'],
            '#^/reports/management#' => ['management_reports', 'management_reports'],
            '#^/documents#' => ['sales', null],
            '#^/imports#' => ['imports', 'imports'],
            '#^/workspace#' => ['workspace', null],
            '#^/settings#' => ['settings', null],
            '#^/r/\{module\}#' => ['resources', null],
            '#^/dashboard#' => ['dashboard', null],
            '#^/(?:login|logout)$#' => ['authentication', null],
        ];
        foreach ($rules as $regex => $policy) {
            if (preg_match($regex, $pattern)) {
                return $policy;
            }
        }
        return ['application', null];
    }
}
