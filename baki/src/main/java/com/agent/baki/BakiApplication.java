package com.agent.baki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Class for Agent Baki - Incident Management System
 *
 * This is the entry point for the Spring Boot application.
 *
 * @SpringBootApplication - Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
 * @EnableScheduling - Enables Spring's scheduled task execution capability for batch jobs
 *
 * Features:
 * - Automated incident collection from Outlook and Jira
 * - AI-powered log analysis using Watsonx AI
 * - Code fix tracking and deduplication
 * - Batch processing for automated responses
 * - Web UI for developer interaction
 *
 * @author Team Baki
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
public class BakiApplication {

	/**
	 * Main method to start the Spring Boot application
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(BakiApplication.class, args);
	}

}
