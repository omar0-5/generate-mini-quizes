package getrepo;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class repoRead {
        public record Commit(String shortHash, String author, Instant date, String message) {}

        public List<Commit> scanCommit(String link ){
            if (link == null || !link.startsWith("https://")) {
            throw new IllegalArgumentException("linke format should be : https://  , got : " + link);
            }

             List<Commit> commits = new ArrayList<>();

             Path tempDir;
            try {
                tempDir = Files.createTempDirectory("repoquiz-");
            } catch (IOException e) {
                throw new RuntimeException("Could not create temp folder", e);
            }

            try (Git git = Git.cloneRepository()
                .setURI(link)
                .setDirectory(tempDir.toFile())
                .setBare(true)              // history only, no project files checked out
                .call()) {
 
            for (RevCommit c : git.log().call()) {      // newest first, default branch
                if (c.getParentCount() > 1) {
                    continue;                           // skip merge commits
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
            deleteRecursively(tempDir);
        }


             return  commits;
        }
    

    
}
