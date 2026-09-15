<?php
use Luna\Core\View;
$editSchedule = [];
foreach (($registerYears ?? []) as $schedule) {
    if ((int) $schedule['fixed_asset_id'] === (int) ($_GET['register_asset'] ?? 0)) { $editSchedule = $schedule; }
}
$scheduleFields = [
    'original_cost' => 'Costo originario', 'revaluations' => 'Rivalutazioni cumulate a fine anno',
    'writedowns' => 'Svalutazioni cumulate a fine anno', 'civil_opening_fund' => 'Fondo iniziale civilistico',
    'tax_opening_fund' => 'Fondo iniziale fiscale', 'civil_rate' => 'Coefficiente effettivo civilistico %',
    'tax_rate' => 'Coefficiente effettivo fiscale %', 'civil_quota' => 'Quota annuale civilistica', 'tax_quota' => 'Quota annuale fiscale',
];
?>
<section class="card" id="asset-register-year">
    <div class="card-header"><div><h2>Schede annuali registro cespiti · <?= (int) $year ?></h2>
    <p>Confermare i valori dell’esercizio e il riferimento al prospetto verificato. Le schede alimentano la stampa del registro nel Centro professionale.</p></div>
    <a class="button" href="/professional#prints">Stampa registro</a></div>
    <form method="post" action="/accounting/compliance/asset-register-year" class="modern-form">
        <input type="hidden" name="_token" value="<?= $token ?>"><input type="hidden" name="year" value="<?= (int) $year ?>">
        <div class="form-grid">
            <label class="field"><span>Cespite</span><select name="fixed_asset_id" required>
                <option value="">Seleziona</option><?php foreach ($assets as $a): ?><option value="<?= (int) $a['id'] ?>" <?= (int) ($editSchedule['fixed_asset_id'] ?? 0) === (int) $a['id'] ? 'selected' : '' ?>><?= View::e($a['asset_code'] . ' - ' . $a['description']) ?></option><?php endforeach; ?>
            </select></label>
            <?php foreach ($scheduleFields as $key => $label): ?><label class="field"><span><?= View::e($label) ?></span>
                <input name="<?= View::e($key) ?>" inputmode="decimal" value="<?= View::e($editSchedule[$key] ?? '') ?>" required placeholder="0,00"></label><?php endforeach; ?>
            <label class="field"><span>Data eliminazione / cessione</span><input type="date" name="disposal_date" value="<?= View::e($editSchedule['disposal_date'] ?? '') ?>"></label>
            <label class="field"><span>Corrispettivo eliminazione / cessione</span><input name="disposal_proceeds" inputmode="decimal" value="<?= View::e($editSchedule['disposal_proceeds'] ?? '') ?>"></label>
            <label class="field"><span>Riferimento verifica / documento di origine</span><input name="evidence_reference" maxlength="500" value="<?= View::e($editSchedule['evidence_reference'] ?? '') ?>" required></label>
        </div><div class="form-actions"><button type="submit" class="button primary">Salva scheda annuale</button></div>
    </form>
    <div class="table-wrap"><table><thead><tr><th>Cespite</th><th>Fondo iniziale civile</th><th>Quota civile</th><th>Quota fiscale</th><th>Verifica</th><th></th></tr></thead><tbody>
    <?php foreach (($registerYears ?? []) as $schedule): ?><tr><td><?= View::e($schedule['asset_code']) ?></td><td><?= View::money($schedule['civil_opening_fund']) ?></td><td><?= View::money($schedule['civil_quota']) ?></td><td><?= View::money($schedule['tax_quota']) ?></td><td><?= View::e($schedule['evidence_reference']) ?></td><td><a class="table-action" href="?year=<?= (int) $year ?>&register_asset=<?= (int) $schedule['fixed_asset_id'] ?>#asset-register-year">Modifica</a></td></tr><?php endforeach; ?>
    <?php if (empty($registerYears)): ?><tr><td colspan="6">Nessuna scheda annuale verificata.</td></tr><?php endif; ?>
    </tbody></table></div>
</section>
