package com.mcp.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "agents")
public class AgentsRepositoryProperties {

    private String repoPath = "";

    @NotBlank
    private String agentsPath = "github/agents";

    @NotBlank
    private String skillsPath = "github/skills";

    @NotBlank
    private String instructionsPath = "github/instructions";

    private List<String> allowedExtensions = List.of(".md", ".json", ".yml", ".yaml", ".txt");

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getAgentsPath() {
        return agentsPath;
    }

    public void setAgentsPath(String agentsPath) {
        this.agentsPath = agentsPath;
    }

    public String getSkillsPath() {
        return skillsPath;
    }

    public void setSkillsPath(String skillsPath) {
        this.skillsPath = skillsPath;
    }

    public String getInstructionsPath() {
        return instructionsPath;
    }

    public void setInstructionsPath(String instructionsPath) {
        this.instructionsPath = instructionsPath;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
