<?php
declare(strict_types=1);

require dirname(__DIR__).'/vendor/autoload.php';

use Luna\Service\ImportService;

$rootSource=$argv[1]??'';
$files=[
    $rootSource.'/Stampa elenco categorie cespiti_150926100420.csv',
    $rootSource.'/Datev/Anagrafica completa cespiti.xls',
    $rootSource.'/Cespiti progressivi.xls',
    $rootSource.'/Datev/Movimenti completi cespiti.xls',
];
if (count(array_filter($files,'is_file'))!==count($files)) throw new RuntimeException('Export cespiti DATEV incompleti.');

$db=new PDO('mysql:host=127.0.0.1;port=33317;charset=utf8mb4','root','',[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,PDO::ATTR_EMULATE_PREPARES=>false]);
$name='luna_datev_assets_'.bin2hex(random_bytes(5));
$db->exec("CREATE DATABASE `$name` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");$db->exec("USE `$name`");
$storage=dirname(__DIR__).'/storage/private/datev-tests/'.$name;$stage=new ReflectionMethod(ImportService::class,'stageFile');$refresh=new ReflectionMethod(ImportService::class,'refreshCounters');
try {
    foreach(glob(dirname(__DIR__).'/database/migrations/*.sql') as $file) foreach(preg_split('/;\s*(?:\r?\n|$)/',file_get_contents($file)) as $sql) if(trim($sql)!=='') $db->exec($sql);
    $db->exec("INSERT INTO organizations (business_name,vat_number) VALUES ('TEST CESPITI','02572350185')");$org=(int)$db->lastInsertId();
    $db->exec("INSERT INTO users (organization_id,name,email,password_hash,role) VALUES ($org,'Tester','datev-assets@example.invalid','test-only','OWNER')");$user=(int)$db->lastInsertId();
    foreach($files as $file){
        $uuid=bin2hex(random_bytes(16));$dir=$storage.'/imports/'.$uuid;mkdir($dir,0700,true);$path=$dir.'/original.'.pathinfo($file,PATHINFO_EXTENSION);copy($file,$path);
        $q=$db->prepare("INSERT INTO import_batches (organization_id,uuid,source_system,import_type,status,original_filename,checksum_sha256,file_size,created_by) VALUES (?,?,'DATEV_KOINOS','datev_koinos','STAGING',?,?,?,?)");
        $q->execute([$org,$uuid,basename($file),hash_file('sha256',$file),filesize($file),$user]);$batch=(int)$db->lastInsertId();
        $service=new ImportService($db,$org,$user,$storage);$stage->invoke($service,$batch,$path,basename($file),'datev_koinos',52428800,0);$refresh->invoke($service,$batch,'READY');
        $result=$service->commit($batch);if($result['errors']){$errors=$db->query("SELECT error_message,COUNT(*) total FROM import_rows WHERE batch_id=$batch AND status='ERROR' GROUP BY error_message")->fetchAll();throw new RuntimeException(basename($file).' contiene errori: '.json_encode($result).' '.json_encode($errors));}
    }
    $expected=['fixed_assets'=>131,'datev_fixed_asset_progressives'=>787,'datev_fixed_asset_movements'=>610];
    foreach($expected as $table=>$count){$actual=(int)$db->query("SELECT COUNT(*) FROM $table WHERE organization_id=$org")->fetchColumn();if($actual!==$count)throw new RuntimeException("$table: attesi $count, trovati $actual");}
    $unlinked=(int)$db->query("SELECT COUNT(*) FROM datev_reference_records WHERE organization_id=$org AND record_kind IN ('fixed_asset_master','asset_progressives','asset_movements') AND application_status<>'APPLIED'")->fetchColumn();
    if($unlinked!==0)throw new RuntimeException("Record cespiti non applicati: $unlinked");
    echo "PASS: 131 cespiti, 787 progressivi e 610 movimenti importati senza errori.\n";
} finally {$db->exec("DROP DATABASE `$name`");}
