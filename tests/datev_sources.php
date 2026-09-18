<?php
declare(strict_types=1);
// Opt-in integration test. Source attachments remain outside the repository.
// Uses only an isolated, randomly named local database; never reads .env.
require dirname(__DIR__).'/vendor/autoload.php';
use Luna\Service\ImportService;
use Luna\Service\DatevKoinosImport;

$source=$argv[1]??'';
if (!is_dir($source)) throw new RuntimeException('Pass a directory containing the original Koinos exports.');
$files=array_merge(glob($source.'/*150926*.csv'),array_map(fn($n)=>$source.'/'.$n,[
 'prima nota 2025.xls','prima nota al 310726.xls','Cespiti progressivi.xls',
 'Registri IVA 2025.pdf','Registri IVA al 310726.pdf',
 '20260915_ExportFattureRicevute.zip','20260915_ExportFattureInviate.zip']));
if (count($files)!==16 || count(array_filter($files,'is_file'))!==16) throw new RuntimeException('Expected the 16 source exports.');
$db=new PDO('mysql:host=127.0.0.1;port=33317;charset=utf8mb4','root','',[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,PDO::ATTR_EMULATE_PREPARES=>false]);
$name='luna_datev_test_'.bin2hex(random_bytes(5));
$db->exec("CREATE DATABASE `$name` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); $db->exec("USE `$name`");
$checks=0; $assert=static function(bool $ok,string $message)use(&$checks){if(!$ok)throw new RuntimeException($message);$checks++;};
$add=static function(string $table,array $data)use($db):int{$q=$db->prepare('INSERT INTO '.$table.' (`'.implode('`,`',array_keys($data)).'`) VALUES ('.implode(',',array_fill(0,count($data),'?')).')');$q->execute(array_values($data));return (int)$db->lastInsertId();};
$root=dirname(__DIR__).'/storage/private/datev-tests/'.$name; mkdir($root.'/imports',0700,true);
try {
 foreach(glob(dirname(__DIR__).'/database/migrations/*.sql') as $file)foreach(preg_split('/;\s*(?:\r?\n|$)/',file_get_contents($file)) as $sql)if(trim($sql)!=='')$db->exec($sql);
 $zip=new ZipArchive();$zip->open($source.'/20260915_ExportFattureInviate.zip');$vat='';
 for($i=0;$i<$zip->numFiles;$i++)if(str_ends_with(strtolower($zip->getNameIndex($i)),'.xml')){$xml=simplexml_load_string($zip->getFromIndex($i));$nodes=$xml->xpath('//*[local-name()="CedentePrestatore"]//*[local-name()="IdFiscaleIVA"]/*[local-name()="IdCodice"]');$vat=(string)$nodes[0];break;}$zip->close();
 $assert($vat!=='','Source company identified');
 $org=$add('organizations',['business_name'=>'TEST KOINOS','vat_number'=>$vat]);
 $user=$add('users',['organization_id'=>$org,'name'=>'Tester','email'=>'datev-test@example.invalid','password_hash'=>'test-only','role'=>'OWNER']);
 $stage=new ReflectionMethod(ImportService::class,'stageFile');$refresh=new ReflectionMethod(ImportService::class,'refreshCounters');
 $counts=[];$batchIds=[];
 foreach($files as $file){
  $uuid=bin2hex(random_bytes(16));$dir=$root.'/imports/'.$uuid;mkdir($dir,0700,true);$path=$dir.'/original.'.pathinfo($file,PATHINFO_EXTENSION);copy($file,$path);
  $batch=$add('import_batches',['organization_id'=>$org,'uuid'=>$uuid,'source_system'=>'DATEV_KOINOS','import_type'=>'datev_koinos','status'=>'STAGING','original_filename'=>basename($file),'checksum_sha256'=>hash_file('sha256',$file),'file_size'=>filesize($file),'created_by'=>$user]);
  $service=new ImportService($db,$org,$user,$root);
  $stage->invoke($service,$batch,$path,basename($file),'datev_koinos',52428800,0);$refresh->invoke($service,$batch,'READY');
  $result=$service->commit($batch);$batchIds[]=$batch;
  echo basename($file).' '.json_encode($result).PHP_EOL;
  if($result['errors']){ $q=$db->query("SELECT error_message,COUNT(*) n FROM import_rows WHERE batch_id=$batch AND status='ERROR' GROUP BY error_message LIMIT 10");echo json_encode($q->fetchAll()).PHP_EOL; }
  $assert($result['errors']===0,'No import errors: '.basename($file));
  $assert(hash_file('sha256',$file)===hash_file('sha256',$path),'Original bytes preserved');
  try{$service->commit($batch);$assert(false,'Second commit must be rejected');}catch(InvalidArgumentException){$assert(true,'Second commit rejected');}
  $assert($db->query("SELECT status FROM import_batches WHERE id=$batch")->fetchColumn()==='COMPLETED','Completed batch remains completed');
 }
 foreach(['journal_entries','vat_movements','accounting_open_items','fixed_assets'] as $table)$assert((int)$db->query("SELECT COUNT(*) FROM $table WHERE organization_id=$org")->fetchColumn()===0,'No invented operational data: '.$table);
 $assert((int)$db->query("SELECT COUNT(*) FROM documents WHERE organization_id=$org AND status='HISTORICAL'")->fetchColumn()===58,'All XML invoices are visible as historical documents');
 $assert((int)$db->query("SELECT COUNT(*) FROM documents WHERE organization_id=$org AND balance_due<>0")->fetchColumn()===0,'Historical invoices do not create receivable or payable balances');
 $summary=$db->query('SELECT record_kind,application_status,COUNT(*) n FROM datev_reference_records GROUP BY record_kind,application_status')->fetchAll();echo json_encode($summary).PHP_EOL;
 $assert((int)$db->query("SELECT COUNT(*) FROM datev_reference_records WHERE record_kind='journal_headers'")->fetchColumn()===8332,'All journal headers');
 $assert((int)$db->query("SELECT COUNT(*) FROM datev_reference_records WHERE record_kind='asset_progressives'")->fetchColumn()===787,'All asset snapshots');
 $assert((int)$db->query("SELECT COUNT(*) FROM datev_reference_records WHERE record_kind='invoice_history'")->fetchColumn()===58,'All invoice XMLs');
 $db->exec("DELETE FROM documents WHERE organization_id=$org AND status='HISTORICAL'");
 $materialized=(new DatevKoinosImport($db,$org,$user))->materializeHistoricalInvoices();
 $assert($materialized['created']===58&&$materialized['errors']===0,'Legacy archived XML invoices can be materialized');
 $assert((int)$db->query("SELECT COUNT(*) FROM documents WHERE organization_id=$org AND status='HISTORICAL'")->fetchColumn()===58,'Backfill restores every historical invoice');
 $batch=$batchIds[0];$db->exec("UPDATE import_batches SET status='READY' WHERE id=$batch");$db->exec("UPDATE import_rows SET status='STAGED' WHERE batch_id=$batch");
 $result=(new ImportService($db,$org,$user,$root))->commit($batch);$assert($result['imported']===0&&$result['skipped']>0&&$result['errors']===0,'Replay does not duplicate source or operational data');
 $assert((int)$db->query("SELECT COUNT(*) FROM documents WHERE organization_id=$org AND status='HISTORICAL'")->fetchColumn()===58,'Replay does not duplicate historical invoices');
 $db->exec("UPDATE import_batches SET status='ERROR' WHERE id=$batch");try{(new ImportService($db,$org,$user,$root))->commit($batch);$assert(false,'Partial staging rejected');}catch(InvalidArgumentException){$assert(true,'Partial staging rejected');}
 echo "PASS $checks checks; peak memory ".round(memory_get_peak_usage(true)/1048576)." MiB\n";
} finally {$db->exec("DROP DATABASE `$name`");}
