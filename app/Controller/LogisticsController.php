<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\InventoryService;

final class LogisticsController extends BaseController
{
    public function index(): never
    {
        $this->requireFeature('inventory'); $this->requireRoles(['OWNER','ADMIN','WAREHOUSE','ACCOUNTANT','VIEWER']); $org=Auth::organizationId();
        $balances=$this->rows('SELECT b.*,p.code product_code,p.name product_name,p.ean,p.sku,w.code warehouse_code,w.name warehouse_name,(b.quantity-b.reserved_quantity) available FROM inventory_balances b INNER JOIN products p ON p.id=b.product_id INNER JOIN warehouses w ON w.id=b.warehouse_id WHERE b.organization_id=? ORDER BY w.code,p.code',[$org]);
        $transfers=$this->rows('SELECT t.*,ws.code source_code,wd.code destination_code FROM inventory_transfers t INNER JOIN warehouses ws ON ws.id=t.source_warehouse_id INNER JOIN warehouses wd ON wd.id=t.destination_warehouse_id WHERE t.organization_id=? ORDER BY t.id DESC LIMIT 100',[$org]);
        $picks=$this->rows('SELECT pl.*,w.code warehouse_code,d.number document_number,COALESCE(SUM(pi.required_quantity),0) required_quantity,COALESCE(SUM(pi.picked_quantity),0) picked_quantity FROM inventory_pick_lists pl INNER JOIN warehouses w ON w.id=pl.warehouse_id INNER JOIN documents d ON d.id=pl.document_id LEFT JOIN inventory_pick_items pi ON pi.pick_list_id=pl.id WHERE pl.organization_id=? GROUP BY pl.id ORDER BY pl.id DESC LIMIT 100',[$org]);
        $warehouses=$this->rows('SELECT id,code,name FROM warehouses WHERE organization_id=? AND active=1 ORDER BY code',[$org]);
        $products=$this->rows("SELECT id,code,name,ean,sku FROM products WHERE organization_id=? AND active=1 AND track_inventory=1 ORDER BY code",[$org]);
        $orders=$this->rows("SELECT id,number,counterparty_name FROM documents WHERE organization_id=? AND document_type IN ('SALES_ORDER','DDT') AND fulfillment_status IN ('OPEN','PARTIAL') AND status<>'CANCELLED' ORDER BY document_date DESC",[$org]);
        $this->view->render('operations/logistics',compact('balances','transfers','picks','warehouses','products','orders')+['title'=>'Logistica e magazzino']);
    }
    public function movement(): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']); $s=new InventoryService($this->db,Auth::organizationId(),Auth::id()); $s->postMovement($_POST); $this->redirect('/operations/logistics','Movimento registrato atomicamente.'); }
    public function transfer(): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']); $lines=[];foreach((array)($_POST['lines']??[]) as $r){$lines[]=['product_id'=>$r['product_id']??0,'quantity'=>$r['quantity']??0];}$id=(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->createTransfer((int)$_POST['source_warehouse_id'],(int)$_POST['destination_warehouse_id'],$lines,$_POST['transfer_date']??null,$_POST['notes']??null);$this->audit('CREATE','inventory_transfers',$id);$this->redirect('/operations/logistics','Trasferimento creato in bozza.'); }
    public function confirm(string $id): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']);(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->confirmTransfer((int)$id);$this->redirect('/operations/logistics','Merce scaricata dall’origine e trasferimento in transito.'); }
    public function receive(string $id): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']);(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->receiveTransfer((int)$id,$_POST['quantities']??[]);$this->redirect('/operations/logistics','Trasferimento ricevuto e giacenze aggiornate.'); }
    public function cancelTransfer(string $id): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']);(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->cancelTransfer((int)$id);$this->redirect('/operations/logistics','Trasferimento in bozza annullato.'); }
    public function pick(): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE','SALES']);$id=(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->createPickList((int)$_POST['document_id'],(int)$_POST['warehouse_id'],(int)($_POST['assigned_to']??0)?:null);$this->redirect('/operations/logistics?pick='.$id,'Lista di picking creata e merce impegnata.'); }
    public function scan(string $id): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']);$r=(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->scanBarcode((int)$id,(string)($_POST['barcode']??''),(float)($_POST['quantity']??1));$this->redirect('/operations/logistics?pick='.$id,$r['message'],$r['result']==='ACCEPTED'?'success':'error'); }
    public function completePick(string $id): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']);(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->completePick((int)$id);$this->redirect('/operations/logistics','Picking completato e scarico registrato.'); }
    public function cancelPick(string $id): never { $this->requireRoles(['OWNER','ADMIN','WAREHOUSE']);(new InventoryService($this->db,Auth::organizationId(),Auth::id()))->cancelPick((int)$id);$this->redirect('/operations/logistics','Picking annullato e disponibilità ripristinata.'); }
    private function rows(string $sql,array $p):array{$s=$this->db->prepare($sql);$s->execute($p);return $s->fetchAll();}
}
