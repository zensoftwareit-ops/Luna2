<?php
declare(strict_types=1);

require dirname(__DIR__).'/vendor/autoload.php';
use Luna\Service\ImportService;

$source=$argv[1]??'';
$jobs=[
    [$source.'/Stampa piano dei conti_150926095325.csv','datev_koinos'],
    [$source.'/Datev/Luna2_integrazione_piano_dei_conti.csv','chart_of_accounts'],
    [$source.'/Datev/Luna2_movimenti_contabili_2025.csv','journal_entries'],
    [$source.'/Datev/Luna2_movimenti_contabili_2026_al_3107.csv','journal_entries'],
];
if(count(array_filter(array_column($jobs,0),'is_file'))!==count($jobs))throw new RuntimeException('Sorgenti contabili di collaudo mancanti.');
$db=new PDO('mysql:host=127.0.0.1;port=33317;charset=utf8mb4','root','',[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,PDO::ATTR_EMULATE_PREPARES=>false]);
$name='luna_datev_journal_'.bin2hex(random_bytes(5));$db->exec("CREATE DATABASE `$name` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");$db->exec("USE `$name`");
$storage=dirname(__DIR__).'/storage/private/datev-tests/'.$name;$stage=new ReflectionMethod(ImportService::class,'stageFile');$refresh=new ReflectionMethod(ImportService::class,'refreshCounters');
try{
 foreach(glob(dirname(__DIR__).'/database/migrations/*.sql')as$file)foreach(preg_split('/;\s*(?:\r?\n|$)/',file_get_contents($file))as$sql)if(trim($sql)!=='')$db->exec($sql);
 $db->exec("INSERT INTO organizations (business_name,vat_number) VALUES ('TEST GIORNALE','02572350185')");$org=(int)$db->lastInsertId();$db->exec("INSERT INTO users (organization_id,name,email,password_hash,role) VALUES ($org,'Tester','datev-journal@example.invalid','test-only','OWNER')");$user=(int)$db->lastInsertId();
 foreach($jobs as[$file,$type]){$uuid=bin2hex(random_bytes(16));$dir=$storage.'/imports/'.$uuid;mkdir($dir,0700,true);$path=$dir.'/original.'.pathinfo($file,PATHINFO_EXTENSION);copy($file,$path);$q=$db->prepare("INSERT INTO import_batches (organization_id,uuid,source_system,import_type,status,original_filename,checksum_sha256,file_size,created_by) VALUES (?,?,'DATEV_KOINOS',?,'STAGING',?,?,?,?)");$q->execute([$org,$uuid,$type,basename($file),hash_file('sha256',$file),filesize($file),$user]);$batch=(int)$db->lastInsertId();$service=new ImportService($db,$org,$user,$storage);$stage->invoke($service,$batch,$path,basename($file),$type,52428800,0);$refresh->invoke($service,$batch,'READY');$result=$service->commit($batch);if($result['errors']){$errors=$db->query("SELECT error_message,COUNT(*) total FROM import_rows WHERE batch_id=$batch AND status='ERROR' GROUP BY error_message")->fetchAll();throw new RuntimeException(basename($file).' '.json_encode($result).' '.json_encode($errors));}}
 $entries=(int)$db->query("SELECT COUNT(*) FROM journal_entries WHERE organization_id=$org")->fetchColumn();$debit=(float)$db->query("SELECT SUM(total_debit) FROM journal_entries WHERE organization_id=$org")->fetchColumn();$credit=(float)$db->query("SELECT SUM(total_credit) FROM journal_entries WHERE organization_id=$org")->fetchColumn();
 if($entries!==8315||abs($debit-9040636.82)>.005||abs($credit-9040636.82)>.005)throw new RuntimeException("Quadratura inattesa: $entries, $debit, $credit");
 echo "PASS: 8.315 registrazioni 2025-2026, Dare/Avere 9.040.636,82.\n";
}finally{$db->exec("DROP DATABASE `$name`");}
