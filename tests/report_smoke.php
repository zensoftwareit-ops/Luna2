<?php

declare(strict_types=1);

use Luna\Service\ManagementReportService;
use PhpOffice\PhpSpreadsheet\IOFactory;

$base = dirname(__DIR__);
require $base . '/vendor/autoload.php';

$db = new PDO('sqlite::memory:');
$db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$db->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
$db->sqliteCreateFunction('NOW', static fn (): string => date('Y-m-d H:i:s'));
$schema = [
    'CREATE TABLE organizations(id INTEGER PRIMARY KEY,business_name TEXT)',
    'CREATE TABLE documents(id INTEGER PRIMARY KEY,organization_id INTEGER,document_date TEXT,number TEXT,counterparty_name TEXT,taxable_total REAL,vat_total REAL,total REAL,balance_due REAL,status TEXT,document_type TEXT,due_date TEXT)',
    'CREATE TABLE products(id INTEGER PRIMARY KEY,code TEXT,name TEXT)',
    'CREATE TABLE warehouses(id INTEGER PRIMARY KEY,code TEXT)',
    'CREATE TABLE inventory_balances(organization_id INTEGER,product_id INTEGER,warehouse_id INTEGER,quantity REAL,reserved_quantity REAL,minimum_stock REAL,average_cost REAL)',
    'CREATE TABLE projects(id INTEGER PRIMARY KEY,organization_id INTEGER,code TEXT,name TEXT,customer_name TEXT,status TEXT,budget REAL,progress_percent REAL)',
    'CREATE TABLE project_time_entries(project_id INTEGER,hours REAL,hourly_cost REAL,billing_status TEXT)',
    'CREATE TABLE project_expenses(project_id INTEGER,cost_amount REAL,billing_status TEXT)',
    'CREATE TABLE document_lines(document_id INTEGER,project_id INTEGER,taxable_amount REAL)',
    'CREATE TABLE users(id INTEGER PRIMARY KEY,organization_id INTEGER,name TEXT,active INTEGER)',
    'CREATE TABLE time_records(user_id INTEGER,work_date TEXT,record_type TEXT,hours REAL,overtime_hours REAL)',
    'CREATE TABLE report_exports(organization_id INTEGER,report_key TEXT,period_from TEXT,period_to TEXT,parameters_json TEXT,filename TEXT,checksum_sha256 TEXT,row_count INTEGER,generated_by INTEGER,generated_at TEXT)',
];
foreach ($schema as $sql) { $db->exec($sql); }
$db->exec("INSERT INTO organizations VALUES(1,'Azienda Demo')");
$db->exec("INSERT INTO documents VALUES(1,1,'2026-01-15','FT-1','Cliente Uno',1000,220,1220,600,'ISSUED','SALES_INVOICE','2026-02-15'),(2,1,'2026-01-20','FP-1','Fornitore Uno',400,88,488,488,'RECEIVED','PURCHASE_INVOICE','2026-02-20')");
$db->exec("INSERT INTO products VALUES(1,'P001','Prodotto Demo'); INSERT INTO warehouses VALUES(1,'MAIN'); INSERT INTO inventory_balances VALUES(1,1,1,20,3,5,12.5)");
$db->exec("INSERT INTO projects VALUES(1,1,'COM-1','Commessa Demo','Cliente Uno','ACTIVE',5000,40); INSERT INTO project_time_entries VALUES(1,10,25,'OPEN'); INSERT INTO project_expenses VALUES(1,100,'OPEN'); INSERT INTO document_lines VALUES(1,1,1000)");
$db->exec("INSERT INTO users VALUES(1,1,'Mario Rossi',1); INSERT INTO time_records VALUES(1,'2026-01-10','WORK',8,1)");
$directory = sys_get_temp_dir() . DIRECTORY_SEPARATOR . 'luna2-report-smoke';
$result = (new ManagementReportService($db, 1, 1))->generate('2026-01-01','2026-12-31',$directory);
if (!is_file($result['path']) || filesize($result['path']) < 5000) { throw new RuntimeException('XLSX non generato o vuoto.'); }
$reader = IOFactory::createReader('Xlsx');
$reader->setIncludeCharts(true);
$book = $reader->load($result['path']);
$expected = ['Dashboard','Vendite','Acquisti','Magazzino','Commesse','Tesoreria','HR','Controlli'];
if ($book->getSheetNames() !== $expected) { throw new RuntimeException('Fogli XLSX inattesi: ' . implode(', ', $book->getSheetNames())); }
if (!str_starts_with((string) $book->getSheetByName('Vendite')->getCell('D7')->getValue(), '=')) { throw new RuntimeException('Formula di controllo totale mancante.'); }
if (count($book->getSheetByName('Dashboard')->getChartCollection()) < 1) { throw new RuntimeException('Grafico dashboard mancante.'); }
echo 'Report XLSX verificato: ' . $result['path'] . PHP_EOL;
