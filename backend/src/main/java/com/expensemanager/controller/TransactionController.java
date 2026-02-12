package com.expensemanager.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.dto.TransactionDto;
import com.expensemanager.service.TransactionService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "CRUD operations and CSV import for transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List transactions with filtering and pagination")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String merchant,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<TransactionDto> page = transactionService.getTransactions(
                userId, category, startDate, endDate, merchant, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<ApiResponse<TransactionDto>> createTransaction(
            @Valid @RequestBody TransactionDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        TransactionDto created = transactionService.createTransaction(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing transaction")
    public ResponseEntity<ApiResponse<TransactionDto>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        TransactionDto updated = transactionService.updateTransaction(userId, id, dto);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted", null));
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import transactions from a CSV file")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> importCsv(
            @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TransactionDto> imported = transactionService.importCsv(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Imported " + imported.size() + " transactions", imported));
    }
}
