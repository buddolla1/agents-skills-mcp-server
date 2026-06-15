package com.mcp.tools;

import com.mcp.config.AgentsRepositoryProperties;
import com.mcp.model.FileContent;
import com.mcp.model.RepositoryFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class RepositoryFileReader {

    private static final Logger log = LoggerFactory.getLogger(RepositoryFileReader.class);

    private final AgentsRepositoryProperties properties;

    public RepositoryFileReader(AgentsRepositoryProperties properties) {
        this.properties = properties;
    }

    public List<RepositoryFile> list(String type) throws IOException {
        log.info("Listing {} files from repository", type);
        Path base = basePath(type);
        ensureDirectoryExists(base);

        try (Stream<Path> stream = Files.walk(base)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isAllowedExtension)
                    .map(path -> toRepositoryFile(base, type, path))
                    .sorted(Comparator.comparing(RepositoryFile::relativePath))
                    .toList();
        }
    }

    public FileContent read(String type, String fileNameOrRelativePath) throws IOException {
        log.info("Reading {} file: {}", type, fileNameOrRelativePath);
        Path base = basePath(type);
        ensureDirectoryExists(base);

        Path file = safeResolve(base, fileNameOrRelativePath);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File not found under " + type + ": " + fileNameOrRelativePath);
        }
        if (!isAllowedExtension(file)) {
            throw new IllegalArgumentException("File extension not allowed: " + fileNameOrRelativePath);
        }

        String content = Files.readString(file, StandardCharsets.UTF_8);
        String relativePath = base.relativize(file).toString();
        return new FileContent(file.getFileName().toString(), relativePath, type, content);
    }

    public FileContent readReference(String type, String reference) throws IOException {
        log.info("Reading {} reference: {}", type, reference);
        Path base = basePath(type);
        ensureDirectoryExists(base);

        for (String candidate : candidateRelativePaths(type, reference)) {
            Path file = safeResolve(base, candidate);
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                continue;
            }
            if (!isAllowedExtension(file)) {
                continue;
            }

            String content = Files.readString(file, StandardCharsets.UTF_8);
            String relativePath = base.relativize(file).toString();
            return new FileContent(file.getFileName().toString(), relativePath, type, content);
        }

        throw new IllegalArgumentException("File not found under " + type + ": " + reference);
    }

    public FileContent readWorkspaceFile(String fileNameOrRelativePath) throws IOException {
        log.info("Reading workspace file: {}", fileNameOrRelativePath);
        Path base = repoRootPath();
        ensureDirectoryExists(base);

        Path file = safeResolve(base, fileNameOrRelativePath);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File not found in workspace: " + fileNameOrRelativePath);
        }
        ensureTextFile(file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        String relativePath = base.relativize(file).toString();
        return new FileContent(file.getFileName().toString(), relativePath, "workspace", content);
    }

    public List<FileContent> search(String type, String keyword) throws IOException {
        log.info("Searching {} files with keyword: {}", type, keyword);
        if (!StringUtils.hasText(keyword)) {
            return list(type).stream()
                    .limit(20)
                    .map(file -> readUnchecked(type, file.relativePath()))
                    .toList();
        }

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return list(type).stream()
                .filter(file -> file.name().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || file.relativePath().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || readUnchecked(type, file.relativePath()).content().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .limit(20)
                .map(file -> readUnchecked(type, file.relativePath()))
                .toList();
    }

    private FileContent readUnchecked(String type, String relativePath) {
        try {
            return read(type, relativePath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file: " + relativePath, e);
        }
    }

    private RepositoryFile toRepositoryFile(Path base, String type, Path path) {
        try {
            return new RepositoryFile(
                    path.getFileName().toString(),
                    base.relativize(path).toString(),
                    type,
                    Files.size(path)
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file metadata: " + path, e);
        }
    }

    private Path basePath(String type) {
        String folder = switch (type) {
            case "agent" -> properties.getAgentsPath();
            case "skill" -> properties.getSkillsPath();
            case "instruction" -> properties.getInstructionsPath();
            default -> throw new IllegalArgumentException("Unsupported repository type: " + type);
        };
        return Path.of(properties.getRepoPath(), folder).normalize().toAbsolutePath();
    }

    private Path repoRootPath() {
        return Path.of(properties.getRepoPath()).normalize().toAbsolutePath();
    }

    private List<String> candidateRelativePaths(String type, String reference) {
        List<String> candidates = new ArrayList<>();
        candidates.add(reference);

        if (!reference.contains("/") && !reference.contains("\\")) {
            candidates.add(reference + ".md");
            if ("skill".equals(type)) {
                candidates.add(reference + "/SKILL.md");
                candidates.add(reference + "/skill.md");
            } else if ("agent".equals(type)) {
                candidates.add(reference + ".agent.md");
                candidates.add(reference + "/" + reference + ".agent.md");
            } else if ("instruction".equals(type)) {
                candidates.add(reference + ".instruction.md");
            }
        }

        return candidates;
    }

    private Path safeResolve(Path base, String fileNameOrRelativePath) {
        if (!StringUtils.hasText(fileNameOrRelativePath)) {
            throw new IllegalArgumentException("File name is required");
        }
        Path resolved = base.resolve(fileNameOrRelativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Invalid path. Path traversal is not allowed.");
        }
        return resolved;
    }

    private boolean isAllowedExtension(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return properties.getAllowedExtensions().stream()
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .anyMatch(fileName::endsWith);
    }

    private void ensureDirectoryExists(Path base) {
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            throw new IllegalStateException("Configured folder does not exist: " + base);
        }
    }

    private void ensureTextFile(Path file) throws IOException {
        long maxSizeBytes = 1_000_000;
        if (Files.size(file) > maxSizeBytes) {
            throw new IllegalArgumentException("File is too large to read safely: " + file);
        }

        byte[] content = Files.readAllBytes(file);
        for (byte b : content) {
            if (b == 0) {
                throw new IllegalArgumentException("Binary files are not supported: " + file);
            }
        }
    }
}
