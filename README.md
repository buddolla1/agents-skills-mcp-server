# Agents Skills MCP Server

Spring Boot MCP server for exposing your `agents-skills-instructions` repository as MCP tools.

Package: `com.mcp`

## What it exposes

MCP tools:

- `listAgents`
- `getAgent`
- `searchAgents`
- `listSkills`
- `getSkill`
- `searchSkills`
- `listInstructions`
- `getInstruction`
- `searchAll`
- `applySkillToFile`
- `runSkill`
- `runAgentTools`

MCP resources:

- `repo://agents`
- `repo://skills`
- `repo://instructions`

## How to access them

This server exposes the repository in two ways:

1. Tools for listing, reading, and searching files.
2. Resources for reading a JSON index of each folder.
3. Execution-bundle tools that combine a skill or agent with a target file for downstream processing.

### Agents

Use the tools below:

- `listAgents`
- `getAgent`
- `searchAgents`

Resource:

- `repo://agents`

### Skills

Use the tools below:

- `listSkills`
- `getSkill`
- `searchSkills`

Resource:

- `repo://skills`

### Instructions

Use the tools below:

- `listInstructions`
- `getInstruction`
- `searchAll`

Resource:

- `repo://instructions`

### Skill and agent execution bundles

Use these tools to package a skill or agent together with a target file:

- `applySkillToFile`
- `runSkill`
- `runAgentTools`

`applySkillToFile` and `runAgentTools` return the instruction content, the target file content, and parsed metadata such as declared agent tools. They do not edit files directly.

`runSkill` packages the same context and also returns a Copilot-ready execution prompt in `executionOutput`. That prompt is what a Copilot client can use to run the skill.

You can pass a bare skill or agent name for the instruction input, for example `yaml-validator`. The server resolves that to the conventional markdown file path when possible.

### Typical MCP flow

1. Call `listAgents`, `listSkills`, or `listInstructions` to discover files.
2. Use `getAgent`, `getSkill`, or `getInstruction` to read one file by name or relative path.
3. Use `searchAgents`, `searchSkills`, or `searchAll` to find files by keyword.
4. Use `applySkillToFile`, `runSkill`, or `runAgentTools` when you want the server to package an instruction and a target file for downstream execution.
5. If your client supports resources, read `repo://agents`, `repo://skills`, or `repo://instructions` for a JSON index.

## 6. Copilot execution

This server does not call a Copilot API directly. It returns a Copilot-ready prompt and the full skill/file context so the client can execute the skill in Copilot Chat or another Copilot-backed workflow.

## 1. Clone your agents repo

```bash
git clone -b agents-skills-instructions-v1 \
  https://github.com/buddolla1/agents-skills-instructions.git \
  ~/agents-skills-instructions
```

## 2. Configure repo path

Edit `src/main/resources/application.yml`:

```yaml
agents:
  repo-path: ${AGENTS_REPO_PATH:/Users/YOUR_USER/agents-skills-instructions}
```

The server expects these folders under `repo-path`:

- `github/agents`
- `github/skills`
- `github/instructions`

The MCP resources read from the configured `repo-path` and return a JSON index for each folder.

Or run with env variable:

```bash
export AGENTS_REPO_PATH=$HOME/agents-skills-instructions
```

## 3. Run

```bash
./gradlew bootRun
```

## 4. MCP endpoint

For WebMVC MCP server:

```text
http://localhost:8080/api/mcp
```

VS Code should use the `http` transport against this Streamable HTTP endpoint.

Example client configuration:

```json
{
  "servers": {
    "agents-skills-mcp-server": {
      "url": "http://localhost:8080/api/mcp",
      "type": "http"
    }
  }
}
```

## 5. REST health check

```bash
curl http://localhost:8080/actuator/health
```

## VS Code integration

This repo includes a VS Code MCP config at `.vscode/mcp.json`.
Open the folder in VS Code, then use the Copilot MCP server picker to load the server and call the tools above.

## Notes

- This server reads files from your local cloned repo.
- It prevents path traversal by resolving files only under configured folders.
- Start with read-only MCP tools before adding agent execution.
