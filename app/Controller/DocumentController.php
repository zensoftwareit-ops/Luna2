<?php

declare(strict_types=1);

namespace Luna\Controller;

use Dompdf\Dompdf;
use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\AccountingService;
use Luna\Service\FatturaPaService;
use Throwable;

final class DocumentController extends BaseController
{
    private const TYPES = [
        'quotes' => ['code' => 'QUOTE', 'title' => 'Preventivi', 'singular' => 'Preventivo', 'prefix' => 'PREV', 'counterparty' => 'customer'],
        'orders' => ['code' => 'SALES_ORDER', 'title' => 'Ordini clienti', 'singular' => 'Ordine', 'prefix' => 'ORD', 'counterparty' => 'customer'],
        'ddt' => ['code' => 'DDT', 'title' => 'Documenti di trasporto', 'singular' => 'DDT', 'prefix' => 'DDT', 'counterparty' => 'customer'],
        'invoices' => ['code' => 'SALES_INVOICE', 'title' => 'Fatture attive', 'singular' => 'Fattura', 'prefix' => 'FT', 'counterparty' => 'customer'],
        'credit-notes' => ['code' => 'CREDIT_NOTE', 'title' => 'Note di credito', 'singular' => 'Nota di credito', 'prefix' => 'NC', 'counterparty' => 'customer'],
        'proformas' => ['code' => 'PROFORMA', 'title' => 'Proforma', 'singular' => 'Proforma', 'prefix' => 'PF', 'counterparty' => 'customer'],
        'purchase-orders' => ['code' => 'PURCHASE_ORDER', 'title' => 'Ordini fornitori', 'singular' => 'Ordine fornitore', 'prefix' => 'ORDF', 'counterparty' => 'supplier'],
        'purchase-invoices' => ['code' => 'PURCHASE_INVOICE', 'title' => 'Fatture passive', 'singular' => 'Fattura passiva', 'prefix' => 'FP', 'counterparty' => 'supplier'],
    ];

    public function index(string $type): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'VIEWER']);
        $definition = $this->type($type);
        $search = trim((string) ($_GET['q'] ?? ''));
        $sql = 'SELECT id, number, document_date, due_date, counterparty_name, subject, taxable_total, vat_total, total, balance_due, status
                FROM documents WHERE organization_id = :organization AND document_type = :type';
        $params = ['organization' => Auth::organizationId(), 'type' => $definition['code']];
        if ($search !== '') {
            $sql .= ' AND (number LIKE :search OR counterparty_name LIKE :search OR subject LIKE :search)';
            $params['search'] = '%' . $search . '%';
        }
        $sql .= ' ORDER BY document_date DESC, id DESC LIMIT 500';
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        $documents = $statement->fetchAll();

        $this->view->render('documents/index', compact('type', 'definition', 'documents', 'search') + [5Ó~yöÚ$z{-®éÜj×ÙXÝ[ÛˆÛ\ÜÏH˜Ø\™X›K]Ü˜\X›OXYˆÏÝ‘\ØÜš^š[Û™OÝ”K0èÝ”™^ž›ÏÝ”ØÛÛÏÝ’[\ÛšXš[OÝ’UOÝ•Ý[OÝÝÝXY›ÙOÜ›Ü™XXÚ
	[™\È\È	[™JNˆÏÏH
[
H	[™VÉÛ[™WÛ[X™\‰×HÏÝÏHšY]ÎŽ™J	[™VÉÙ\ØÜš\[Û‰×JHÏÝÏHšY]ÎŽ™J	[™VÉÜ]X[]I×Hˆ	È	Èˆ	[™VÉÝ[š]	×JHÏÝÏHšY]ÎŽ›[Û™^J	[™VÉÝ[š]ÜšXÙI×JHÏÝÏHšY]ÎŽ™J	[™VÉÙ\ØÛÝ[Ü\˜Ù[	×JHÏ‰OÝÏHšY]ÎŽ›[Û™^J	[™VÉÝ^X›WØ[[Ý[	×JHÏÝÏHšY]ÎŽ™J	[™VÉÝ˜]Ü˜]I×JHÏ‰OÝÏHšY]ÎŽ›[Û™^J	[™VÉÝÝ[Ø[[Ý[	×JHÏÝÝÜ[™›Ü™XXÚÈÏÝ›ÙO›ÛÝÛÛÜ[HHÝ’[\ÛšXš[OÝÛÛÜ[HŒˆÏHšY]ÎŽ›[Û™^J	ØÝ[Y[ÉÝ^X›WÝÝ[	×JHÏÝÝÛÛÜ[HHÝ’UOÝÛÛÜ[HŒˆÏHšY]ÎŽ›[Û™^J	ØÝ[Y[ÉÝ˜]ÝÝ[	×JHÏÝÝÛÛÜ[HHÝ•Ý[OÝÛÛÜ[HŒˆÝ›Û™ÏÏHšY]ÎŽ›[Û™^J	ØÝ[Y[ÉÝÝ[	×JHÏÜÝ›Û™ÏÝÝÝ›ÛÝÝX›OÜÙXÝ[Û‚ÙXÝ[ÛˆÛ\ÜÏH˜Ø\™›Ü›KXØ\™YÙÚ[Ü›˜HÝ]ÏÚ›Ü›HY]ÙHœÜÝˆXÝ[ÛH‹ÙØÝ[Y[ËÏÏHšY]ÎŽ™J	\JHÏ‹ÏÏH
[
H	ØÝ[Y[ÉÚY	×HÏ‹ÜÝ]\ÈˆÛ\ÜÏHš[›[™KY›Ü›H[œ]\OHšY[ˆˆ˜[YOH—ÝÚÙ[ˆˆ˜[YOHÏHšY]ÎŽ™JÜÜ™ŽŽÚÙ[Š
JHÏˆÙ[XÝ˜[YOHœÝ]\ÈÜ[Û‘Q•ÛÜ[ÛÜ[Û”ÑS•ÛÜ[ÛÜ[ÛPÐÑTQÛÜ[ÛÜ[Û”‘R‘PÕQÛÜ[ÛÜ[ÛÓÓ‘’T“QQÛÜ[ÛÜ[Û’S—Ô“ÑÔ‘TÔÏÛÜ[ÛÜ[Û‘•S’SQÛÜ[ÛÜ[Û’TÔÕQQÛÜ[ÛÜ[Û”‘PÑRU‘QÛÜ[ÛÜ[Û”T•PSWÔRQÛÜ[ÛÜ[Û”RQÛÜ[ÛÜ[Û“Õ‘T‘QOÛÜ[ÛÜ[ÛÐSÑSQÛÜ[ÛÜÙ[XÝ]ÛˆÛ\ÜÏH˜]Ûˆš[X\žHYÙÚ[Ü›˜OØ]ÛÙ›Ü›OÛ\ÜÏHš[“8 &Y[Z\ÜÚ[Û™HH[˜H˜]\˜H]]˜HÈHšXÙ^š[Û™HH[˜H\ÜÚ]˜HÙ[™\˜H]]ÛX]XØ[Y[HHØÜš]\˜HÛÛXš[KÙH[X[›ÈZHÛÛHHÚ\Ý[XH0êÛÛ™šYÝ\˜]ËÜÜÙXÝ[Û‚