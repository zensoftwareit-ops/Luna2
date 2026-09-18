<?php
declare(strict_types=1);

require dirname(__DIR__) . '/vendor/autoload.php';

use Luna\Service\ImportService;
use Luna\Service\VatService;

$source = $argv[1] ?? '';
if (!is_file($source)) throw new RuntimeException('Pass the DATEV VAT CSV path.');
$db = new PDO('mysql:host=127.0.0.1;port=33317;charset=utf8mb4','root','',[
    PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES=>false,
]);
$name = 'luna_vat_test_' . bin2hex(random_bytes(5));
$db->exec("CREATE DATABASE `$name` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$db->exec("USE `$name`");
$root = dirname(__DIR__) . '/storage/private/datev-vat-tests/' . $name;
mkdir($root . '/imports/test',0700,true);
try {
    foreach (glob(dirname(__DIR__) . '/database/migrations/*.sql') as $file) {
        foreach (preg_split('/;\s*(?:\r?\n|$)/',file_get_contents($file)) as $sql) if (trim($sql)!=='') $db->exec($sql);
    }
    $db->prepare("INSERT INTO organizations (business_name,vat_number) VALUES ('VAT TEST','02572350185')")->execute();
    $organizationId=(int)$db->lastInsertId();
    $db->prepare("INSERT INTO users (organization_id,name,email,password_hash,role) VALUES (?,'Tester','vat-test@example.invalid','test','OWNER')")->execute([$organizationId]);
    $userId=(int)$db->lastInsertId();
    $stored=$root.'/imports/test/original.csv'; copy($source,$stored);
    $db->prepare("INSERT INTO import_batches (organization_id,uuid,source_system,import_type,status,original_filename,checksum_sha256,file_size,created_by) VALUES (?,'test','DATEV_KOINOS','vat_movements','STAGING',?,?,?,?)")
        ->execute([$organizationId,basename($source),hash_file('sha256',$source),filesize($source),$userId]);
    $batchId=(int)$db->lastInsertId();
    $service=new ImportService($db,$organizationId,$userId,$root);
    $stage=new ReflectionMethod(ImportService::class,'stageFile');
    $refresh=new ReflectionMethod(ImportService::class,'refreshCounters');
    $stage->invoke($service,$batchId,$stored,basename($source),'vat_movements',52428800,0);
    $refresh->invoke($service,$batchId,'READY');
    // Reproduce a row leaked by the old importer before the DATEV metadata update failed.
    $first=json_decode((string)$db->query("SELECT normalized_data_json FROM import_rows WHERE batch_id=$batchId ORDER BY id LIMIT 1")->fetchColumn(),true,512,JSON_THROW_ON_ERROR);
    (new VatService($db,$organizationId,$userId))->saveImported($first);
    $result=$service->commit($batchId);
    echo json_encode($result,JSON_UNESCAPED_UNICODE).PHP_EOL;
    $errors=$db->query("SELECT error_message,COUNT(*) total FROM import_rows WHERE batch_id=$batchId AND status='ERROR' GROUP BY error_message ORDER BY total DESC")->fetchAll();
    echo json_encode($errors,JSON_PRETTY_PRINT|JSON_UNESCAPED_UNICODE).PHP_EOL;
    $count=(int)$db->query("SELECT COUNT(*) FROM vat_movements WHERE organization_id=$organizationId")->fetchColumn();
    if ($result['errors']!==0 || $result['imported']!==$count || $count===0) throw new RuntimeException('VAT CSV import did not complete cleanly.');
    echo "PASS: $count DATEV VAT movements imported.\n";
} finally {
    $db->exec("DROP DATABASE `$name`");
}
