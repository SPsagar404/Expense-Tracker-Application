package com.expensemanager.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.service.GdprService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gdpr")
@RequiredArgsConstructor
@Tag(name = "GDPR", description = "Data export and deletion for GDPR compliance")
public class GdprController {

    private final GdprService gdprService;

    @GetMapping("/export")
    @Operation(summary = "Export all user data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportData() {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> data = gdprService.exportUserData(userId);
        return ResponseEntity.ok(ApiResponse.success("Data exported", data));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete all user data (irreversible)")
    public ResponseEntity<ApiResponse<Void>> deleteData() {
        Long userId = SecurityUtils.getCurrentUserId();
        gdprService.deleteUserData(userId);
        return ResponseEntity.ok(ApiResponse.success("All user data deleted", null));
    }
}
