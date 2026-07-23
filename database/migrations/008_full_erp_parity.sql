ALTER TABLE documents
    ADD COLUMN warehouse_id BIGINT UNSIGNED NULL AFTER source_document_id,
    ADD COLUMN project_id BIGINT UNSIGNED NULL AFTER warehouse_id,
    ADD COLUMN fulfillment_status ENUM('NOT_REQUIRED','OPEN','PARTIAL','FULFILLED','CANCELLED') NOT NULL DEFAULT 'NOT_REQUIRED' AFTER status,
    ADD COLUMN converted_total DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER balance_due,
    ADD KEY idx_documents_workflow (organization_id, document_type, fulfillment_status),
    ADD CONSTRAINT fk_documents_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;

ALTER TABLE document_lines
    ADD COLUMN source_line_id BIGINT UNSIGNED NULL AFTER document_id,
    ADD COLUMN warehouse_id BIGINT UNSIGNED NULL AFTER product_id,
    ADD COLUMN project_id BIGINT UNSIGNED NULL AFTER warehouse_id,
    ADD COLUMN converted_quantity DECIMAL(15,4) NOT NULL DEFAULT 0 AFTER quantity,
    ADD COLUMN reserved_quantity DECIMAL(15,4) NOT NULL DEFAULT 0 AFTER converted_quantity,
    ADD COLUMN fulfilled_quantity DECIMAL(15,4) NOT NULL DEFAULT 0 AFTER reserved_quantity,
    ADD KEY idx_document_line_source (organization_id, source_line_id),
    ADD CONSTRAINT fk_document_line_source FOREIGN KEY (source_line_id) REFERENCES document_lines(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_document_line_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_document_line_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS document_links (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    source_document_id BIGINT UNSIGNED NOT NULL,
    target_document_id BIGINT UNSIGNED NOT NULL,
    link_type ENUM('CONVERSION','MERGE','SPLIT','PROJECT_BILLING','ECOMMERCE_IMPORT','RENTAL_BILLING') NOT NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_document_link (organization_id, source_document_id, target_document_id, link_type),
    CONSTRAINT fk_document_link_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_link_source FOREIGN KEY (source_document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_link_target FOREIGN KEY (target_document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE inventory_movements
    ADD COLUMN movement_uuid CHAR(36) NULL AFTER id,
    ADD COLUMN source_type VARCHAR(50) NULL AFTER document_number,
    ADD COLUMN source_id BIGINT UNSIGNED NULL AFTER source_type,
    ADD COLUMN reversed_movement_id BIGINT UNSIGNED NULL AFTER source_id,
    ADD UNIQUE KEY uq_inventory_movement_uuid (organization_id, movement_uuid),
    ADD KEY idx_inventory_source (organization_id, source_type, source_id),
    ADD CONSTRAINT fk_inventory_reversal FOREIGN KEY (reversed_movement_id) REFERENCES inventory_movements(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS inventory_transfers (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    transfer_number VARCHAR(100) NOT NULL,
    transfer_date DATE NOT NULL,
    source_warehouse_id BIGINT UNSIGNED NOT NULL,
    destination_warehouse_id BIGINT UNSIGNED NOT NULL,
    status ENUM('DRAFT','CONFIRMED','IN_TRANSIT','RECEIVED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    notes TEXT NULL,
    confirmed_at DATETIME NULL,
    received_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_inventory_transfer (organization_id, transfer_number),
    CONSTRAINT fk_transfer_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_source_warehouse FOREIGN KEY (source_warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_transfer_destination_warehouse FOREIGN KEY (destination_warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_transfer_lines (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    transfer_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    requested_quantity DECIMAL(15,4) NOT NULL,
    shipped_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    received_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    unit_cost DECIMAL(15,4) NOT NULL DEFAULT 0,
    UNIQUE KEY uq_transfer_product (transfer_id, product_id),
    CONSTRAINT fk_transfer_line_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_line_transfer FOREIGN KEY (transfer_id) REFERENCES inventory_transfers(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_line_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_pick_lists (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    pick_number VARCHAR(100) NOT NULL,
    warehouse_id BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,
    status ENUM('OPEN','PICKING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    assigned_to BIGINT UNSIGNED NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pick_number (organization_id, pick_number),
    UNIQUE KEY uq_pick_document (organization_id, document_id),
    CONSTRAINT fk_pick_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_pick_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_assignee FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_pick_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    pick_list_id BIGINT UNSIGNED NOT NULL,
    document_line_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    barcode VARCHAR(190) NULL,
    required_quantity DECIMAL(15,4) NOT NULL,
    picked_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    status ENUM('OPEN','PARTIAL','PICKED','EXCEPTION') NOT NULL DEFAULT 'OPEN',
    UNIQUE KEY uq_pick_document_line (pick_list_id, document_line_id),
    CONSTRAINT fk_pick_item_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_item_list FOREIGN KEY (pick_list_id) REFERENCES inventory_pick_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_item_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_item_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_barcode_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    pick_list_id BIGINT UNSIGNED NULL,
    warehouse_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NULL,
    barcode VARCHAR(190) NOT NULL,
    quantity DECIMAL(15,4) NOT NULL DEFAULT 1,
    result ENUM('ACCEPTED','UNKNOWN','EXCESS','WRONG_WAREHOUSE','ERROR') NOT NULL,
    message VARCHAR(500) NULL,
    scanned_by BIGINT UNSIGNED NULL,
    scanned_at DATETIME NOT NULL,
    KEY idx_barcode_scan (organization_id, barcode, scanned_at),
    CONSTRAINT fk_barcode_event_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_barcode_event_pick FOREIGN KEY (pick_list_id) REFERENCES inventory_pick_lists(id) ON DELETE SET NULL,
    CONSTRAINT fk_barcode_event_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_barcode_event_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    CONSTRAINT fk_barcode_event_user FOREIGN KEY (scanned_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_time_entries (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    work_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    hours DECIMAL(7,2) NOT NULL,
    hourly_cost DECIMAL(12,4) NOT NULL DEFAULT 0,
    hourly_rate DECIMAL(12,4) NOT NULL DEFAULT 0,
    billable TINYINT(1) NOT NULL DEFAULT 1,
    billing_status ENUM('OPEN','INVOICED','NON_BILLABLE','CANCELLED') NOT NULL DEFAULT 'OPEN',
    document_line_id BIGINT UNSIGNED NULL,
    approved_by BIGINT UNSIGNED NULL,
    approved_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_time_open (organization_id, project_id, billing_status, work_date),
    CONSTRAINT fk_project_time_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_time_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_time_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_time_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_time_approver FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_expenses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    expense_date DATE NOT NULL,
    supplier_id BIGINT UNSIGNED NULL,
    description VARCHAR(500) NOT NULL,
    cost_amount DECIMAL(15,2) NOT NULL,
    billable_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    billable TINYINT(1) NOT NULL DEFAULT 0,
    billing_status ENUM('OPEN','INVOICED','NON_BILLABLE','CANCELLED') NOT NULL DEFAULT 'OPEN',
    source_document_id BIGINT UNSIGNED NULL,
    document_line_id BIGINT UNSIGNED NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_expense_open (organization_id, project_id, billing_status, expense_date),
    CONSTRAINT fk_project_expense_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_expense_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_expense_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_expense_document FOREIGN KEY (source_document_id) REFERENCES documents(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_expense_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_milestones (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(255) NOT NULL,
    due_date DATE NULL,
    billing_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    status ENUM('OPEN','READY','INVOICED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    document_line_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_milestone_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_milestone_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_milestone_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS communication_settings (
    organization_id BIGINT UNSIGNED PRIMARY KEY,
    transport ENUM('SMTP','SENDMAIL','DISABLED') NOT NULL DEFAULT 'DISABLED',
    host VARCHAR(255) NULL,
    port SMALLINT UNSIGNED NOT NULL DEFAULT 587,
    encryption ENUM('NONE','TLS','SMTPS') NOT NULL DEFAULT 'TLS',
    username VARCHAR(190) NULL,
    secret_reference VARCHAR(190) NULL,
    from_email VARCHAR(190) NULL,
    from_name VARCHAR(190) NULL,
    reply_to VARCHAR(190) NULL,
    tracking_base_url VARCHAR(500) NULL,
    tracking_enabled TINYINT(1) NOT NULL DEFAULT 1,
    max_attempts TINYINT UNSIGNED NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_communication_setting_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbound_emails (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    message_uuid CHAR(36) NOT NULL,
    document_id BIGINT UNSIGNED NULL,
    recipient VARCHAR(190) NOT NULL,
    cc VARCHAR(1000) NULL,
    bcc VARCHAR(1000) NULL,
    subject VARCHAR(255) NOT NULL,
    html_body MEDIUMTEXT NOT NULL,
    text_body MEDIUMTEXT NULL,
    attachment_json JSON NULL,
    tracking_token CHAR(64) NOT NULL,
    status ENUM('DRAFT','QUEUED','SENDING','SENT','RETRY','FAILED','CANCELLED') NOT NULL DEFAULT 'QUEUED',
    attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NULL,
    sent_at DATETIME NULL,
    message_id VARCHAR(255) NULL,
    error_message TEXT NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_outbound_message_uuid (organization_id, message_uuid),
    UNIQUE KEY uq_outbound_tracking_token (tracking_token),
    KEY idx_outbound_queue (status, next_attempt_at),
    CONSTRAINT fk_outbound_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_document FORMwï¾m¢G§²ÚîÆ­yÖ–æS£ÒçW&–öBÖÆ&VÇ¶föçB×6—¦S£ƒ¶6öÆ÷#§f"‚ÒÖ×WFVB“¶föçB×vV–v‡C£sÒç7FGW2×÷7FVBÂç7FGW2×7V&Ö—GFVBÂç7FGW2×–BÂç7FGW2Ö6Æ7VÆFVG¶&6¶w&÷VæC¢6Sfc†Vc¶6öÆ÷#¢3vSÒç7FGW2×&WfW'6VG¶&6¶w&÷VæC¢6ffV&VC¶6öÆ÷#¢6#S&c6GÒæ66÷VçF–ærÖÆ–æW2Ö6&BFfö÷BF‡¶&6¶w&÷VæC¢6c†ff3¶6öÆ÷#§f"‚ÒÖ–æ²×6ögB“¶föçB×6—¦S£‡Òæ66÷VçF–ærÖÆ–æW2Ö6&B¶FFÖ&Ææ6R×7FGW5×¶föçB×6—¦S£—‡Òæ66÷VçF–ærÖÆ–æW2Ö6&B¶FFÖ&Ææ6R×7FGW5Òæ&Ææ6VG¶6öÆ÷#§f"‚Ò×7V66W72—Òæ66÷VçF–ærÖÆ–æW2Ö6&B¶FFÖ&Ææ6R×7FGW5ÒçVæ&Ææ6VG¶6öÆ÷#§f"‚ÒÖFævW"—Òæ66÷VçF–ærÖ7F–öç7·÷6—F–öã§7F–6·“¶&÷GFöÓ£·¢Ö–æFWƒ£#¶&÷&FW#£‚6öÆ–Bf"‚ÒÖÆ–æR“¶&÷&FW"×&F—W3£'ƒ¶&÷‚×6†F÷s£Ów‚#'‚&v&ƒRÃ#2ÃC"Âãb“¶Ö&v–â×F÷£G‡Òæ66÷VçF–ærÖæ÷FW·FF–æs£—‚#‡Òæ66÷VçF–ærÖæ÷FR¶Ö&v–ã£g‚¶6öÆ÷#§f"‚ÒÖ–æ²×6ögB—Òæ66÷VçF–ær×F'7¶F—7Æ“¦fÆWƒ¶v£Wƒ¶&6¶w&÷VæC¢6S–VVcS¶&÷&FW"×&F—W3£ƒ·FF–æs£Gƒ·v–GFƒ¦Ö‚Ö6öçFVçC¶Ö‚×v–GFƒ£S¶Ö&v–âÖ&÷GFöÓ£g‡Òæ66÷VçF–ær×F'2·FF–æs£‡‚Gƒ¶&÷&FW"×&F—W3£‡ƒ¶6öÆ÷#§f"‚ÒÖ×WFVB“¶föçB×6—¦S£ƒ¶föçB×vV–v‡C£sSÒæ66÷VçF–ær×F'2æ7F—fW¶&6¶w&÷VæC¢6ffc¶6öÆ÷#§f"‚ÒÖ&ÇVRÓs“¶&÷‚×6†F÷s§f"‚Ò×6†F÷r×6Ò—ÒçfBÖw&–G¶Ö&v–âÖ&÷GFöÓ£#‡ÒçfB×7VÖÖ'—¶Æ–vâ×6VÆc§7F'GÒçfBÖÖçVÂ×æVÇ¶Ö&v–â×F÷£'‡Òæf–VÆB6ÖÆÇ¶föçB×6—¦S£—ƒ¶6öÆ÷#§f"‚ÒÖ×WFVB“¶föçB×vV–v‡C£SÒææWWG&ÂÖ&ææW'¶&6¶w&÷VæC¢6cVc†f3¶&÷&FW"Ö6öÆ÷#¢6F6SFVS¶6öÆ÷#¢3C3S3fÒææWWG&ÂÖ&ææW"ç7—7FVÒÖ&ææW"Ö–6öç¶&6¶w&÷VæC¢6SvVFcS¶6öÆ÷#¢3S#cCvÒææWWG&ÂÖ&ææW"F—b7ç¶6öÆ÷#¢3cCsC†'Òç6WGFÆVÖVçBÖf÷&×¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ"ÆÖ–æÖ‚ƒÃg"’—Òç6WGFÆVÖVçB×7FGW7¶föçB×6—¦S£ƒ·FF–æs£w‚‡Òç6WGFÆVÖVçBÖ·—7¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒRÆÖ–æÖ‚ƒÃg"’—Òç6WGFÆVÖVçBÖ7F–öç7¶F—7Æ“¦fÆWƒ¶Æ–vâÖ—FV×3¦6VçFW#¶§W7F–g’Ö6öçFVçC§76RÖ&WGvVVã¶v£#ƒ·FF–æs£#‡Òç6WGFÆVÖVçBÖ7F–öç2ƒ'¶föçB×6—¦S£gƒ¶Ö&v–ã£'‚Òç6WGFÆVÖVçBÖ7F–öç2¶Ö&v–ã£G‚¶6öÆ÷#§f"‚ÒÖ×WFVB“¶föçB×6—¦S£‡Òç6WGFÆVÖVçBÖ7F–öç2f÷&×¶Ö&v–ã£ÒæVçG'’Ö·—7¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒBÃg"—Ğ ¢ò¢WF†VçF–6F–öâ¢ğ¢æWF‚×vW¶&6¶w&÷VæC¢6ffgÒæWF‚×6†VÆÇ¶Ö–âÖ†V–v‡C£fƒ¶F—7Æ“¦w&–C¶w&–B×FV×ÆFRÖ6öÇVÖç3¦Ö–æÖ‚ƒC3‚ÃC"R’g'ÒæWF‚×f—7VÇ·÷6—F–öã§&VÆF—fS¶÷fW&fÆ÷s¦†–FFVã¶&6¶w&÷VæC§&F–ÂÖw&F–VçB†6—&6ÆRBRRRÂ336SsÇG&ç7&VçB3"R’Ç&F–ÂÖw&F–VçB†6—&6ÆRB“R“RÂ3S&cSBÇG&ç7&VçB3‚R’Çf"‚ÒÖæg’Ó“S“¶6öÆ÷#¢6ffgÒæWF‚×f—7VÂÖw&–G·÷6—F–öã¦'6öÇWFS¶–ç6WC£¶÷6—G“¢ã“¶&6¶w&÷VæBÖ–ÖvS¦Æ–æV"Öw&F–VçB‡&v&ƒ#SRÃ#SRÃ#SRÂã#R’‚ÇG&ç7&VçB‚’ÆÆ–æV"Öw&F–VçBƒ“FVrÇ&v&ƒ#SRÃ#SRÃ#SRÂã#R’‚ÇG&ç7&VçB‚“¶&6¶w&÷VæB×6—¦S£C‡‚C‡ƒ¶Ö6²Ö–ÖvS¦Æ–æV"Öw&F–VçBƒ3VFVrÂ3ÇG&ç7&VçBsRR—ÒæWF‚×f—7VÃ¦gFW'¶6öçFVçC¢"#·÷6—F–öã¦'6öÇWFS·v–GFƒ£3ƒ¶†V–v‡C£3ƒ¶&÷&FW#£‚6öÆ–B&v&ƒ“‚ÃcBÃ#SRÂã#"“¶&÷&FW"×&F—W3£SS·&–v‡C¢Óƒƒ·F÷£#bS¶&÷‚×6†F÷s£c‚&v&ƒsbÃ3’Ã#3"ÂãB’Ã#‚&v&ƒsbÃ3’Ã#3"Âã#R—ÒæWF‚×f—7VÂÖ6öçFVçG·÷6—F–öã§&VÆF—fS·¢Ö–æFWƒ£#¶†V–v‡C£S·FF–æs£C‡‚SGƒ¶F—7Æ“¦fÆWƒ¶fÆW‚ÖF—&V7F–öã¦6öÇVÖã¶§W7F–g’Ö6öçFVçC§76RÖ&WGvVVçÒæWF‚ÖÆöv÷¶F—7Æ“¦fÆWƒ¶Æ–vâÖ—FV×3¦6VçFW#¶v£ƒ¶föçB×6—¦S£#‡ÒæWF‚×f—7VÂÖ6öçFVçBƒ¶föçB×6—¦S£C'ƒ¶Æ–æRÖ†V–v‡C£ã#¶ÆWGFW"×76–æs¢ÒãCVVÓ¶Ö&v–ã£'‚‡‡ÒæWF‚×f—7VÂÖ6öçFVçB¶Ö‚×v–GFƒ£CCƒ¶6öÆ÷#¢3–V#3c¶föçB×6—¦S£W‡ÒæWF‚Ö÷fW&Æ–æW¶6öÆ÷#¢3fV†fc·FW‡B×G&ç6f÷&Ó§WW&66S¶ÆWGFW"×76–æs¢ãFVÓ¶föçB×6—¦S£ƒ¶föçB×vV–v‡C£ƒÒæWF‚×f—7VÂÖ6öçFVçCç6ÖÆÇ¶6öÆ÷#¢3c3sƒ“#¶föçB×6—¦S£‡ÒæWF‚Öf÷&Ò×æVÇ¶F—7Æ“¦w&–C·Æ6RÖ—FV×3¦6VçFW#·FF–æs£3gƒ¶&6¶w&÷VæC¢6ffgÒæWF‚Ö6&G·v–GFƒ¦Ö–âƒC‚ÃR—ÒæWF‚Ö6&BÖ†VBƒ'¶föçB×6—¦S£3ƒ¶ÆWGFW"×76–æs¢Òã3VVÓ¶Ö&v–ã£—‚W‡ÒæWF‚Ö6&BÖ†VB¶6öÆ÷#§f"‚ÒÖ×WFVB“¶Ö&v–ã£#W‡ÒæÖö&–ÆRÖWF‚ÖÆöv÷¶F—7Æ“¦æöæWÒç7F6²Öf÷&×¶F—7Æ“¦w&–C¶v£g‡Òç7F6²Öf÷&ÒÆ&VÇ¶föçB×6—¦S£‡Òç7F6²Öf÷&Ò–çWG¶†V–v‡C£CWƒ¶&6¶w&÷VæC¢6f&f6fWÒæWF‚×7V&Ö—G¶†V–v‡C£Cgƒ¶Ö&v–â×F÷£Wƒ·v–GFƒ£S¶§W7F–g’Ö6öçFVçC§76RÖ&WGvVVã·FF–æs£w‡ÒæWF‚×6V7W&—G—·FW‡BÖÆ–vã¦6VçFW#¶6öÆ÷#§f"‚ÒÖ×WFVBÖÆ–v‡B“¶föçB×6—¦S£—ƒ¶Ö&v–â×F÷£#‡Ğ ¢ò¢ÆVv7’Ö6ö×F–&ÆR6ö×öæVçG2¢ğ¢çGvòÖ6öÇVÖç7¶F—7Æ“¦w&–C¶w&–B×FV×ÆFRÖ6öÇVÖç3£ãFg"g#¶v£#‡ÒæÖWG&–2Öw&–Bæ6ö×7G¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒRÆÖ–æÖ‚ƒÃg"’—ÒæV×G’×7FFW·FW‡BÖÆ–vã¦6VçFW#·FF–æs£f‚#‡ÒæV×G’Ö–6öç¶F—7Æ“¦–æÆ–æRÖw&–C·Æ6RÖ—FV×3¦6VçFW#·v–GFƒ£cƒ¶†V–v‡C£cƒ¶&÷&FW"×&F—W3£SS¶&6¶w&÷VæC§f"‚ÒÖFævW"Ö&r“¶6öÆ÷#§f"‚ÒÖFævW"“¶föçB×6—¦S£3ƒ¶föçB×vV–v‡C£ƒÒæ§6öâ×&Wf–Ww·v†—FR×76S§&R×w&¶Ö‚×v–GFƒ£s#ƒ¶Ö‚Ö†V–v‡C£##ƒ¶÷fW&fÆ÷s¦WFó¶föçB×6—¦S£ƒ¶Ö&v–ã£Òæ–æÆ–æRÖf÷&×¶Æ–vâÖ—FV×3¦VæGĞ ¤ÖVF–†Ö‚×v–GFƒ£3ƒ‚—²æÖWG&–2Öw&–G¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ2Ãg"—ÒæÖöGVÆRÖw&–G¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ"Ãg"—×Ğ¤ÖVF–†Ö‚×v–GFƒ£ƒ‚—²ç7âÓrÂç7âÓW¶w&–BÖ6öÇVÖã£òÓÒæf÷&ÒÖw&–G¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ"ÆÖ–æÖ‚ƒÃg"’—ÒæWF‚×6†VÆÇ¶w&–B×FV×ÆFRÖ6öÇVÖç3£3ƒ‚g'ÒæWF‚×f—7VÂÖ6öçFVçG·FF–æs£C‡ÒæWF‚×f—7VÂÖ6öçFVçBƒ¶föçB×6—¦S£3G‡Òç6WGFÆVÖVçBÖ·—7¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ2Ãg"—×Ğ¤ÖVF–†Ö‚×v–GFƒ£ƒS‚—²æ×6†VÆÇ¶F—7Æ“¦&Æö6·Òç6–FV&'·G&ç6f÷&Ó§G&ç6ÆFU‚‚ÓRR“·G&ç6—F–öã§G&ç6f÷&Òã'2V6S¶&÷‚×6†F÷s£#‚C‚&v&ƒRÃ"Ã#2Âã#"—Òç6–FV&"æ÷Vç·G&ç6f÷&Ó§G&ç6ÆFU‚ƒ—Òç6–FV&"Ö6Æ÷6W¶F—7Æ“¦&Æö6·Òç6–FV&"Ö&6¶G&÷·÷6—F–öã¦f—†VC¶–ç6WC£¶&÷&FW#£¶&6¶w&÷VæC§&v&ƒ‚ÃRÃ#‚ÂãR“·¢Ö–æFWƒ£3WÒæÖVçRÖ÷Vâç6–FV&"Ö&6¶G&÷¶F—7Æ“¦&Æö6·ÒæÖ–âÖ6öçFVçG¶Ö–âÖ†V–v‡C£f‡ÒæÖVçR×FövvÆW¶F—7Æ“¦w&–C·Æ6RÖ—FV×3¦6VçFW'ÒçF÷&'¶†V–v‡C£cGƒ·FF–æs£g‡ÒçvW·FF–æs£#'‚w‚C‡Òç7F–6·’×6fW¶ÆVgC£·FF–æs£‚w‡Òç6WGF–æw2Öw&–G¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'ÒæWF‚×6†VÆÇ¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'ÒæWF‚×f—7VÇ¶F—7Æ“¦æöæWÒæÖö&–ÆRÖWF‚ÖÆöv÷¶F—7Æ“¦fÆWƒ¶Æ–vâÖ—FV×3¦6VçFW#¶v£—ƒ¶föçB×6—¦S£‡ƒ¶föçB×vV–v‡C£sS¶Ö&v–âÖ&÷GFöÓ£3g‡ÒæÖöGVÆRÖw&–G¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òæ7&VFVçF–Ç2Ö6&G¶w&–B×FV×ÆFRÖ6öÇVÖç3£S‚g'Òæ7&VFVçF–Ç2Ö6&BFÇ¶w&–BÖ6öÇVÖã£òÓÒæ÷&væ—¦F–öâ×7v—F6†W'¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òæ÷&væ—¦F–öâÖÆ—7G¶§W7F–g’Ö6öçFVçC¦fÆW‚×7F'G×Ğ¤ÖVF–†Ö‚×v–GFƒ£cC‚—²çvRÖ–çG&òÂçvRÖ–çG&òæ6ö×7G¶Æ–vâÖ—FV×3¦fÆW‚×7F'C¶fÆW‚ÖF—&V7F–öã¦6öÇVÖçÒçvRÖ–çG&òƒÂæF6†&ö&BÖ–çG&òƒ¶föçB×6—¦S£#W‡Òæ–çG&òÖ7F–öç2Âæ–æÆ–æRÖ7F–öç7·v–GFƒ£WÒæ–çG&òÖ7F–öç2æ'WGFöâÂæ–æÆ–æRÖ7F–öç2æ'WGFöç¶fÆWƒ£ÒæÖWG&–2Öw&–BÂæÖWG&–2Öw&–Bæ6ö×7G¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ"Ãg"—ÒæÖWG&–7¶Ö–âÖ†V–v‡C£ƒ·FF–æs£G‡ÒæÖWG&–27G&öæw¶föçB×6—¦S£g‡Òæf÷&ÒÖw&–G¶w&–B×FV×ÆFRÖ6öÇVÖç3£g#·FF–æs£w‡ÒægVÆÂ×v–GF‡¶w&–BÖ6öÇVÖã¦WF÷ÒæÆ—7B×FööÆ&"ÂçvRÖ7F–öç7¶Æ–vâÖ—FV×3§7G&WF6ƒ¶fÆW‚ÖF—&V7F–öã¦6öÇVÖçÒç6V&6‚Öf÷&×·v–GFƒ£S¶fÆW‚×w&§w&Òç6V&6‚Ö6öçG&öÇ¶Ö–â×v–GFƒ£¶fÆWƒ£ÒçF÷&"Ö7F–öç2æVçf—&öæÖVçB×–ÆÇ¶F—7Æ“¦æöæWÒçV–6²Ö7&VFR7ç¶F—7Æ“¦æöæWÒæ6öçFVçBÖw&–G¶F—7Æ“¦&Æö6·Òç6WGF–æw2Öw&–G¶F—7Æ“¦&Æö6·Òç6WGF–æw2Ö6&G¶Ö&v–âÖ&÷GFöÓ£W‡Òç7F–6·’×6fSæF—g¶F—7Æ“¦æöæWÒç7F–6·’×6fW¶§W7F–g’Ö6öçFVçC¦fÆW‚ÖVæGÒæW'&÷"×æVÇ·FF–æs£#‡‚#ƒ¶Ö&v–ã£'f‚WF÷ÒæW'&÷"Ö7F–öç7¶fÆW‚ÖF—&V7F–öã¦6öÇVÖã·v–GFƒ£WÒæFö7VÖVçBÖ†VG¶Æ–vâÖ—FV×3¦fÆW‚×7F'C¶fÆW‚ÖF—&V7F–öã¦6öÇVÖã¶v£‡‡ÒæFö7VÖVçB×F÷FÇ·FW‡BÖÆ–vã¦ÆVgGÒæ6ö×7BÖÆ—7G¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òæ7&VFVçF–Ç2Ö6&G¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òæ7&VFVçF–Ç2Ö6&BFÇ¶w&–BÖ6öÇVÖã¦WF÷Òæ7&VFVçF–Ç2Ö6&BFÂF—g¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òæ÷&væ—¦F–öâÖ6†—·v–GFƒ£WÒæ÷&væ—¦F–öâÖÆ—7BÂæ÷&væ—¦F–öâÖÆ—7Bf÷&×·v–GFƒ£WÒç6WGW×æVÃç7VÖÖ'—¶w&–B×FV×ÆFRÖ6öÇVÖç3£C'‚Ö–æÖ‚ƒÃg"’g‡Òæ66÷VçF–ærÖ·—2ÂæVçG'’Ö·—2Âç6WGFÆVÖVçBÖ·—7¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ"Ãg"—Òç6WGFÆVÖVçBÖf÷&×¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òç6WGFÆVÖVçBÖ7F–öç7¶Æ–vâÖ—FV×3¦fÆW‚×7F'C¶fÆW‚ÖF—&V7F–öã¦6öÇVÖçÒæ66÷VçF–ær×F'7·v–GFƒ£S¶÷fW&fÆ÷s¦WF÷Òæ66÷VçF–ær×F'2¶fÆWƒ£·FW‡BÖÆ–vã¦6VçFW#·v†—FR×76S¦æ÷w&Òç7—7FVÒÖ&ææW'¶w&–B×FV×ÆFRÖ6öÇVÖç3£C'‚g'Òç7—7FVÒÖ÷væW"Öæ÷FW¶w&–BÖ6öÇVÖã£òÓÒæf–ÇFW"Öf÷&Òç6V&6‚Ö6öçG&öÇ¶fÆW‚Ö&6—3£W×Ğ¤ÖVF–†Ö‚×v–GFƒ£C#‚—²æÖWG&–2Öw&–BÂæÖWG&–2Öw&–Bæ6ö×7G¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'ÒæWF‚Öf÷&Ò×æVÇ·FF–æs£#W‡ÒæWF‚Ö6&BÖ†VBƒ'¶föçB×6—¦S£#w‡ÒæÖöGVÆRÖ6&G¶w&–B×FV×ÆFRÖ6öÇVÖç3£C'‚Ö–æÖ‚ƒÃg"’WF÷ÒçF÷&"×F—FÆR7ç¶F—7Æ“¦æöæW×Ğ ¢ò¢66÷VçF–ærv÷&·76RBã¢ğ¢æÖ–ærÖw&–G¶F—7Æ“¦w&–C¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ"ÆÖ–æÖ‚ƒÃg"’“¶v£‡ƒ·FF–æs£‡‚#‚#‡ÒæÖ–ær×&÷w¶F—7Æ“¦w&–C¶w&–B×FV×ÆFRÖ6öÇVÖç3¦Ö–æÖ‚ƒS‚Âã†g"’Ö–æÖ‚ƒ##‚Ãã&g"’WFó¶Æ–vâÖ—FV×3¦6VçFW#¶v£ƒ·FF–æs£‚¶&÷&FW"Ö&÷GFöÓ£‚6öÆ–Bf"‚ÒÖÆ–æR×6ögB—ÒæÖ–ær×&÷rÆ&VÇ¶F—7Æ“¦w&–GÒæÖ–ær×&÷rÆ&VÂ7G&öæw¶föçB×6—¦S£‡ÒæÖ–ær×&÷rÆ&VÂ6ÖÆÇ¶föçB×6—¦S£‡ƒ¶6öÆ÷#§f"‚ÒÖ×WFVB“¶föçBÖfÖ–Ç“§V’ÖÖöæ÷76RÆÖöæ÷76WÒæÖ–ær×&÷r6VÆV7BÂæ6ö×7BÖ7F–öâ6VÆV7G¶†V–v‡C£3‡ƒ¶&÷&FW#£‚6öÆ–B6CFF6Ss¶&÷&FW"×&F—W3£—ƒ¶&6¶w&÷VæC¢6ffc·FF–æs£w‚—ƒ¶6öÆ÷#§f"‚ÒÖ–æ²—Òç6–ævÆRÖ6&BÖf÷&×¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVBƒ"ÆÖ–æÖ‚ƒÃg"’—Òæ6ö×Æ–æ6RÖ7F–öç7¶F—7Æ“¦w&–C¶w&–B×FV×ÆFRÖ6öÇVÖç3£g"g#¶v£'ƒ·FF–æs£g‚#ƒ¶&÷&FW"Ö&÷GFöÓ£‚6öÆ–Bf"‚ÒÖÆ–æR×6ögB—Òæ6ö×7BÖ7F–öç¶F—7Æ“¦fÆWƒ¶Æ–vâÖ—FV×3¦VæC¶§W7F–g’Ö6öçFVçC§76RÖ&WGvVVã¶v£Gƒ·FF–æs£7ƒ¶&÷&FW#£‚6öÆ–Bf"‚ÒÖÆ–æR“¶&÷&FW"×&F—W3£ƒ¶&6¶w&÷VæC¢6ff&fGÒæ6ö×7BÖ7F–öãç7âÂæ6ö×7BÖ7F–öâÆ&VÇ¶F—7Æ“¦w&–C¶v£7ƒ¶föçB×6—¦S£‡Òæ6ö×7BÖ7F–öâ6ÖÆÇ¶6öÆ÷#§f"‚ÒÖ×WFVB—Òç7FFVÖVçB×7VÖÖ'—¶F—7Æ“¦w&–C¶w&–B×FV×ÆFRÖ6öÇVÖç3§&WVB†WFòÖf—BÆÖ–æÖ‚ƒƒ‚Ãg"’“¶v£ƒ·FF–æs£g‚#ƒ¶&÷&FW"Ö&÷GFöÓ£‚6öÆ–Bf"‚ÒÖÆ–æR×6ögB—Òç7FFVÖVçB×7VÖÖ'’'F–6ÆW¶F—7Æ“¦w&–C¶v£Gƒ·FF–æs£'ƒ¶&÷&FW#£‚6öÆ–Bf"‚ÒÖÆ–æR“¶&÷&FW"×&F—W3£‡Òç7FFVÖVçB×7VÖÖ'’7ç¶föçB×6—¦S£‡ƒ¶6öÆ÷#§f"‚ÒÖ×WFVB“¶föçB×vV–v‡C£sSÒç7FFVÖVçB×7VÖÖ'’7G&öæw¶föçB×6—¦S£W‡ÒæVÖ&VFFVBÖw&–G·FF–æs£#‡ÒæVÖ&VFFVBÖw&–CæF—g¶&÷&FW#£‚6öÆ–Bf"‚ÒÖÆ–æR“¶&÷&FW"×&F—W3£'ƒ¶÷fW&fÆ÷s¦†–FFVçÒæVÖ&VFFVBÖw&–Bf÷&×¶†V–v‡C£WĞ¤ÖVF–†Ö‚×v–GFƒ£ƒ‚—²æÖ–ærÖw&–G¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'×Ğ¤ÖVF–†Ö‚×v–GFƒ£cC‚—²ç6–ævÆRÖ6&BÖf÷&×¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'ÒæÖ–ær×&÷w¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òæ6ö×Æ–æ6RÖ7F–öç7¶w&–B×FV×ÆFRÖ6öÇVÖç3£g'Òæ6ö×7BÖ7F–öç¶Æ–vâÖ—FV×3§7G&WF6ƒ¶fÆW‚ÖF—&V7F–öã¦6öÇVÖç×Ğ