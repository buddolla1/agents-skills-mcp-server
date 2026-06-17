package com.mcp.tools;

import com.mcp.config.AgentsRepositoryProperties;
import com.mcp.model.FileContent;
import com.mcp.model.RepositoryFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class RepositoryFileReader {

    private static final Logger log = LoggerFactory.getLogger(RepositoryFileReader.class);
    private static final String CLASS_PATH_PREFIX = "classpath*:";

    private final AgentsRepositoryProperties properties;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public RepositoryFileReader(AgentsRepositoryProperties properties) {
        this.properties = properties;
    }

    public List<RepositoryFile> list(String type) throws IOException {
        log.info("Listing {} files from repository", type);
        if (!hasFilesystemRepoPath()) {
            return listClasspath(type);
        }
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
        if (!hasFilesystemRepoPath()) {
            return readClasspath(type, fileNameOrRelativePath);
        }
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
        if (!hasFilesystemRepoPath()) {
            return readClasspathReference(type, reference);
        }
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
        Path base = hasFilesystemRepoPath() ? repoRootPath() : Path.of(System.getProperty("user.dir")).normalize().toAbsolutePath();
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

    private List<RepositoryFile> listClasspath(String type) throws IOException {
        String folder = folder(type);
        Resource[] resources = resourcePatternResolver.getResources(CLASS_PATH_PREFIX + folder + "/**");
        Map<String, RepositoryFile> files = new LinkedHashMap<>();
        for (Resource resource : resources) {
            if (!resource.isReadable() || resource.getFilename() == null || !isAllowedExtension(resource.getFilename())) {
                continue;
            }
            files.putIfAbsent(classpathRelativePath(folder, resource), toRepositoryFile(type, resource, folder));
        }
        return files.values().stream()
                .sorted(Comparator.comparing(RepositoryFile::relativePath))
                .toList();
    }

    private FileContent readClasspath(String type, String fileNameOrRelativePath) throws IOException {
        String folder = folder(type);
        Resource resource = findClasspathResource(folder, fileNameOrRelativePath);
        if (resource == null) {
            throw new IllegalArgumentException("File not found under " + type + ": " + fileNameOrRelativePath);
        }
        return readClasspathResource(type, folder, resource);
    }

    private FileContent readClasspathReference(String type, String reference) throws IOException {
        String folder = folder(type);
        for (String candidate : candidateRelativePaths(type, reference)) {
            Resource resource = findClasspathResource(folder, candidate);
            if (resource != null) {
                return readClasspathResource(type, folder, resource);
            }
        }
        throw new IllegalArgumentException("File not found under " + type + ": " + reference);
    }

    private FileContent readClasspathResource(String type, String folder, Resource resource) throws IOException {
        ensureAllowedExtension(resource);
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String relativePath = classpathRelativePath(folder, resource);
        return new FileContent(resource.getFilename(), relativePath, type, content);
    }

    private Resource findClasspathResource(String folder, String relativePath) throws IOException {
        Resource[] resources = resourcePatternResolver.getResources(CLASS_PATH_PREFIX + folder + "/" + relativePath);
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                return resource;
            }
        }
        return null;
    }

    private RepositoryFile toRepositoryFile(String type, Resource resource, String folder) {
        try {
            return new RepositoryFile(
                    resource.getFilename(),
                    classpathRelativePath(folder, resource),
                    type,
                    resource.contentLength()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file metadata: " + resource.getFilename(), e);
        }
    }

    private String classpathRelativePath(String folder, Resource resource) {
        String location;
        try {
            location = resource.getURL().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to resolve classpath resource location", e);
        }
        String marker = folder + "/";
        int index = location.indexOf(marker);
        if (index >= 0) {
            return location.substring(index + marker.length());
        }
        String filename = resource.getFilename();
        return filename != null ? filename : location;
    }

    private Path basePath(String type) {
        return Path.of(properties.getRepoPath(), folder(type)).normalize().toAbsolutePath();
    }

    private Path repoRootPath() {
        return Path.of(properties.getRepoPath()).normalize().toAbsolutePath();
    }

    private boolean hasFilesystemRepoPath() {
        return StringUtils.hasText(properties.getRepoPath());
    }

    private String folder(String type) {
        return switch (type) {
            case "agent" -> properties.getAgentsPath();
            case "skill" -> properties.getSkillsPath();
            case "instruction" -> properties.getInstructionsPath();
            default -> throw new IllegalArgumentException("Unsupported repository type: " + type);
        };
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

    private boolean isAllowedExtension(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        return properties.getAllowedExtensions().stream()
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::endsWith);
    }

    private void ensureAllowedExtension(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null || !isAllowedExtension(filename)) {
            throw new IllegalArgumentException("File extension not allowed: " + filename);
        }
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
