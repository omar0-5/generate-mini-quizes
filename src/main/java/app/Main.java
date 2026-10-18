package app;

import getrepo.repoRead;
import getrepo.repoRead.Commit;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String link;
        if (args.length > 0) {
            link = args[0];
        } else {
            Scanner in = new Scanner(System.in);
            System.out.print("Repo link: ");
            link = in.nextLine().trim();
        }

        List<Commit> commits = new repoRead().scanCommit(link);
        System.out.println(commits.size() + " commits");
        for (Commit c : commits) {
            System.out.println(c.shortHash() + "  " + c.date() + "  " + c.author() + "  " + c.message());
        }
    }
}