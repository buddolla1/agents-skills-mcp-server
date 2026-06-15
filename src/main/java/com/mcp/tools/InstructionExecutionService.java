package com.mcp.tools;

import com.mcp.model.FileContent;
import com.mcp.model.InstructionExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Service
public class InstructionExecutionService {

    private static final Logger log = LoggerFactory.getLogger(InstructionExecutionService.class);

    private static final String EXECUTION_NOTE = "This server packages the instruction and target file context for an MCP client to execute. It does not mutate files.";

    private final RepositoryFileReader repositoryFileReader;

    public InstructionExecutionService(RepositoryFileReader repositoryFileReader) {
        this.repositoryFileReader = repositoryFileReader;
    }

    public InstructionExecutionResult applySkillToFile(String skillFileOrRelativePath, String filePath) throws IOException {
        return buildResult("applySkillToFile", "skill", skillFileOrRelativePath, filePath);
    }

    public InstructionExecutionResult runSkill(String skillFileOrRelativePath, String filePath) throws IOException {
        return buildResult("runSkill", "skill", skillFileOrRelativePath, filePath);
    }

    public InstructionExecutionResult runAgentTools(String agentFileOrRelativePath, String filePath) throws IOException {
        return buildResult("runAgentTools", "agent", agentFileOrRelativePath, filePath);
    }

    private InstructionExecutionResult buildResult(
            String operation,
            String instructionType,
            String instructionFileOrRelativePath,
            String filePath
    ) throws IOException {
        log.info("Building execution bundle: operation={}, type={}, instruction={}, target={}",
                operation, instructionType, instructionFileOrRelativePath, filePath);
        if (!StringUtils.hasText(instructionFileOrRelativePath)) {
            throw new IllegalArgumentException("Instruction file path is required");
        }
        if (!StringUtils.hasText(filePath)) {
            throw new IllegalArgumentException("Target file path is required");
        }

        FileContent instruction = repositoryFileReader.readReference(instructionType, instructionFileOrRelativePath);
        FileContent target = repositoryFileReader.readWorkspaceFile(filePath);
        ParsedInstruction parsedInstruction = parseInstruction(instruction);

        return new InstructionExecutionResult(
                operation,
                instructionType,
                parsedInstruction.name(),
                instruction.relativePath(),
                parsedInstruction.description(),
                parsedInstruction.declaredTools(),
                target.name(),
                target.relativePath(),
                target.type(),
                instruction.content(),
                target.content(),
                EXECUTION_NOTE
        );
    }

    private ParsedInstruction parseInstruction(FileContent instruction) {
        String content = instruction.content();
        List<String> declaredTools = new ArrayList<>();
        String name = instruction.name();
        String description = "";

        if (!StringUtils.hasText(content)) {
            return new ParsedInstruction(name, description, declaredTools);
        }

        String[] lines = content.split("\\R");
        boolean inFrontmatter = false;
        boolean frontmatterStarted = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if ("---".equals(trimmed)) {
                if (!frontmatterStarted) {
                    frontmatterStarted = true;
                    inFrontmatter = true;
                    continue;
                }
                break;
            }

            if (!inFrontmatter) {
                break;
            }

            if (trimmed.startsWith("name:")) {
                name = value(trimmed);
            } else if (trimmed.startsWith("description:")) {
                description = value(trimmed);
            } else if (trimmed.startsWith("tools:")) {
                declaredTools = parseListValue(trimmed.substring("tools:".length()).trim());
            }
        }

        return new ParsedInstruction(name, description, declaredTools);
    }

    private String value(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return "";
        }
        String value = line.substring(colon + 1).trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private List<String> parseListValue(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return List.of();
        }

        String normalized = rawValue.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> {
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        return value.substring(1, value.length() - 1);
                    }
                    return value;
                })
                .toList();
    }

    private record ParsedInstruction(String name, String description, List<String> declaredTools) {
    }
}
