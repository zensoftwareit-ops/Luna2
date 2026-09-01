<?php

declare(strict_types=1);

namespace Luna\Service;

use DomainException;
use Luna\Core\LicenseService;
use PDO;
use Throwable;

final class UserLimitService
{
    public function __construct(private readonly PDO $db)
    {
    }

    /** @return array{used:int,max:?int,remaining:?int,enforced:bool} */
    public function usage(int $organizationId): array
    {
        try {
            $statement = $this->db->prepare("SELECT COUNT(*) FROM users WHERE organization_id = ? AND active = 1 AND account_type = 'HUMAN'");
            $statement->execute([$organizationId]);
            $used = (int) $statement->fetchColumn();
        } catch (Throwable) {
            // Compatibilità durante il breve intervallo fra deploy e migrazione 011.
            $statement = $this->db->prepare("SELECT COUNT(*) FROM users WHERE organization_id = ? AND active = 1 AND role <> 'SUPERUSER'");
            $statement->execute([$organizationId]);
            $used = (int) $statement->fetchColumn();
        }
        $licenseService = new LicenseService($this->db, $organizationId);
        $license = $licenseService->snapshot();
        $max = isset($license['max_users']) && $license['max_users'] !== null ? (int) $license['max_users'] : null;
        return [
            'used' => $used,
            'max' => $max,
            'remaining' => $max === null ? null : max(0, $max - $used),
            'enforced' => $licenseService->enforcement() === 'enforce' && $max !== null,
        ];
    }

    public function assertCanActivate(int $organizationId): void
    {
        $lock = $this->db->prepare('SELECT id FROM organizations WHERE id = ? FOR UPDATE');
        $lock->execute([$organizationId]);
        if (!$lock->fetchColumn()) {
            throw new DomainException('Azienda non disponibile.');
        }
        $usage = $this->usage($organizationId);
        if ($usage['enforced'] && $usage['max'] !== null && $usage['used'] >= $usage['max']) {
            throw new DomainException('Limite utenti raggiunto per il piano attivo. Disattiva un utente oppure aggiorna la licenza.');
        }
    }
}
