package com.agent.baki.controller;

import com.agent.baki.dto.ApplicationDTO;
import com.agent.baki.dto.FixDTO;
import com.agent.baki.dto.FixUpdateRequest;
import com.agent.baki.entity.Application;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.IssueStatus;
import com.agent.baki.exception.ResourceNotFoundException;
import com.agent.baki.mapper.ApplicationMapper;
import com.agent.baki.repository.ApplicationRepository;
import com.agent.baki.repository.FixRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Application and Fix Management
 * 
 * Provides RESTful API endpoints for managing applications and fixes
 * Returns ResponseEntity with proper HTTP status codes
 * 
 * @author Team Baki
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class ApplicationRestController {
    
    private final ApplicationRepository applicationRepository;
    private final FixRepository fixRepository;
    private final ApplicationMapper mapper;
    
    /**
     * Get all applications with summary statistics
     * 
     * @return List of applications with fix counts
     */
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationDTO>> getAllApplications() {
        log.info("REST API: Fetching all applications");
        
        List<Application> applications = applicationRepository.findAll();
        List<ApplicationDTO> dtos = mapper.toDTOList(applications);
        
        log.info("REST API: Returning {} applications", dtos.size());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get application by ID with all fixes
     * 
     * @param id Application ID
     * @return Application with full fix details
     */
    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationDTO> getApplicationById(@PathVariable Long id) {
        log.info("REST API: Fetching application with ID: {}", id);
        
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Application", id));
        
        ApplicationDTO dto = mapper.toDTOWithFixes(application);
        
        log.info("REST API: Returning application: {}", dto.getApplicationName());
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Get fix by ID
     * 
     * @param id Fix ID
     * @return Fix details
     */
    @GetMapping("/fixes/{id}")
    public ResponseEntity<FixDTO> getFixById(@PathVariable Long id) {
        log.info("REST API: Fetching fix with ID: {}", id);
        
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix", id));
        
        FixDTO dto = mapper.toFixDTO(fix);
        
        log.info("REST API: Returning fix: {}", dto.getIssueSummary());
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Mark fix as IGNORED
     * 
     * @param id Fix ID
     * @param request Update request with reason
     * @return Updated fix
     */
    @PutMapping("/fixes/{id}/ignore")
    public ResponseEntity<FixDTO> markFixAsIgnored(
            @PathVariable Long id,
            @Valid @RequestBody FixUpdateRequest request) {
        
        log.info("REST API: Marking fix {} as IGNORED", id);
        
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix", id));
        
        if (fix.getIssueStatus() == IssueStatus.RESOLVED) {
            throw new IllegalStateException("Cannot update resolved fix");
        }
        
        fix.setIssueStatus(IssueStatus.IGNORED);
        fix.setReason(request.getReason());
        fix = fixRepository.save(fix);
        
        log.info("REST API: Fix {} marked as IGNORED", id);
        return ResponseEntity.ok(mapper.toFixDTO(fix));
    }
    
    /**
     * Mark fix as IN_PROGRESS
     * 
     * @param id Fix ID
     * @param request Update request with GitHub PR
     * @return Updated fix
     */
    @PutMapping("/fixes/{id}/in-progress")
    public ResponseEntity<FixDTO> markFixAsInProgress(
            @PathVariable Long id,
            @Valid @RequestBody FixUpdateRequest request) {
        
        log.info("REST API: Marking fix {} as IN_PROGRESS", id);
        
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix", id));
        
        if (fix.getIssueStatus() == IssueStatus.RESOLVED) {
            throw new IllegalStateException("Cannot update resolved fix");
        }
        
        fix.setIssueStatus(IssueStatus.IN_PROGRESS);
        fix.setGithubPr(request.getGithubPr());
        fix = fixRepository.save(fix);
        
        log.info("REST API: Fix {} marked as IN_PROGRESS", id);
        return ResponseEntity.ok(mapper.toFixDTO(fix));
    }
    
    /**
     * Mark fix as DB_FIX
     * 
     * @param id Fix ID
     * @param request Update request with change number and optional reason
     * @return Updated fix
     */
    @PutMapping("/fixes/{id}/db-fix")
    public ResponseEntity<FixDTO> markFixAsDBFix(
            @PathVariable Long id,
            @Valid @RequestBody FixUpdateRequest request) {
        
        log.info("REST API: Marking fix {} as DB_FIX", id);
        
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix", id));
        
        if (fix.getIssueStatus() == IssueStatus.RESOLVED) {
            throw new IllegalStateException("Cannot update resolved fix");
        }
        
        fix.setIssueStatus(IssueStatus.DB_FIX);
        fix.setChangeNumber(request.getChangeNumber());
        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            fix.setReason(request.getReason());
        }
        fix = fixRepository.save(fix);
        
        log.info("REST API: Fix {} marked as DB_FIX", id);
        return ResponseEntity.ok(mapper.toFixDTO(fix));
    }
    
    /**
     * Mark fix as RESOLVED
     * 
     * @param id Fix ID
     * @return Updated fix
     */
    @PutMapping("/fixes/{id}/resolved")
    public ResponseEntity<FixDTO> markFixAsResolved(@PathVariable Long id) {
        log.info("REST API: Marking fix {} as RESOLVED", id);
        
        Fix fix = fixRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fix", id));
        
        fix.setIssueStatus(IssueStatus.RESOLVED);
        fix = fixRepository.save(fix);
        
        log.info("REST API: Fix {} marked as RESOLVED", id);
        return ResponseEntity.ok(mapper.toFixDTO(fix));
    }
    
    /**
     * Health check endpoint
     * 
     * @return API status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Baki Application Management API");
        return ResponseEntity.ok(response);
    }
}

// Made with Bob
