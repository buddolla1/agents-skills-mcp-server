package com.mcp.tools;

import com.mcp.config.AgentsRepositoryProperties;
import com.mcp.model.InstructionExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InstructionExecutionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void applySkillToFilePackagesSkillAndTargetFileContext() throws IOException {
        RepositoryFileReader repositoryFileReader = new RepositoryFileReader(properties());
        InstructionExecutionService service = new InstructionExecutionService(repositoryFileReader);

        write(tempDir.resolve("github/skills/yaml-validator/SKILL.md"), """
                ---
                name: yaml-validator
                description: Example skill
                ---
                # Sample Skill
                """);
        write(tempDir.resolve("src/main/java/com/example/App.java"), """
                package com.example;

                class App {
                }
                """);

        InstructionExecutionResult result = service.applySkillToFile("yaml-validator", "src/main/java/com/example/App.java");

        assertThat(result.operation()).isEqualTo("applySkillToFile");
        assertThat(result.instructionType()).isEqualTo("skill");
        assertThat(result.instructionName()).isEqualTo("yaml-validator");
        assertThat(result.instructionDescription()).isEqualTo("Example skill");
        assertThat(result.targetType()).isEqualTo("workspace");
        assertThat(result.targetRelativePath()).isEqualTo("src/main/java/com/example/App.java");
        assertThat(result.targetContent()).contains("class App");
        assertThat(result.instructionContent()).contains("Sample Skill");
    }

    @Test
    void runAgentToolsIncludesDeclaredToolsFromAgentFrontmatter() throws IOException {
        RepositoryFileReader repositoryFileReader = new RepositoryFileReader(properties());
        InstructionExecutionService service = new InstructionExecutionService(repositoryFileReader);

        write(tempDir.resolve("github/agents/sample.agent.md"), """
                ---
                name: sample-agent
                description: Example agent
                tools: [codebase, file_operations]
                ---
                # Sample Agent
                """);
        write(tempDir.resolve("README.md"), "# Demo");

        InstructionExecutionResult result = service.runAgentTools("sample.agent.md", "README.md");

        assertThat(result.operation()).isEqualTo("runAgentTools");
        assertThat(result.instructionName()).isEqualTo("sample-agent");
        assertThat(result.declaredTools()).containsExactly("codebase", "file_operations");
        assertThat(result.targetRelativePath()).isEqualTo("README.md");
    }

    private AgentsRepositoryProperties properties() {
        AgentsRepositoryProperties properties = new AgentsRepositoryProperties();
        properties.setRepoPath(tempDir.toString());
        return properties;
    }

    private void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content.stripLeading());
    }
}
