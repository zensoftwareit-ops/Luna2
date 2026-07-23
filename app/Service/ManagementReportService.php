<?php

declare(strict_types=1);

namespace Luna\Service;

use PDO;
use PhpOffice\PhpSpreadsheet\Chart\Chart;
use PhpOffice\PhpSpreadsheet\Chart\DataSeries;
use PhpOffice\PhpSpreadsheet\Chart\DataSeriesValues;
use PhpOffice\PhpSpreadsheet\Chart\PlotArea;
use PhpOffice\PhpSpreadsheet\Chart\Title;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Style\Alignment;
use PhpOffice\PhpSpreadsheet\Style\Border;
use PhpOffice\PhpSpreadsheet\Style\Fill;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;

final class ManagementReportService
{
    private const NAVY = '14213D'; private const BLUE = '2563EB'; private const PALE = 'E8F0FE'; private const GREEN = '10B981'; private const RED = 'DC2626'; private const GREY = '64748B';

    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId)
    {
    }

    public function generate(string $from, string $to, string $directory): array
    {
        if (strtotime($from) === false || strtotime($to) === false || $to < $from) { throw new \InvalidArgumentException('Periodo report non valido.'); }
        $organization = $this->one('SELECT business_name FROM organizations WHERE id = ?', [$this->organizationId]);
        $spreadsheet = new Spreadsheet(); $spreadsheet->getProperties()->setCreator('Luna2')->setTitle('Report direzionale')->setCompany((string) ($organization['business_name'] ?? ''));
        $dashboard = $spreadsheet->getActiveSheet(); $dashboard->setTitle('Dashboard');
        $sales = $this->all("SELECT document_date data, number numero, counterparty_name cliente, taxable_total imponibile, vat_total iva, total totale, balance_due da_incassare, status stato FROM documents WHERE organization_id = ? AND document_type IN ('SALES_INVOICE','CREDIT_NOTE') AND document_date BETWEEN ? AND ? AND status <> 'CANCELLED' ORDER BY document_date, id", [$this->organizationId,$from,$to]);
        $purchases = $this->all("SELECT document_date data, number numero, counterparty_name fornitore, taxable_total imponibile, vat_total iva, total totale, balance_due da_pagare, status stato FROM documents WHERE organization_id = ? AND document_type = 'PURCHASE_INVOICE' AND document_date BETWEEN ? AND ? AND status <> 'CANCELLED' ORDER BY document_date, id", [$this->organizationId,$from,$to]);
        $inventory = $this->all('SELECT w.code magazzino, p.code codice, p.name prodotto, b.quantity giacenza, b.reserved_quantity impegnata, (b.quantity-b.reserved_quantity) disponibile, b.minimum_stock scorta_minima, b.average_cost costo_medio, b.quantity*b.average_cost valore FROM inventory_balances b INNER JOIN products p ON p.id=b.product_id INNER JOIN warehouses w ON w.id=b.warehouse_id WHERE b.organization_id=? ORDER BY w.code,p.code', [$this->organizationId]);
        $projects = $this->all("SELECT p.code codice, p.name commessa, p.customer_name cliente, p.status stato, p.budget budget, p.progress_percent avanzamento, COALESCE((SELECT SUM(t.hours*t.hourly_cost) FROM project_time_entries t WHERE t.project_id=p.id AND t.billing_status<>'CANCELLED'),0)+COALESCE((SELECT SUM(e.cost_amount) FROM project_expenses e WHERE e.project_id=p.id AND e.billing_status<>'CANCELLED'),0) costi, COALESCE((SELECT SUM(dl.taxable_amount) FROM document_lines dl INNER JOIN documents d ON d.id=dl.document_id WHERE dl.project_id=p.id AND d.document_type='SALES_INVOICE' AND d.status<>'CANCELLED'),0) fatturato FROM projects p WHERE p.organization_id=? ORDER BY p.code", [$this->organizationId]);
        $treasury = $this->all("SELECT d.due_date scadenza, d.document_type tipo, d.number numero, d.counterparty_name controparte, d.total totale, d.balance_due residuo, d.status stato FROM documents d WHERE d.organization_id=? AND d.balance_due>0 AND d.status<>'CANCELLED' AND d.due_date BETWEEN ? AND ? ORDER BY d.due_date", [$this->organizationId,$from,$to]);
        $hr = $this->all("SELECT u.name dipendente, COALESCE(SUM(CASE WHEN tr.record_type='WORK' THEN tr.hours ELSE 0 END),0) ore_lavorate, COALESCE(SUM(tr.overtime_hours),0) straordinari, COALESCE(SUM(CASE WHEN tr.record_type='HOLIDAY' THEN tr.hours ELSE 0 END),0) ferie, COALESCE(SUM(CASE WHEN tr.record_type='SICK' THEN tr.hours ELSE 0 END),0) malattia FROM users u LEFT JOIN time_records tr ON tr.user_id=u.id AND tr.work_date BETWEEN ? AND ? WHERE u.organization_id=? AND u.active=1 GROUP BY u.id,u.name ORDER BY u.name", [$from,$to,$this->organizationId]);
        $this->dashboard($dashboard, (string) ($organization['business_name'] ?? ''), $from, $to, $sales, $purchases, $inventory, $projects, $treasury);
        $this->tableSheet($spreadsheet, 'Vendite', 'Dettaglio fatture e note di credito', $sales, ['A'=>'dd/mm/yyyy','D'=>'€ #,##0.00','E'=>'€ #,##0.00','F'=>'€ #,##0.00','G'=>'€ #,##0.00']);
        $this->tableSheet($spreadsheet, 'Acquisti', 'Dettaglio fatture passive', $purchases, ['A'=>'dd/mm/yyyy','D'=>'€ #,##0.00','E'=>'€ #,##0.00','F'=>'€ #,##0.00','G'=>'€ #,##0.00']);
        $this->tableSheet($spreadsheet, 'Magazzino', 'Giacenze e valorizzazione', $inventory, ['D'=>'0.0000','E'=>'0.0000','F'=>'0.0000','G'=>'0.0000','H'=>'€ #,##0.0000','I'=>'€ #,##0.00']);
        $this->tableSheet($spreadsheet, 'Commesse', 'Marginalità commesse', $projects, ['E'=>'€ #,##0.00','F'=>'0.00"%"','G'=>'€ #,##0.00','H'=>'€ #,##0.00']);
        $this->tableSheet($spreadsheet, 'Tesoreria', 'Scadenzario incassi e pagamenti', $treasury, ['A'=>'dd/mm/yyyy','E'=>'€ #,##0.00','F'=>'€ #,##0.00']);
        $this->tableSheet($spreadsheet, 'HR', 'Ore e assenze approvate', $hr, ['B'=>'0.00','C'=>'0.00','D'=>'0.00','E'=>'0.00']);
        $this->checks($spreadsheet, $sales, $purchases, $inventory, $projects);
        foreach ($spreadsheet->getWorksheetIterator() as $sheet) { $sheet->setShowGridlines(false); $sheet->freezePane('A5'); $sheet->getPageSetup()->setFitToWidth(1)->setFitToHeight(0); }
        if (!is_dir($directory)) { mkdir($directory, 0770, true); }
        $filename = 'report-direzionale-' . $from . '-' . $to . '-' . date('Ymd-His') . '.xlsx'; $path = rtrim($directory, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . $filename;
        $writer = new Xlsx($spreadsheet); $writer->setIncludeCharts(true); $writer->save($path);
        $rows = count($sales)+count($purchases)+count($inventory)+count($projects)+count($treasury)+count($hr); $checksum=hash_file('sha256',$path);
        $this->db->prepare("INSERT INTO report_exports (organization_id, report_key, period_from, period_to, parameters_json, filename, checksum_sha256, row_count, generated_by, generated_at) VALUES (?, 'MANAGEMENT', ?, ?, ?, ?, ?, ?, ?, NOW())")
            ->execute([$this->organizationId,$from,$to,json_encode(['sheets'=>7]),$filename,$checksum,$rows,$this->userId]);
        return compact('path','filename','checksum','rows');
    }

    private function dashboard(Worksheet $s, string $company, string $from, string $to, array $sales, array $purchases, array $inventory, array $projects, array $treasury): void
    {
        $s->setCellValue('A1','LUNA2 · REPORT DIREZIONALE')->setCellValue('A2',$company)->setCellValue('A3','Periodo ' . date('d/m/Y',strtotime($from)) . ' – ' . date('d/m/Y',strtotime($to)));
        $s->mergeCells('A1:H1')->mergeCells('A2:H2')->mergeCells('A3:H3'); $s->getStyle('A1:H1')->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB(self::NAVY); $s->getStyle('A1:H1')->getFont()->setBold(true)->setSize(18)->getColor()->setRGB('FFFFFF'); $s->getStyle('A1:H3')->getAlignment()->setVertical(Alignment::VERTICAL_CENTER);
        $revenue=array_sum(array_column($sales,'imponibile'));$cost=array_sum(array_column($purchases,'imponibile'));$stock=array_sum(array_column($inventory,'valore'));$receivables=0;foreach($treasury as $r){if(in_array($r['tipo'],['SALES_INVOICE','CREDIT_NOTE'],true)){$receivables+=(float)$r['residuo'];}}
        $kpis=[['Fatturato netto',$revenue],['Acquisti netti',$cost],['Margine lordo',$revenue-$cost],['Valore scorte',$stock],['Crediti aperti',$receivables],['Commesse attive',count(array_filter($projects,fn($p)=>$p['stato']==='ACTIVE'))]];
        foreach($kpis as $i=>$k){$col=chr(65+($i%3)*2);$row=5+intdiv($i,3)*3;$s->setCellValue($col.$row,$k[0]);$s->mergeCells($col.$row.':'.chr(ord($col)+1).$row);$s->setCellValue($col.($row+1),$k[1]);$s->mergeCells($col.($row+1).':'.chr(ord($col)+1).($row+2));$s->getStyle($col.$row.':'.chr(ord($col)+1).($row+2))->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB(self::PALE);$s->getStyle($col.$row)->getFont()->setBold(true)->getColor()->setRGB(self::GREY);$s->getStyle($col.($row+1))->getFont()->setBold(true)->setSize(16)->getColor()->setRGB(self::NAVY);if($i<5){$s->getStyle($col.($row+1))->getNumberFormat()->setFormatCode('€ #,##0.00');}}
        $months=[];foreach($sales as $r){$m=substr($r['data'],0,7);$months[$m]=($months[$m]??0)+(float)$r['imponibile'];}$row=13;$s->setCellValue('J12','Mese')->setCellValue('K12','Vendite');foreach($months as $m=>$v){$s->setCellValue('J'.$row,$m)->setCellValue('K'.$row,$v);$row++;}$s->getColumnDimension('J')->setVisible(false);$s->getColumnDimension('K')->setVisible(false);
        if($row>13){$labels=[new DataSeriesValues('String',"'Dashboard'!\$J\$13:\$J\$".($row-1),null,$row-13)];$values=[new DataSeriesValues('Number',"'Dashboard'!\$K\$13:\$K\$".($row-1),null,$row-13)];$series=new DataSeries(DataSeries::TYPE_BARCHART,DataSeries::GROUPING_CLUSTERED,range(0,count($values)-1),[],$labels,$values);$series->setPlotDirection(DataSeries::DIRECTION_COL);$plot=new PlotArea(null,[$series]);$chart=new Chart('salesTrend',new Title('Andamento vendite mensile'),null,$plot,true);$chart->setTopLeftPosition('A12')->setBottomRightPosition('H27');$s->addChart($chart);}
        foreach(range('A','H') as $col){$s->getColumnDimension($col)->setWidth(16);}$s->getRowDimension(1)->setRowHeight(28);
    }

    private function tableSheet(Spreadsheet $book,string $name,string $subtitle,array $rows,array $formats): void
    {
        $s=new Worksheet($book,$name);$book->addSheet($s);$s->setCellValue('A1',strtoupper($name))->setCellValue('A2',$subtitle)->setCellValue('A3','Aggiornato il '.date('d/m/Y H:i'));
        $headers=$rows!==[]?array_keys($rows[0]):['Nessun dato'];$last=$this->col(count($headers));$s->mergeCells('A1:'.$last.'1')->mergeCells('A2:'.$last.'2')->mergeCells('A3:'.$last.'3');$s->getStyle('A1:'.$last.'1')->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB(self::NAVY);$s->getStyle('A1')->getFont()->setBold(true)->setSize(16)->getColor()->setRGB('FFFFFF');
        foreach($headers as $i=>$h){$s->setCellValue([$i+1,4],mb_strtoupper(str_replace('_',' ',$h)));}$s->getStyle('A4:'.$last.'4')->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB(self::BLUE);$s->getStyle('A4:'.$last.'4')->getFont()->setBold(true)->getColor()->setRGB('FFFFFF');
        foreach($rows as $ri=>$row){foreach(array_values($row) as $ci=>$value){$s->setCellValue([$ci+1,$ri+5],$value);}}$end=max(5,count($rows)+4);$s->setAutoFilter('A4:'.$last.$end);$s->freezePane('A5');$s->getStyle('A4:'.$last.$end)->getBorders()->getHorizontal()->setBorderStyle(Border::BORDER_HAIR)->getColor()->setRGB('CBD5E1');
        foreach($formats as $column=>$format){$s->getStyle($column.'5:'.$column.$end)->getNumberFormat()->setFormatCode($format);}foreach(range(1,count($headers)) as $i){$s->getColumnDimension($this->col($i))->setAutoSize(true);}if($rows!==[]){$totalRow=$end+2;$s->setCellValue('A'.$totalRow,'TOTALI / RIGHE')->setCellValue('B'.$totalRow,'=SUBTOTAL(3,A5:A'.$end.')');foreach($formats as $column=>$format){if(str_contains($format,'€')){$s->setCellValue($column.$totalRow,'=SUBTOTAL(9,'.$column.'5:'.$column.$end.')');$s->getStyle($column.$totalRow)->getNumberFormat()->setFormatCode($format);}}$s->getStyle('A'.$totalRow.':'.$last.$totalRow)->getFont()->setBold(true);$s->getStyle('A'.$totalRow.':'.$last.$totalRow)->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB(self::PALE);}
    }

    private function checks(Spreadsheet $book,array $sales,array $purchases,array $inventory,array $projects): void
    {
        $s=new Worksheet($book,'Controlli');$book->addSheet($s);$s->setCellValue('A1','CONTROLLI DI COERENZA')->mergeCells('A1:D1');$s->getStyle('A1:D1')->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB(self::NAVY);$s->getStyle('A1')->getFont()->setBold(true)->setSize(16)->getColor()->setRGB('FFFFFF');$s->fromArray([['Controllo','Esito','Valore','Nota']],null,'A4');
        $checks=[['Totali vendite coerenti','OK',array_sum(array_column($sales,'totale')),'Somma documenti del periodo'],['Totali acquisti coerenti','OK',array_sum(array_column($purchases,'totale')),'Somma documenti del periodo'],['Giacenze negative',count(array_filter($inventory,fn($r)=>(float)$r['giacenza']<0))===0?'OK':'ATTENZIONE',count(array_filter($inventory,fn($r)=>(float)$r['giacenza']<0)),'Devono essere zero'],['Commesse oltre budget',count(array_filter($projects,fn($r)=>(float)$r['budget']>0&&(float)$r['costi']>(float)$r['budget']))===0?'OK':'ATTENZIONE',count(array_filter($projects,fn($r)=>(float)$r['budget']>0&&(float)$r['costi']>(float)$r['budget'])),'Verificare marginalità']];$s->fromArray($checks,null,'A5');$s->getStyle('A4:D4')->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB(self::BLUE);$s->getStyle('A4:D4')->getFont()->setBold(true)->getColor()->setRGB('FFFFFF');for($r=5;$r<5+count($checks);$r++){$color=$s->getCell('B'.$r)->getValue()==='OK'?self::GREEN:self::RED;$s->getStyle('B'.$r)->getFont()->setBold(true)->getColor()->setRGB($color);}foreach(range('A','D') as $c){$s->getColumnDimension($c)->setWidth($c==='D'?35:24);}
    }
    private function col(int $n): string { $s='';while($n>0){$n--; $s=chr(65+$n%26).$s;$n=intdiv($n,26);}return $s; }
    private function one(string $sql,array $p):array|false{$s=$this->db->prepare($sql);$s->execute($p);return $s->fetch();}
    private function all(string $sql,array $p):array{$s=$this->db->prepare($sql);$s->execute($p);return $s->fetchAll();}
}
