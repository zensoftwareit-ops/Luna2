<?php

declare(strict_types=1);

$text = static fn (string $label, bool $required = false): array => ['label' => $label, 'type' => 'text', 'required' => $required];
$date = static fn (string $label, bool $required = false): array => ['label' => $label, 'type' => 'date', 'required' => $required];
$decimal = static fn (string $label, bool $required = false): array => ['label' => $label, 'type' => 'decimal', 'required' => $required];
$select = static fn (string $label, array $options, bool $required = false): array => ['label' => $label, 'type' => 'select', 'options' => $options, 'required' => $required];

return [
    'customers' => [
        'feature' => 'anagraphics', 'group' => 'Anagrafiche', 'title' => 'Clienti', 'singular' => 'Cliente', 'table' => 'customers',
        'title_column' => 'business_name', 'search' => ['code', 'business_name', 'vat_number', 'tax_code', 'email'],
        'columns' => ['code', 'business_name', 'vat_number', 'tax_code', 'city', 'email', 'active'],
        'fields' => [
            'code' => $text('Codice'), 'business_name' => $text('Ragione sociale', true),
            'vat_number' => $text('Partita IVA'), 'tax_code' => $text('Codice fiscale'),
            'sdi_code' => $text('Codice destinatario'), 'pec' => ['label' => 'PEC', 'type' => 'email'],
            'email' => ['label' => 'Email', 'type' => 'email'], 'phone' => $text('Telefono'),
            'address' => $text('Indirizzo'), 'postal_code' => $text('CAP'), 'city' => $text('CittÃ '),
            'province' => $text('Provincia'), 'country_code' => $text('Paese'), 'iban' => $text('IBAN'),
            'payment_terms' => $text('Condizioni di pagamento'), 'credit_limit' => $decimal('Fido'),
            'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1],
        ],
    ],
    'suppliers' => [
        'feature' => 'anagraphics', 'group' => 'Anagrafiche', 'title' => 'Fornitori', 'singular' => 'Fornitore', 'table' => 'suppliers',
        'title_column' => 'business_name', 'search' => ['code', 'business_name', 'vat_number', 'tax_code', 'email'],
        'columns' => ['code', 'business_name', 'vat_number', 'city', 'email', 'active'],
        'fields' => [
            'code' => $text('Codice'), 'business_name' => $text('Ragione sociale', true),
            'vat_number' => $text('Partita IVA'), 'tax_code' => $text('Codice fiscale'),
            'email' => ['label' => 'Email', 'type' => 'email'], 'pec' => ['label' => 'PEC', 'type' => 'email'],
            'phone' => $text('Telefono'), 'address' => $text('Indirizzo'), 'postal_code' => $text('CAP'),
            'city' => $text('CittÃ '), 'province' => $text('Provincia'), 'country_code' => $text('Paese'),
            'iban' => $text('IBAN'), 'payment_terms' => $text('Condizioni di pagamento'),
            'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1],
        ],
    ],
    'products' => [
        'feature' => 'inventory', 'group' => 'Vendite e magazzino', 'title' => 'Prodotti e servizi', 'singular' => 'Prodotto', 'table' => 'products',
        'title_column' => 'name', 'search' => ['code', 'sku', 'ean', 'name', 'category'],
        'columns' => ['code', 'name', 'category', 'unit', 'sale_price', 'purchase_cost', 'active'],
        'fields' => [
            'code' => $text('Codice', true), 'sku' => $text('SKU'), 'ean' => $text('EAN/barcode'),
            'name' => $text('Nome', true), 'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
            'category' => $text('Categoria'), 'product_type' => $select('Tipo', ['PRODUCT' => 'Prodotto', 'SERVICE' => 'Servizio'], true),
            'unit' => $text('UnitÃ  di misura', true), 'sale_price' => $decimal('Prezzo vendita'),
            'purchase_cost' => $decimal('Costo acquisto'), 'vat_rate' => $decimal('IVA %'),
            'track_inventory' => ['label' => 'Gestione giacenza', 'type' => 'checkbox'],
            'minimum_stock' => $decimal('Scorta minima'), 'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1],
        ],
    ],
    'leads' => [
        'feature' => 'crm', 'group' => 'CRM', 'title' => 'Lead', 'singular' => 'Lead', 'table' => 'leads',
        'title_column' => 'company_name', 'search' => ['company_name', 'contact_name', 'email', 'phone', 'source'],
        'columns' => ['company_name', 'contact_name', 'status', 'estimated_value', 'next_action_at', 'owner_name'],
        'fields' => [
            'company_name' => $text('Azienda', true), 'contact_name' => $text('Referente'),
            'email' => ['label' => 'Email', 'type' => 'email'], 'phone' => $text('Telefono'), 'source' => $text('Fonte'),
            'status' => $select('Stato', ['NEW' => 'Nuovo', 'CONTACTED' => 'Contattato', 'QUALIFIED' => 'Qualificato', 'PROPOSAL' => 'Proposta', 'NEGOTIATION' => 'Negoziazione', 'WON' => 'Vinto', 'LOST' => 'Perso'], true),
            'estimated_value' => $decimal('Valore stimato'), 'probability' => $decimal('ProbabilitÃ  %'),
            'next_action_at' => ['label' => 'Prossima azione', 'type' => 'datetime-local'], 'owner_name' => $text('Responsabile'),
            'notes' => ['label' => 'Note', 'type' => 'textarea'],
        ],
    ],
    'activities' => [
        'feature' => 'crm', 'group' => 'CRM', 'title' => 'AttivitÃ  CRM', 'singular' => 'AttivitÃ ', 'table' => 'activities',
        'title_column' => 'subject', 'search' => ['subject', 'activity_type', 'status', 'assigned_to'],
        'columns' => ['subject', 'activity_type', 'status', 'starts_at', 'due_at', 'assigned_to'],
        'fields' => [
            'subject' => $text('Oggetto', true), 'activity_type' => $select('Tipo', ['CALL' => 'Chiamata', 'EMAIL' => 'Email', 'MEETING' => 'Riunione', 'TASK' => 'AttivitÃ '], true),
            'status' => $select('Stato', ['OPEN' => 'Aperta', 'IN_PROGRESS' => 'In corso', 'DONE' => 'Completata', 'CANCELLED' => 'Annullata'], true),
            'starts_at' => ['label' => 'Inizio', 'type' => 'datetime-local'], 'due_at' => ['label' => 'Scadenza', 'type' => 'datetime-local'],
            'assigned_to' => $text('Assegnata a'), 'related_type' => $text('Tipo collegamento'), 'related_id' => ['label' => 'ID collegato', 'type' => 'number'],
            'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
        ],
    ],
    'warehouses' => [
        'feature' => 'inventory', 'group' => 'Vendite e magazzino', 'title' => 'Magazzini', 'singular' => 'Magazzino', 'table' => 'warehouses',
        'title_column' => 'name', 'search' => ['code', 'name', 'address'], 'columns' => ['code', 'name', 'address', 'active'],
        'fields' => ['code' => $text('Codice', true), 'name' => $text('Nome', true), 'address' => $text('Indirizzo'), 'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1]],
    ],
    'inventory-movements' => [
        'feature' => 'inventory', 'group' => 'Vendite e magazzino', 'title' => 'Movimenti magazzino', 'singular' => 'Movimento', 'table' => 'inventory_movements',
        'title_column' => 'reason', 'search' => ['product_code', 'movement_type', 'reason', 'document_number'],
        'columns' => ['movement_date', 'product_code', 'movement_type', 'quantity', 'unit_cost', 'warehouse_code', 'document_number'],
        'fields' => [
            'movement_date' => $date('Data', true), 'product_id' => ['label' => 'ID prodotto', 'type' => 'number', 'required' => true],
            'product_code' => $text('Codice prodotto'), 'warehouse_id' => ['label' => 'ID magazzino', 'type' => 'number', 'required' => true],
            'warehouse_code' => $text('Codice magazzino'), 'movement_type' => $select('Tipo', ['IN' => 'Carico', 'OUT' => 'Scarico', 'TRANSFER_IN' => 'Trasferimento in', 'TRANSFER_OUT' => 'Trasferimento out', 'ADJUSTMENT' => 'Rettifica'], true),
            'quantity' => $decimal('QuantitÃ ', true), 'unit_cost' => $decimal('Costo unitario'), 'reason' => $text('Causale', true),
            'document_type' => $text('Tipo documento'), 'document_number' => $text('Numero documento'),
        ],
    ],
    'projects' => [
        'feature' => 'projects', 'group' => 'OperativitÃ ', 'title' => 'Commesse', 'singular' => 'Commessa', 'table' => 'projects',
        'title_column' => 'name', 'search' => ['code', 'name', 'customer_name', 'status'],
        'columns' => ['code', 'name', 'customer_name', 'status', 'start_date', 'end_date', 'budget'],
        'fields' => [
            'code' => $text('Codice', true), 'name' => $text('Nome', true), 'customer_id' => ['label' => 'ID cliente', 'type' => 'number'],
            'customer_name' => $text('Cliente'), 'status' => $select('Stato', ['DRAFT' => 'Bozza', 'ACTIVE' => 'Attiva', 'SUSPENDED' => 'Sospesa', 'COMPLETED' => 'Completata', 'CANCELLED' => 'Annullata'], true),
            'start_date' => $date('Data inizio'), 'end_date' => $date('Data fine'), 'budget' => $decimal('Budget'),
            'progress_percent' => $decimal('Avanzamento %'), 'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
        ],
    ],
    'tax-deadlines' => [
        'feature' => 'accounting', 'group' => 'ContabilitÃ ', 'title' => 'Scadenzario fiscale', 'singular' => 'Scadenza', 'table' => 'tax_deadlines',
        'title_column' => 'description', 'search' => ['deadline_type', 'description', 'status', 'reference_period'],
        'columns' => ['due_date', 'deadline_type', 'description', 'reference_period', 'amount', 'status'],
        'fields' => [
            'due_date' => $date('Scadenza', true), 'deadline_type' => $select('Tipo', ['VAT' => 'IVA', 'F24' => 'F24', 'INPS' => 'INPS', 'WITHHOLDING' => 'Ritenute', 'LIPE' => 'LIPE', 'CU' => 'CU', 'MODEL_770' => '770', 'INCOME_TAX' => 'Redditi', 'OTHER' => 'Altro'], true),
            'description' => $text('Descrizione', true), 'reference_period' => $text('Periodo'), 'amount' => $decimal('Importo'),
            'status' => $select('Stato', ['OPEN' => 'Aperta', 'IN_PROGRESS' => 'In corso', 'COMPLETED' => 'Completata', 'OVERDUE' => 'Scaduta'], true),
            'completed_at' => ['label' => 'Completata il', 'type' => 'datetime-local'], 'notes' => ['label' => 'Note', 'type' => 'textarea'],
        ],
    ],
    'fixed-assets' => [
        'feature' => 'accounting', 'group' => 'ContabilitÃ ', 'title' => 'Cespiti', 'singular' => 'Cespite', 'table' => 'fixed_assets',
        'title_column' => 'description', 'search' => ['asset_code', 'description', 'cat×]ùÚÚ$z{-®éÜj×QS•’SPT–HÑVKˆÜ™Ø[š^˜][Û—ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ™[[ØÛÛ˜XÝÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ™XY[™×Ù]HUH“Õ•SˆÚ[ÛY]\œÈS•S”ÒQÓ‘Q“Õ•SˆÛÝ\˜ÙHS•SJ	ÓPS•PS	Ë	ÐÕTÕÓQT‰Ë	ÔÑT•’PÑIË	ÒSTÔ•	ÊH“Õ•SQUS	ÓPS•PS	Ëˆ›Ý\ÈTÒTŠL
H•SˆÜ™X]YØžH’QÒS•S”ÒQÓ‘Q•SˆÜ™X]YØ]SQTÕST“Õ•SQUSÕT”‘S•ÕSQTÕSTˆS’TUQHÑVH\WÜ™[[ÛY]\ˆ
™[[ØÛÛ˜XÝÚY™XY[™×Ù]JKˆÓÓ”ÕRS•š×Ü™[[ÛY]\—ÛÜ™È“Ô‘RQÓˆÑVH
Ü™Ø[š^˜][Û—ÚY
H‘Q‘T‘SÑTÈÜ™Ø[š^˜][ÛœÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü™[[ÛY]\—ØÛÛ˜XÝ“Ô‘RQÓˆÑVH
™[[ØÛÛ˜XÝÚY
H‘Q‘T‘SÑTÈ™[[ØÛÛ˜XÝÊY
HÓˆSUHÐTÐÐQBŠHS‘ÒS‘OR[››ÑˆQUSÒT”ÑU]]ŽXÓÓUO]]ŽXÝ[šXÛÙWØÚNÂ‚Ô‘PUHP“HQˆ“ÕVTÕÈ™[[ÝXÚÙ]Ù]™[È
ˆY’QÒS•S”ÒQÓ‘QUU×ÒSÔ‘SQS•’SPT–HÑVKˆÜ™Ø[š^˜][Û—ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ™[[ÝXÚÙ]ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ]™[Ý\HS•SJ	ÓÔS‘Q	Ë	ÐTÔÒQÓ‘Q	Ë	ÐÓÓSQS•	Ë	ÔÕUTÉË	ÔÓWÐ”‘PPÒ	Ë	Ô‘TÓÓ‘Q	Ë	ÐÓÔÑQ	ÊH“Õ•SˆÛÝ˜[YHTÒTŠNL
H•Sˆ™]×Ý˜[YHTÒTŠNL
H•Sˆ›Ý\ÈV•SˆXÝÜ—ÚY’QÒS•S”ÒQÓ‘Q•SˆÜ™X]YØ]SQTÕST“Õ•SQUSÕT”‘S•ÕSQTÕSTˆÓÓ”ÕRS•š×Ü™[[ÝXÚÙ]Ù]™[ÛÜ™È“Ô‘RQÓˆÑVH
Ü™Ø[š^˜][Û—ÚY
H‘Q‘T‘SÑTÈÜ™Ø[š^˜][ÛœÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü™[[ÝXÚÙ]Ù]™[ÝXÚÙ]“Ô‘RQÓˆÑVH
™[[ÝXÚÙ]ÚY
H‘Q‘T‘SÑTÈ™[[ÝXÚÙ]ÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü™[[ÝXÚÙ]Ù]™[ØXÝÜˆ“Ô‘RQÓˆÑVH
XÝÜ—ÚY
H‘Q‘T‘SÑTÈ\Ù\œÊY
HÓˆSUHÑU•SŠHS‘ÒS‘OR[››ÑˆQUSÒT”ÑU]]ŽXÓÓUO]]ŽXÝ[šXÛÙWØÚNÂ‚Ô‘PUHP“HQˆ“ÕVTÕÈ™[[Ú[›ÚXÙWÛ[šÜÈ
ˆY’QÒS•S”ÒQÓ‘QUU×ÒSÔ‘SQS•’SPT–HÑVKˆÜ™Ø[š^˜][Û—ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ™[[ØÛÛ˜XÝÚY’QÒS•S”ÒQÓ‘Q“Õ•SˆØÝ[Y[ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ\š[ÙÜÝ\UH“Õ•Sˆ\š[ÙÙ[™UH“Õ•Sˆ[[Ý[PÒSPS
MKŠH“Õ•SˆÜ™X]YØ]SQTÕST“Õ•SQUSÕT”‘S•ÕSQTÕSTˆS’TUQHÑVH\WÜ™[[Ú[›ÚXÙWÜ\š[Ù
™[[ØÛÛ˜XÝÚY\š[ÙÜÝ\\š[ÙÙ[™
KˆÓÓ”ÕRS•š×Ü™[[Ú[›ÚXÙWÛÜ™È“Ô‘RQÓˆÑVH
Ü™Ø[š^˜][Û—ÚY
H‘Q‘T‘SÑTÈÜ™Ø[š^˜][ÛœÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü™[[Ú[›ÚXÙWØÛÛ˜XÝ“Ô‘RQÓˆÑVH
™[[ØÛÛ˜XÝÚY
H‘Q‘T‘SÑTÈ™[[ØÛÛ˜XÝÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü™[[Ú[›ÚXÙWÙØÝ[Y[“Ô‘RQÓˆÑVH
ØÝ[Y[ÚY
H‘Q‘T‘SÑTÈØÝ[Y[ÊY
HÓˆSUHÐTÐÐQBŠHS‘ÒS‘OR[››ÑˆQUSÒT”ÑU]]ŽXÓÓUO]]ŽXÝ[šXÛÙWØÚNÂ‚Ô‘PUHP“HQˆ“ÕVTÕÈ™\ÜÙ^ÜÈ
ˆY’QÒS•S”ÒQÓ‘QUU×ÒSÔ‘SQS•’SPT–HÑVKˆÜ™Ø[š^˜][Û—ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ™\ÜÚÙ^HTÒTŠL
H“Õ•Sˆ\š[ÙÙœ›ÛHUH•Sˆ\š[ÙÝÈUH•Sˆ\˜[Y]\œ×ÚœÛÛˆ”ÓÓˆ•Sˆš[[˜[YHTÒTŠMJH“Õ•SˆÚXÚÜÝ[WÜÚLMˆÒTŠ
H•Sˆ›Ý×ØÛÝ[S•S”ÒQÓ‘Q“Õ•SQUSˆÙ[™\˜]YØžH’QÒS•S”ÒQÓ‘Q•SˆÙ[™\˜]YØ]UUSQH“Õ•SˆÑVHYÜ™\ÜÙ^Ü
Ü™Ø[š^˜][Û—ÚY™\ÜÚÙ^KÙ[™\˜]YØ]
KˆÓÓ”ÕRS•š×Ü™\ÜÙ^ÜÛÜ™È“Ô‘RQÓˆÑVH
Ü™Ø[š^˜][Û—ÚY
H‘Q‘T‘SÑTÈÜ™Ø[š^˜][ÛœÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü™\ÜÙ^ÜÝ\Ù\ˆ“Ô‘RQÓˆÑVH
Ù[™\˜]YØžJH‘Q‘T‘SÑTÈ\Ù\œÊY
HÓˆSUHÑU•SŠHS‘ÒS‘OR[››ÑˆQUSÒT”ÑU]]ŽXÓÓUO]]ŽXÝ[šXÛÙWØÚNÂ‚STˆP“HØ[[™\—ØXØÛÝ[ÂˆQÓÓSSˆ[™Ú[Ý\›TÒTŠL
H•SQ•TˆXØÛÝ[Ù[XZ[ˆQÓÓSSˆ]]Ý\HS•SJ	ÓÐUU‰Ë	ÐTÒPÉË	ÐTÔTÔÕÓÔ‘	Ë	Ð‘PT‘T‰ÊH“Õ•SQUS	ÓÐUU‰ÈQ•Tˆ[™Ú[Ý\›ˆQÓÓSSˆÙXÜ™]Ü™Y™\™[˜ÙHTÒTŠNL
H•SQ•Tˆ[˜Üž\YØÜ™Y[X[ËˆQÓÓSSˆ^\›˜[ØØ[[™\—ÚYTÒTŠMJH•SQ•TˆÙXÜ™]Ü™Y™\™[˜ÙKˆQÓÓSSˆ\ÝÙ\œ›ÜˆV•SQ•Tˆ\ÝÜÞ[˜×Ø]ˆQÓÓSSˆÜ™X]YØžH’QÒS•S”ÒQÓ‘Q•SQ•Tˆ\ÝÙ\œ›Ü‹ˆQÓÓSSˆ\]YØžH’QÒS•S”ÒQÓ‘Q•SQ•TˆÜ™X]YØžNÂ‚STˆP“HØ[[™\—Ù]™[ÂˆSÑQ–HÓÓSSˆ›ÝšY\ˆS•SJ	ÓÐÐS	Ë	ÑÓÓÑÓIË	ÒPÓÕQ	Ë	ÐÐSU‰ÊH“Õ•SQUS	ÓÐÐS	ËˆQÓÓSSˆ]YÈTÒTŠMJH•SQ•Tˆ›ÝšY\—Ù]™[ÚYˆQÓÓSSˆ\ÝÜÞ[˜ÙYØ]UUSQH•SQ•TˆÞ[˜×ÜÝ]\ËˆQÓÓSSˆÞ[˜×Ù\œ›ÜˆV•SQ•Tˆ\ÝÜÞ[˜ÙYØ]ˆQÓÓSSˆ[]YÙ^\›˜[S–RS•
JH“Õ•SQUSQ•TˆÞ[˜×Ù\œ›ÜŽÂ‚Ô‘PUHP“HQˆ“ÕVTÕÈØ[[™\—ÜÞ[˜×ÛÙÜÈ
ˆY’QÒS•S”ÒQÓ‘QUU×ÒSÔ‘SQS•’SPT–HÑVKˆÜ™Ø[š^˜][Û—ÚY’QÒS•S”ÒQÓ‘Q“Õ•SˆØ[[™\—ØXØÛÝ[ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ\™XÝ[ÛˆS•SJ	ÔTÒ	Ë	ÔS	Ë	Ð’QT‘PÕSÓS	ÊH“Õ•SˆÝ]\ÈS•SJ	Ô•S“’S‘ÉË	ÔÕPÐÑTÔÉË	ÔT•PS	Ë	ÑT”“Ô‰ÊH“Õ•Sˆ›ØÙ\ÜÙYØÛÝ[S•S”ÒQÓ‘Q“Õ•SQUSˆ\œ›Ü—ØÛÝ[S•S”ÒQÓ‘Q“Õ•SQUSˆÝ\YØ]UUSQH“Õ•Sˆ[™YØ]UUSQH•SˆY\ÜØYÙHV•SˆÓÓ”ÕRS•š×ØØ[[™\—ÜÞ[˜×ÛÙ×ÛÜ™È“Ô‘RQÓˆÑVH
Ü™Ø[š^˜][Û—ÚY
H‘Q‘T‘SÑTÈÜ™Ø[š^˜][ÛœÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×ØØ[[™\—ÜÞ[˜×ÛÙ×ØXØÛÝ[“Ô‘RQÓˆÑVH
Ø[[™\—ØXØÛÝ[ÚY
H‘Q‘T‘SÑTÈØ[[™\—ØXØÛÝ[ÊY
HÓˆSUHÐTÐÐQBŠHS‘ÒS‘OR[››ÑˆQUSÒT”ÑU]]ŽXÓÓUO]]ŽXÝ[šXÛÙWØÚNÂ‚STˆP“H^\›ÛÜ[œÂˆQÓÓSSˆØ[Ý[][Û—Û[ÙHS•SJ	ÐÓÓ‘’QÕT‘QÔUTÉË	ÒSTÔ•QÔVTÓTÉÊH“Õ•SQUS	ÐÓÓ‘’QÕT‘QÔUTÉÈQ•Tˆ\š[ÙÙ[™ˆQÓÓSSˆØÚ[XWÝ™\œÚ[ÛˆTÒTŠL
H“Õ•SQUS	ÒS•T“SLIÈQ•TˆØ[Ý[][Û—Û[ÙKˆQÓÓSSˆÛÛ™š\›YYØžH’QÒS•S”ÒQÓ‘Q•SQ•TˆÝ]\ËˆQÓÓSSˆÛÛ™š\›YYØ]UUSQH•SQ•TˆÛÛ™š\›YYØžKˆQÓÓSSˆØÚÙYØ]UUSQH•SQ•TˆÛÛ™š\›YYØ]ˆQÓÓSSˆZYØ]UUSQH•SQ•TˆØÚÙYØ]ˆQÓÓSSˆ›Ù™\ÜÚ[Û˜[Ý˜[Y][Û—Ü™Y™\™[˜ÙHTÒTŠNL
H•SQ•TˆZYØ]ˆQÓÓSSˆ˜[Y]YØ]UUSQH•SQ•Tˆ›Ù™\ÜÚ[Û˜[Ý˜[Y][Û—Ü™Y™\™[˜ÙKˆQÓÓSSˆ›Ý\ÈV•SQ•Tˆ˜[Y]YØ]ˆQÓÓ”ÕRS•š×Ü^\›ÛÜ[—ØÛÛ™š\›Y\ˆ“Ô‘RQÓˆÑVH
ÛÛ™š\›YYØžJH‘Q‘T‘SÑTÈ\Ù\œÊY
HÓˆSUHÑU•SÂ‚STˆP“H^\›ÛÜ[œÂˆ“ÔS‘V\WÜ^\›ÛÜ\š[ÙˆQS’TUQHÑVH\WÜ^\›ÛÜ\š[ÙÛ[ÙH
Ü™Ø[š^˜][Û—ÚY\š[ÙÜÝ\\š[ÙÙ[™Ø[Ý[][Û—Û[ÙJNÂ‚STˆP“H^\›ÛÙ]Z[ÂˆQÓÓSSˆ[\ÞY\—ØÛÛšX][Ûœ×Ø[[Ý[PÒSPS
MKŠH“Õ•SQUSQ•TˆÛÛšX][Ûœ×Ø[[Ý[ˆQÓÓSSˆ™Z[X\œÙ[Y[×Ø[[Ý[PÒSPS
MKŠH“Õ•SQUSQ•Tˆ^Ø[[Ý[ˆQÓÓSSˆYXÝ[Ûœ×Ø[[Ý[PÒSPS
MKŠH“Õ•SQUSQ•Tˆ™Z[X\œÙ[Y[×Ø[[Ý[ˆQÓÓSSˆ[\ÞY\—ØÛÜÝPÒSPS
MKŠH“Õ•SQUSQ•Tˆ™]Ø[[Ý[ˆQÓÓSSˆÛÝ\˜ÙWÜ™Y™\™[˜ÙHTÒTŠNL
H•SQ•Tˆ[\ÞY\—ØÛÜÝÂ‚STˆP“H^\›ÛÙ[\ÞYYWØÛÛ™šYÜÂˆQÓÓSSˆ[\ÞYYWØÛÙHTÒTŠL
H•SQ•Tˆ\Ù\—ÚYˆQÓÓSSˆÝ[™\™ÝÙYZÛWÚÝ\œÈPÒSPS
ËŠH“Õ•SQUSQ•Tˆ[\ÞYYWØÛÙKˆQÓÓSSˆ[\ÞY\—ØÛÛšX][Û—Ü˜]HPÒSPS
Ë
H“Õ•SQUSQ•Tˆ[˜Z[Ü˜]KˆQÓÓSSˆš^YÛ[ÛWØ[[Ý[PÒSPS
MKŠH“Õ•SQUSQ•Tˆ[\ÞY\—ØÛÛšX][Û—Ü˜]KˆQÓÓSSˆXÝ]™HS–RS•
JH“Õ•SQUSHQ•Tˆ˜[YÝËˆQÓÓSSˆÜ™X]YØžH’QÒS•S”ÒQÓ‘Q•SQ•TˆXÝ]™KˆQÓÓSSˆ\]YØžH’QÒS•S”ÒQÓ‘Q•SQ•TˆÜ™X]YØžNÂ‚Ô‘PUHP“HQˆ“ÕVTÕÈ^\›ÛØÛÛ\Û™[È
ˆY’QÒS•S”ÒQÓ‘QUU×ÒSÔ‘SQS•’SPT–HÑVKˆÜ™Ø[š^˜][Û—ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ^\›ÛÙ]Z[ÚY’QÒS•S”ÒQÓ‘Q“Õ•SˆÛÛ\Û™[ØÛÙHTÒTŠL
H“Õ•Sˆ\ØÜš\[ÛˆTÒTŠMJH“Õ•SˆÛÛ\Û™[Ý\HS•SJ	ÑPT“’S‘ÉË	ÑQPÕSÓ‰Ë	ÑSTÖQQWÐÓÓ•’P•USÓ‰Ë	ÑSTÖQT—ÐÓÓ•’P•USÓ‰Ë	ÕV	Ë	Ô‘RSP•T”ÑSQS•	ÊH“Õ•Sˆ]X[]HPÒSPS
L‹
H“Õ•SQUSKˆ˜]HPÒSPS
L‹
H“Õ•SQUSˆ[[Ý[PÒSPS
MKŠH“Õ•SˆÛÝ\˜ÙHS•SJ	ÐÐSÕSUQ	Ë	ÒSTÔ•Q	Ë	ÓPS•PSÐQ•TÕQS•	ÊH“Õ•SQUS	ÐÐSÕSUQ	ËˆS’TUQHÑVH\WÜ^\›ÛØÛÛ\Û™[
^\›ÛÙ]Z[ÚYÛÛ\Û™[ØÛÙJKˆÓÓ”ÕRS•š×Ü^\›ÛØÛÛ\Û™[ÛÜ™È“Ô‘RQÓˆÑVH
Ü™Ø[š^˜][Û—ÚY
H‘Q‘T‘SÑTÈÜ™Ø[š^˜][ÛœÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü^\›ÛØÛÛ\Û™[Ù]Z[“Ô‘RQÓˆÑVH
^\›ÛÙ]Z[ÚY
H‘Q‘T‘SÑTÈ^\›ÛÙ]Z[ÊY
HÓˆSUHÐTÐÐQBŠHS‘ÒS‘OR[››ÑˆQUSÒT”ÑU]]ŽXÓÓUO]]ŽXÝ[šXÛÙWØÚNÂ‚Ô‘PUHP“HQˆ“ÕVTÕÈ^\›ÛÚ[\ÜÜ›ÝÜÈ
ˆY’QÒS•S”ÒQÓ‘QUU×ÒSÔ‘SQS•’SPT–HÑVKˆÜ™Ø[š^˜][Û—ÚY’QÒS•S”ÒQÓ‘Q“Õ•Sˆ^\›ÛÜ[—ÚY’QÒS•S”ÒQÓ‘Q“Õ•SˆÛÝ\˜ÙWÙš[HTÒTŠMJH“Õ•SˆÛÝ\˜ÙWÜ›ÝÈS•S”ÒQÓ‘Q“Õ•Sˆ[\ÞYYWØÛÙHTÒTŠL
H•Sˆ›Ü›X[^™YÚœÛÛˆ”ÓÓˆ“Õ•SˆÝ]\ÈS•SJ	ÔÕQÑQ	Ë	ÒSTÔ•Q	Ë	ÑT”“Ô‰ÊH“Õ•SQUS	ÔÕQÑQ	Ëˆ\œ›Ü—ÛY\ÜØYÙHV•SˆÜ™X]YØ]SQTÕST“Õ•SQUSÕT”‘S•ÕSQTÕSTˆÑVHYÜ^\›ÛÚ[\Ü
Ü™Ø[š^˜][Û—ÚY^\›ÛÜ[—ÚYÝ]\ÊKˆÓÓ”ÕRS•š×Ü^\›ÛÚ[\ÜÛÜ™È“Ô‘RQÓˆÑVH
Ü™Ø[š^˜][Û—ÚY
H‘Q‘T‘SÑTÈÜ™Ø[š^˜][ÛœÊY
HÓˆSUHÐTÐÐQKˆÓÓ”ÕRS•š×Ü^\›ÛÚ[\ÜÜ[ˆ“Ô‘RQÓˆÑVH
^\›ÛÜ[—ÚY
H‘Q‘T‘SÑTÈ^\›ÛÜ[œÊY
HÓˆSUHÐTÐÐQBŠHS‘ÒS‘OR[››ÑˆQUSÒT”ÑU]]ŽXÓÓUO]]ŽXÝ[šXÛÙWØÚNÂ‚’S”ÑT•S•ÈÛÛ[][šXØ][Û—ÜÙ][™ÜÈ
Ü™Ø[š^˜][Û—ÚY
B”ÑSPÕY”“ÓHÜ™Ø[š^˜][ÛœÂ“ÓˆTPÐUHÑVHTUHÜ™Ø[š^˜][Û—ÚYHSQTÊÜ™Ø[š^˜][Û—ÚY
NÂ