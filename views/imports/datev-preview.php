<?php use Luna\Core\Csrf; use Luna\Core\View; use Luna\Service\DatevKoinosReader;
$states=['APPLIED'=>'Applicato','REFERENCE'=>'Riferimento','NEEDS_DATA'=>'Da completare','CONFLICT'=>'Da riconciliare']; ?>
<div class="page-actions"><a class="button" href="/imports">← Importazioni</a><span class="badge"><?= View::e(View::label($batch['status'])) ?></span></div>
<section class="card form-card">
 <div class="card-header"><div><h2><?= View::e($batch['original_filename']) ?></h2><p>Azienda destinataria: <strong><?= View::e($destination['business_name']) ?></strong> · P. IVA <?= View::e($destination['vat_number']) ?></p></div></div>
 <div class="form-grid"><div class="full-width">
 <p class="help">L’acquisizione guidata applica conti, anagrafiche e dati cespiti completi senza sovrascrivere quelli esistenti. Causali e tabelle fiscali incomplete restano da verificare; PDF, stampe e fatture storiche vengono conservati come fonti probatorie e di riconciliazione. I movimenti contabili operativi devono essere importati separatamente con il tracciato “Prima nota / movimenti”, seguendo l’ordine indicato nella procedura DATEV.</p>
 <?php if ($batch['error_message']): ?><div class="alert alert-error"><?= View::e($batch['error_message']) ?></div><?php endif; ?>
 <?php if ($batch['status']==='READY'): ?>
 <form method="post" action="/imports/<?= (int)$batch['id'] ?>/commit" data-confirm="Acquisire nell’azienda indicata? I dati incompleti resteranno nell’archivio di riferimento.">
  <input type="hidden" name="_token" value="<?= View::e(Csrf::token()) ?>">
  <label class="field checkbox-field"><input type="checkbox" name="confirm_destination" value="1" required><span> Ho verificato l’azienda destinataria, eseguito un backup e letto i limiti dell’acquisizione.</span></label>
  <button class="button primary" type="submit">Conferma acquisizione</button>
 </form><?php endif; ?>
 <p class="muted">Righe analizzate: <?= (int)$batch['total_rows'] ?> · Acquisite: <?= (int)$batch['imported_rows'] ?> · Errori: <?= (int)$batch['error_rows'] ?>. Non è previsto l’annullamento massivo: eseguire prima il collaudo su una copia del database.</p>
 </div></div>
</section>
<?php if ($summary): ?><section class="card table-wrap"><table><thead><tr><th>Contenuto</th><th>Esito operativo</th><th>Righe</th></tr></thead><tbody>
<?php foreach ($summary as $item): ?><tr><td><?= View::e(DatevKoinosReader::LABELS[$item['record_kind']]??$item['record_kind']) ?></td><td><?= View::e($states[$item['application_status']]??$item['application_status']) ?></td><td><?= (int)$item['total'] ?></td></tr><?php endforeach; ?>
</tbody></table></section><?php endif; ?>
<section class="card"><details class="form-grid"><summary>File originali e allegati (<?= count($files) ?>)</summary><ul><?php foreach ($files as $file): ?><li><?php if ($file['stored_relative_path']): ?><a href="/imports/files/<?= (int)$file['id'] ?>/download"><?= View::e($file['filename']) ?></a><?php else: ?><?= View::e($file['filename']) ?><?php endif; ?> · <?= number_format((int)$file['file_size']/1024,1,',','.') ?> KB</li><?php endforeach; ?></ul></details></section>
<section class="card form-card"><form method="get" class="form-grid">
 <label class="field"><span>Contenuto</span><select name="kind"><option value="">Tutti</option><?php foreach (DatevKoinosReader::LABELS as $value=>$label): ?><option value="<?= View::e($value) ?>" <?= $kind===$value?'selected':'' ?>><?= View::e($label) ?></option><?php endforeach; ?></select></label>
 <label class="field"><span>Cerca nei dati</span><input name="q" value="<?= View::e($search) ?>" placeholder="Codice, denominazione, documento…"></label>
 <div class="full-width"><button class="button primary">Filtra</button><a class="button" href="/imports/<?= (int)$batch['id'] ?>">Azzera</a></div>
</form></section>
<section class="card table-wrap" data-no-table-tools><table><thead><tr><th>Origine</th><th>Contenuto</th><th>Dati</th><th>Esito / verifiche</th></tr></thead><tbody>
<?php foreach ($rows as $row): $record=$row['data']; ?><tr>
 <td><?= View::e($row['filename']) ?><br>Riga <?= (int)$row['source_row_number'] ?></td>
 <td><?= View::e(DatevKoinosReader::LABELS[$record['kind']??'']??'Dato') ?><br><?= View::e($record['key']??'') ?></td>
 <td class="import-preview-cell"><dl class="import-field-list"><?php foreach (($record['data']??[]) as $key=>$value): ?><div><dt><?= View::e(ucfirst(str_replace('_',' ',(string)$key))) ?></dt><dd><?php if (is_array($value)): ?><details><summary>Dettaglio</summary><pre class="json-preview"><?= View::e(json_encode($value,JSON_PRETTY_PRINT|JSON_UNESCAPED_UNICODE)) ?></pre></details><?php else: ?><?= View::e(is_bool($value)?($value?'Sì':'No'):($value??'—')) ?><?php endif; ?></dd></div><?php endforeach; ?></dl></td>
 <td><span class="badge"><?= View::e($row['status']==='IMPORTED'?'Acquisito':(['STAGED'=>'Da confermare','VALID'=>'Validato','SKIPPED'=>'Già acquisito','ERROR'=>'Errore'][$row['status']]??View::label($row['status']))) ?></span><p><?= View::e($row['error_message']?:($record['issue']??'Verificare i dati prima della conferma.')) ?></p></td>
</tr><?php endforeach; ?>
<?php if (!$rows): ?><tr><td colspan="4">Nessun risultato per i filtri selezionati.</td></tr><?php endif; ?>
</tbody></table></section>
<nav class="page-actions" aria-label="Pagine acquisizione"><span><?= (int)$total ?> risultati · pagina <?= (int)$page ?> di <?= (int)$pages ?></span><div>
<?php if ($page>1): ?><a class="button" href="?<?= View::e(http_build_query(['kind'=>$kind,'q'=>$search,'page'=>$page-1])) ?>">Precedente</a><?php endif; ?>
<?php if ($page<$pages): ?><a class="button" href="?<?= View::e(http_build_query(['kind'=>$kind,'q'=>$search,'page'=>$page+1])) ?>">Successiva</a><?php endif; ?>
</div></nav>
