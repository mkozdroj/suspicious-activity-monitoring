package com.grad.sam.controller;

import com.grad.sam.dto.request.CaseNoteDto;
import com.grad.sam.dto.request.OpenCaseRequestDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class InvestigationController {

    private final InvestigationService investigationService;
    private final InvestigationRepository investigationRepository;

    @PostMapping("/cases")
    public ResponseEntity<CaseResponseDto> openCase(@Valid @RequestBody OpenCaseRequestDto request) {

        Priority priority = request.getPriority() != null ? request.getPriority() : Priority.MEDIUM;
        Investigation inv = investigationService.openCase(
                request.getAlertId(),
                request.getAssignedOfficer(),
                priority
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(inv));
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<CaseResponseDto> getCase(@PathVariable Integer id) {

        Investigation inv = investigationRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(
                        "Investigation not found for id: " + id));
        return ResponseEntity.ok(toDto(inv));
    }

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
                + noteDto.getAuthor() + "] " + noteDto.getNoteText();
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

    private CaseResponseDto toDto(Investigation inv) {
        CaseResponseDto dto = new CaseResponseDto();
        dto.setCaseId(inv.getInvestigationId());
        dto.setStatus(inv.getState());
        dto.setAssignedOfficer(inv.getOpenedBy());
        dto.setOpenedAt(inv.getOpenedAt());
        dto.setClosedAt(inv.getClosedAt());
        dto.setOutcome(inv.getOutcome());
        dto.setFindings(inv.getFindings());

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
