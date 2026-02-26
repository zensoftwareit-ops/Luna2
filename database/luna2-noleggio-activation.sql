-- Luna2 Broker Noleggio Auto - Module Activation (demo-safe)

USE luna2;

INSERT INTO module_settings (code, name, description, enabled, updated_at)
VALUES
    ('CRM_BROKER_AUTO', 'CRM Broker Auto', 'Broker Auto - Gestione completa noleggio auto', TRUE, NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    enabled = VALUES(enabled),
    updated_at = NOW();
