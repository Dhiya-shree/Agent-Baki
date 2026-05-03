package com.agent.baki.mapper;

import com.agent.baki.dto.ApplicationDTO;
import com.agent.baki.dto.FixDTO;
import com.agent.baki.entity.Application;
import com.agent.baki.entity.Fix;
import com.agent.baki.entity.IssueStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between entities and DTOs
 */
@Component
public class ApplicationMapper {
    
    /**
     * Convert Application entity to DTO with statistics
     */
    public ApplicationDTO toDTO(Application application) {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setApplicationId(application.getApplicationId());
        dto.setApplicationName(application.getApplicationName());
        dto.setRepositoryLink(application.getRepositoryLink());
        dto.setCreatedAt(application.getCreatedAt());
        dto.setUpdatedAt(application.getUpdatedAt());
        
        // Calculate statistics
        List<Fix> fixes = application.getFixes();
        dto.setTotalFixes((long) fixes.size());
        dto.setPendingFixes(fixes.stream().filter(f -> f.getIssueStatus() == IssueStatus.PENDING).count());
        dto.setInProgressFixes(fixes.stream().filter(f -> f.getIssueStatus() == IssueStatus.IN_PROGRESS).count());
        dto.setResolvedFixes(fixes.stream().filter(f -> f.getIssueStatus() == IssueStatus.RESOLVED).count());
        dto.setIgnoredFixes(fixes.stream().filter(f -> f.getIssueStatus() == IssueStatus.IGNORED).count());
        dto.setDbFixes(fixes.stream().filter(f -> f.getIssueStatus() == IssueStatus.DB_FIX).count());
        
        return dto;
    }
    
    /**
     * Convert Application entity to DTO with full fix details
     */
    public ApplicationDTO toDTOWithFixes(Application application) {
        ApplicationDTO dto = toDTO(application);
        dto.setFixes(application.getFixes().stream()
            .map(this::toFixDTO)
            .collect(Collectors.toList()));
        return dto;
    }
    
    /**
     * Convert Fix entity to DTO
     */
    public FixDTO toFixDTO(Fix fix) {
        FixDTO dto = new FixDTO();
        dto.setFixId(fix.getFixId());
        dto.setApplicationId(fix.getApplication().getApplicationId());
        dto.setApplicationName(fix.getApplication().getApplicationName());
        dto.setIssueSummary(fix.getIssueSummary());
        dto.setCodeClassName(fix.getCodeClassName());
        dto.setCodeLine(fix.getCodeLine());
        dto.setGithubPr(fix.getGithubPr());
        dto.setIssueStatus(fix.getIssueStatus());
        dto.setReason(fix.getReason());
        dto.setChangeNumber(fix.getChangeNumber());
        dto.setCreatedAt(fix.getCreatedAt());
        dto.setUpdatedAt(fix.getUpdatedAt());
        dto.setMailCount(fix.getMails().size());
        dto.setJiraCount(fix.getJiras().size());
        return dto;
    }
    
    /**
     * Convert list of Application entities to DTOs
     */
    public List<ApplicationDTO> toDTOList(List<Application> applications) {
        return applications.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}

// Made with Bob
