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
        assertThat(result.executionOutput()).isNull();
    }

    @Test
    void runSkillBuildsCopilotExecutionPrompt() throws IOException {
        RepositoryFileReader repositoryFileReader = new RepositoryFileReader(properties());
        InstructionExecutionService service = new InstructionExecutionService(repositoryFileReader);

        write(tempDir.resolve("github/skills/sample/SKILL.md"), """
                ---
                name: sample-skill
                description: Example skill
                ---
                # Sample Skill
                """);
        write(tempDir.resolve("README.md"), "# Demo");

        InstructionExecutionResult result = service.runSkill("sample", "README.md");

        assertThat(result.operation()).isEqualTo("runSkill");
        assertThat(result.instructionName()).isEqualTo("sample-skill");
        assertThat(result.executionOutput()).contains("Execute the repository skill below against the target file.");
        assertThat(result.executionOutput()).contains("Sample Skill");
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
