package com.agent.baki.repository;

import com.agent.baki.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Application entity
 * 
 * Provides data access operations for Application table
 * Extends JpaRepository for basic CRUD operations
 * 
 * Custom Query Methods:
 * - findByApplicationName: Find application by unique name
 * - existsByApplicationName: Check if application exists by name
 * 
 * @author Team Baki
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    
    /**
     * Find an application by its unique name
     * 
     * Used when:
     * - Parsing incidents to find or create application
     * - Validating application existence
     * 
     * @param applicationName The unique application name
     * @return Optional containing the application if found
     */
    Optional<Application> findByApplicationName(String applicationName);
    
    /**
     * Check if an application exists by name
     * 
     * More efficient than findByApplicationName when only checking existence
     * 
     * @param applicationName The application name to check
     * @return true if application exists, false otherwise
     */
    boolean existsByApplicationName(String applicationName);
}

// Made with Bob
