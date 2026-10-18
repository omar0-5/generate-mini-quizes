package getrepo;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

public class repoRead {
    public record Commit(String shortHash, String author, Instant date, String message) {}

    public List<Commit> scanCommit(String link) {
        if (link == null || !link.startsWith("https://")) {
            throw new IllegalArgumentException("link format should be https://..., got: " + link);
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("repoquiz-");
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp folder", e);
        }

        try (Git git = Git.cloneRepository()
                .setURI(link)
                .setDirectory(tempDir.toFile())
                .setBare(true)
                .call()) {

            List<Commit> commits = new ArrayList<>();
            for (RevCommit c : git.log().call()) {
                if (c.getParentCount() > 1) {
                    continue;
                }
                commits.add(new Commit(
                        c.getId().abbreviate(7).name(),
                        c.getAuthorIdent().getName(),
                        Instant.ofEpochSecond(c.getCommitTime()),
                        c.getFullMessage().trim()
                ));
            }
            return commits;

        } catch (GitAPIException e) {
            throw new RuntimeException("Could not read repository " + link + ": " + e.getMessage(), e);
        } finally {
            delDir(tempDir);
        }
    }

    private void delDir(Path dir) {
        List<Path> order = new ArrayList<>();
        Deque<Path> stack = new ArrayDeque<>();
        stack.push(dir);

        // Pass 1: DFS with an explicit stack; every parent is recorded before its children
        while (!stack.isEmpty()) {
            Path p = stack.pop();
            order.add(p);
            if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                try (DirectoryStream<Path> children = Files.newDirectoryStream(p)) {
                    for (Path child : children) {
                        stack.push(child);
                    }
                } catch (IOException e) {
                    System.err.println("Warning: could not list " + p);
                }
            }
        }

        // Pass 2: delete in reverse, so children go before their parents
        for (int i = order.size() - 1; i >= 0; i--) {
            try {
                Files.delete(order.get(i));
            } catch (IOException e) {
                System.err.println("Warning: could not delete " + order.get(i));
            }
        }
    }

    // public static void main(String[] args) {
    //     List<Commit> commits = new repoRead().scanCommit("https://github.com/octocat/Hello-World.git");
    //     System.out.println(commits.size() + " commits");
    //     for (Commit c : commits) {
    //         System.out.println(c.shortHash() + "  " + c.date() + "  " + c.author() + "  " + c.message());
    //     }
    // }
}