package com.navan.commerce.service.impl;

import com.navan.commerce.dao.InvoiceDao;
import com.navan.commerce.dao.TransactionDao;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.dto.CreateInvoiceRequest;
import com.navan.commerce.dto.InvoiceResponse;
import com.navan.commerce.dto.PageResponse;
import com.navan.commerce.entity.InvoiceEntity;
import com.navan.commerce.entity.InvoiceLineEntity;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.exception.ErrorCode;
import com.navan.commerce.exception.NotFoundException;
import com.navan.commerce.mapper.InvoiceMapper;
import com.navan.commerce.mapper.InvoiceSnapshotMapper;
import com.navan.commerce.service.InvoiceService;
import com.navan.commerce.service.line.LineCategoryRegistry;
import com.navan.commerce.util.IdGenerator;
import com.navan.commerce.util.MoneyUtils;
import com.navan.commerce.validation.invoice.InvoiceValidationChain;
import com.navan.commerce.validation.invoice.InvoiceValidationContext;
import com.navan.commerce.domain.LineCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Isolation: default READ_COMMITTED — snapshot reads within one transaction are consistent for
 * our purposes, and @Version on the invoice serializes concurrent PATCH/DELETE (D10).
 *
 * Entities are flat: header + snapshot lines via the DAO's key queries (one IN query per level).
 * Invoice composition rules (ids → existence → currency) live in the invoice validation chain.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceDao invoiceDao;
    private final TransactionDao transactionDao;
    private final InvoiceSnapshotMapper invoiceSnapshotMapper;
    private final InvoiceMapper invoiceMapper;
    private final LineCategoryRegistry lineCategoryRegistry;
    private final InvoiceValidationChain invoiceValidationChain;

    /**
     * One transaction: the validation chain (400/404/409 — any failure persists nothing) →
     * deep-copy snapshot → header + lines in a single commit.
     */
    @Override
    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        InvoiceValidationContext context = invoiceValidationChain.validate(request);

        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(IdGenerator.newInvoiceId())
                .currency(context.getCurrency())
                .build();
        List<InvoiceLineEntity> lines = invoiceSnapshotMapper.toSnapshotLines(
                invoice.getId(), context.getTransactions(),
                currentLinesByTransaction(context.getTransactionIds()));
        setTransactionsTotal(invoice, lines);

        invoiceDao.createWithSnapshot(invoice, lines);
        log.info("Created invoice id={} lines={} total={} {}",
                invoice.getId(), invoice.getLineCount(), invoice.getTotal(), invoice.getCurrency());
        return toInvoiceResponse(invoice, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse get(String id) {
        InvoiceEntity invoice = findActiveByIdOrThrow(id);
        return toInvoiceResponse(invoice, invoiceDao.findLines(id));
    }

    /**
     * Re-snapshot (spec §4): the DAO bulk-removes the old snapshot lines and inserts the rebuild
     * from CURRENT transactions — one commit; @Version on the header rejects concurrent losers;
     * any 404/409 from the chain leaves the existing snapshot untouched.
     */
    @Override
    @Transactional
    public InvoiceResponse patch(String id, CreateInvoiceRequest request) {
        InvoiceEntity invoice = findActiveByIdOrThrow(id);
        InvoiceValidationContext context = invoiceValidationChain.validate(request);

        invoice.setCurrency(context.getCurrency());
        List<InvoiceLineEntity> lines = invoiceSnapshotMapper.toSnapshotLines(
                id, context.getTransactions(), currentLinesByTransaction(context.getTransactionIds()));
        setTransactionsTotal(invoice, lines);

        invoiceDao.replaceSnapshot(id, lines);
        log.info("Re-snapshotted invoice id={} lines={} total={} {}",
                id, invoice.getLineCount(), invoice.getTotal(), invoice.getCurrency());
        return toInvoiceResponse(invoice, lines);
    }

    @Override
    @Transactional
    public void delete(String id) {
        invoiceDao.delete(findActiveByIdOrThrow(id)); // soft delete; transactions are never touched
        log.info("Soft-deleted invoice id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> list(int limit, int offset) {
        Page<InvoiceEntity> page = invoiceDao.findPage(limit, offset);
        List<String> invoiceIds = page.getContent().stream().map(InvoiceEntity::getId).toList();

        // One IN query for the whole page's lines — no N+1 (breakdowns ride inside each line row).
        Map<String, List<InvoiceLineEntity>> linesByInvoice = invoiceIds.isEmpty()
                ? Map.of()
                : invoiceDao.findLinesByInvoiceIds(invoiceIds).stream()
                        .collect(Collectors.groupingBy(InvoiceLineEntity::getInvoiceId));

        return PageResponse.<InvoiceResponse>builder()
                .items(page.getContent().stream()
                        .map(invoice -> toInvoiceResponse(
                                invoice, linesByInvoice.getOrDefault(invoice.getId(), List.of())))
                        .toList())
                .limit(limit)
                .offset(offset)
                .total(page.getTotalElements())
                .build();
    }

    // ---- helpers --------------------------------------------------------------

    /** Snapshot needs the source transactions' current money lines too — one IN query. */
    private Map<String, List<TransactionLineEntity>> currentLinesByTransaction(List<String> transactionIds) {
        return transactionDao.findLinesByTransactionIds(transactionIds).stream()
                .collect(Collectors.groupingBy(TransactionLineEntity::getTransactionId));
    }

    private void setTransactionsTotal(InvoiceEntity invoice, List<InvoiceLineEntity> lines) {
        invoice.setTotal(MoneyUtils.sum(
                lines.stream().map(InvoiceLineEntity::getTotal).toList()));
        invoice.setLineCount(lines.size());
    }

    private InvoiceEntity findActiveByIdOrThrow(String id) {
        return invoiceDao.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVOICE_NOT_FOUND, id));
    }

    /**
     * Assembles the response: each line's frozen breakdown arrives TYPED via the entity converter
     * (List<LineItem>) and is used for BOTH the line payloads (mapper splits by category) and the
     * rollups (D9) — computed from the snapshot by the registered per-category strategies,
     * never from live transactions.
     */
    private InvoiceResponse toInvoiceResponse(InvoiceEntity invoice, List<InvoiceLineEntity> lines) {
        List<LineItem> allItems = lines.stream()
                .flatMap(line -> line.getBreakdown().stream())
                .toList();
        return invoiceMapper.toInvoiceResponse(
                invoice,
                lines,
                lineCategoryRegistry.of(LineCategory.TAX).rollup(allItems),
                lineCategoryRegistry.of(LineCategory.FEE).rollup(allItems));
    }
}
