package com.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.model.RepositoryFile;
import com.mcp.tools.RepositoryFileReader;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

@Configuration
public class AgentsMcpResourcesConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentsMcpResourcesConfig.class);

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> repoResources(
            RepositoryFileReader repositoryFileReader,
            ObjectMapper objectMapper
    ) {
        return List.of(
                resource("repo://agents", "Agents repository", "JSON index of agent files", "agent", repositoryFileReader, objectMapper),
                resource("repo://skills", "Skills repository", "JSON index of skill files", "skill", repositoryFileReader, objectMapper),
                resource("repo://instructions", "Instructions repository", "JSON index of instruction files", "instruction", repositoryFileReader, objectMapper)
        );
    }

    private McpServerFeatures.SyncResourceSpecification resource(
            String uri,
            String name,
            String description,
            String type,
            RepositoryFileReader repositoryFileReader,
            ObjectMapper objectMapper
    ) {
        McpSchema.Resource resource = new McpSchema.Resource(
                uri,
                name,
                description,
                "application/json",
                new McpSchema.Annotations(List.of(), null)
        );

        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) -> {
            try {
                log.info("MCP resource accessed: {}", uri);
                List<RepositoryFile> files = repositoryFileReader.list(type);
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(files);
                return new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(uri, "application/json", json)
                ));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read resources for " + type, e);
            }
        });
    }
}
