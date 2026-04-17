package com.grad.sam.controller;

import com.grad.sam.dto.request.CaseNoteDto;
import com.grad.sam.dto.response.CaseResponseDto;
import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.Priority;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.model.Investigation;
import com.grad.sam.repository.InvestigationRepository;
import com.grad.sam.service.InvestigationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class InvestigationController {

    private final InvestigationService investigationService;
    private final InvestigationRepository investigationRepository;

    // POST /api/v1/cases
    @PostMapping("/cases")
    public ResponseEntity<CaseResponseDto> openCase(
            @RequestBody Map<String, Object> body) {

        Integer alertId = (Integer) body.get("alertId");
        String officer = (String) body.get("assignedOfficer");
        String priorityStr = (String) body.getOrDefault("priority", "MEDIUM");

        if (alertId == null)
            throw new IllegalArgumentException("alertId is required");
        if (officer == null || officer.isBlank())
            throw new IllegalArgumentException("assignedOfficer is required");

        Priority priority;
        try {
            priority = Priority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid priority. Valid values: LOW, MEDIUM, HIGH, URGENT");
        }

        Investigation inv = investigationService.openCase(alertId, officer, priority);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(inv));
    }

    // GET /api/v1/cases/{id}
    @GetMapping("/cases/{id}")
    public ResponseEntity<CaseResponseDto> getCase(@PathVariable Integer id) {

        Investigation inv = investigationRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(
                        "Investigation not found for id: " + id));
        return ResponseEntity.ok(toDto(inv));
    }

    // POST /api/v1/cases/{id}/notes
    @PostMapping("/cases/{id}/notes")
    public ResponseEntity<CaseResponseDto> addNote(
            @PathVariable Integer id,
            @Valid @RequestBody CaseNoteDto noteDto) {

        Investigation inv = investigationRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(
                        "Investigation not found for id: " + id));

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm"));
        String noteEntry = "[" + timestamp + " | "
                + noteDto.getAssignedOfficer() + "] " + noteDto.getAlertId();
        String existing = inv.getFindings() != null ? inv.getFindings() : "";
        String combined = existing.isBlank()
                ? noteEntry : existing + "\n" + noteEntry;

        if (combined.length() > 500) {
            combined = combined.substring(combined.length() - 497) + "...";
        }

        inv.setFindings(combined);
        investigationRepository.save(inv);
        return ResponseEntity.ok(toDto(inv));
    }

    // -----------------------------------------------------------------------
    // Mapping helper
    // -----------------------------------------------------------------------

    private CaseResponseDto toDto(Investigation inv) {
        CaseResponseDto dto = new CaseResponseDto();
        dto.setCaseId(inv.getInvestigationId());
        dto.setStatus(inv.getState());
        dto.setAssignedOfficer(inv.getOpenedBy());
        dto.setOpenedAt(inv.getOpenedAt());
        dto.setClosedAt(inv.getClosedAt());
        dto.setOutcome(inv.getOutcome());

        if (inv.getAlert() != null) {
            dto.setAlertId(inv.getAlert().getAlertId());
            Short score = inv.getAlert().getAlertScore();
            dto.setAlertSeverity(score != null && score >= 70
                    ? AlertSeverity.HIGH
                    : score != null && score >= 40
                    ? AlertSeverity.MEDIUM
                    : AlertSeverity.LOW);
        }
        return dto;
    }
}