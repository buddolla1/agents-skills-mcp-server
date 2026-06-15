package com.mcp.tools;

import com.mcp.model.FileContent;
import com.mcp.model.InstructionExecutionResult;
import com.mcp.model.RepositoryFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgentsSkillsMcpTools {

    private static final Logger log = LoggerFactory.getLogger(AgentsSkillsMcpTools.class);

    private final RepositoryFileReader repositoryFileReader;
    private final InstructionExecutionService instructionExecutionService;

    public AgentsSkillsMcpTools(RepositoryFileReader repositoryFileReader, InstructionExecutionService instructionExecutionService) {
        this.repositoryFileReader = repositoryFileReader;
        this.instructionExecutionService = instructionExecutionService;
    }

    @McpTool(name = "listAgents", description = "List all available agent files from the configured agents repository folder")
    public List<RepositoryFile> listAgents() throws IOException {
        log.info("MCP tool accessed: listAgents");
        return repositoryFileReader.list("agent");
    }

    @McpTool(name = "getAgent", description = "Get full agent content by file name or relative path")
    public FileContent getAgent(
            @McpArg(name = "fileNameOrRelativePath", description = "File name or relative path under the agents folder", required = true)
            String fileNameOrRelativePath
    ) throws IOException {
        log.info("MCP tool accessed: getAgent ({})", fileNameOrRelativePath);
        return repositoryFileReader.read("agent", fileNameOrRelativePath);
    }

    @McpTool(name = "searchAgents", description = "Search agents by keyword across file names, paths, and content")
    public List<FileContent> searchAgents(
            @McpArg(name = "keyword", description = "Search term to match against file names, paths, and content", required = true)
            String keyword
    ) throws IOException {
        log.info("MCP tool accessed: searchAgents ({})", keyword);
        return repositoryFileReader.search("agent", keyword);
    }

    @McpTool(name = "listSkills", description = "List all available skill files from the configured skills repository folder")
    public List<RepositoryFile> listSkills() throws IOException {
        log.info("MCP tool accessed: listSkills");
        return repositoryFileReader.list("skill");
    }

    @McpTool(name = "getSkill", description = "Get full skill content by file name or relative path")
    public FileContent getSkill(
            @McpArg(name = "fileNameOrRelativePath", description = "File name or relative path under the skills folder", required = true)
            String fileNameOrRelativePath
    ) throws IOException {
        log.info("MCP tool accessed: getSkill ({})", fileNameOrRelativePath);
        return repositoryFileReader.read("skill", fileNameOrRelativePath);
    }

    @McpTool(name = "searchSkills", description = "Search skills by keyword across file names, paths, and content")
    public List<FileContent> searchSkills(
            @McpArg(name = "keyword", description = "Search term to match against file names, paths, and content", required = true)
            String keyword
    ) throws IOException {
        log.info("MCP tool accessed: searchSkills ({})", keyword);
        return repositoryFileReader.search("skill", keyword);
    }

    @McpTool(name = "listInstructions", description = "List all available instruction files from the configured instructions repository folder")
    public List<RepositoryFile> listInstructions() throws IOException {
        log.info("MCP tool accessed: listInstructions");
        return repositoryFileReader.list("instruction");
    }

    @McpTool(name = "getInstruction", description = "Get full instruction content by file name or relative path")
    public FileContent getInstruction(
            @McpArg(name = "fileNameOrRelativePath", description = "File name or relative path under the instructions folder", required = true)
            String fileNameOrRelativePath
    ) throws IOException {
        log.info("MCP tool accessed: getInstruction ({})", fileNameOrRelativePath);
        return repositoryFileReader.read("instruction", fileNameOrRelativePath);
    }

    @McpTool(name = "searchAll", description = "Search agents, skills, and instructions by keyword")
    public List<FileContent> searchAll(
            @McpArg(name = "keyword", description = "Search term to match against all supported file sets", required = true)
            String keyword
    ) throws IOException {
        log.info("MCP tool accessed: searchAll ({})", keyword);
        List<FileContent> results = new ArrayList<>();
        results.addAll(repositoryFileReader.search("agent", keyword));
        results.addAll(repositoryFileReader.search("skill", keyword));
        results.addAll(repositoryFileReader.search("instruction", keyword));
        return results;
    }

    @McpTool(name = "applySkillToFile", description = "Package a skill and a target file into an execution bundle for downstream processing")
    public InstructionExecutionResult applySkillToFile(
            @McpArg(name = "skillFileOrRelativePath", description = "Skill file name or relative path", required = true)
            String skillFileOrRelativePath,
            @McpArg(name = "filePath", description = "Target file path relative to the configured repo root", required = true)
            String filePath
    ) throws IOException {
        log.info("MCP tool accessed: applySkillToFile (skill={}, file={})", skillFileOrRelativePath, filePath);
        return instructionExecutionService.applySkillToFile(skillFileOrRelativePath, filePath);
    }

    @McpTool(name = "runSkill", description = "Package a skill and a target file into an execution bundle for downstream processing")
    public InstructionExecutionResult runSkill(
            @McpArg(name = "skillFileOrRelativePath", description = "Skill file name or relative path", required = true)
            String skillFileOrRelativePath,
            @McpArg(name = "filePath", description = "Target file path relative to the configured repo root", required = true)
            String filePath
    ) throws IOException {
        log.info("MCP tool accessed: runSkill (skill={}, file={})", skillFileOrRelativePath, filePath);
        return instructionExecutionService.runSkill(skillFileOrRelativePath, filePath);
    }

    @McpTool(name = "runAgentTools", description = "Package an agent and a target file into an execution bundle for downstream processing")
    public InstructionExecutionResult runAgentTools(
            @McpArg(name = "agentFileOrRelativePath", description = "Agent file name or relative path", required = true)
            String agentFileOrRelativePath,
            @McpArg(name = "filePath", description = "Target file path relative to the configured repo root", required = true)
            String filePath
    ) throws IOException {
        log.info("MCP tool accessed: runAgentTools (agent={}, file={})", agentFileOrRelativePath, filePath);
        return instructionExecutionService.runAgentTools(agentFileOrRelativePath, filePath);
    }
}
