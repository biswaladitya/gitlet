package gitlet;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.io.FileWriter;
/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Aditya Prasad Biswalc
 */
public class Main {

    private static Repo repo;
    private static final File CWD = new File(".");


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        if (!checkInitialized(args)) {
            return;
        };

        repo = new Repo(CWD.getPath());

        String argument = args[0];

        switch (argument) {
            case "merge":
                String mergedBranch = args[1];
                executeMerge(mergedBranch);
                break;
            case "reset" :
                String hashId = args[1];
                executeReset(hashId);
                break;
            case "rm-branch":
                String removedBranch = args[1];
                removeBranch(removedBranch);
                break;
            case "rm":
                String removedFile = args[1];
                removeFile(removedFile);
                break;
            case "find":
                String findMessage = args[1];
                executeFind(findMessage);
                break;
            case "global-log":
                displayGlobalLog();
                break;
            case "status":
                displayStatus();
                break;
            case "branch":
                String branchName = args[1];
                createBranch(branchName);
                break;
            case "log":
                displayLog();
                break;
            case "checkout":
                if (args.length == 2) {
                    String branch = args[1];
                    executeBranchCheckout(branch);
                    break;
                } else if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    String fileName = args[2];
                    executeFileCheckout(fileName);
                    break;
                } else {
                    String commitId = args[1].substring(0, 7);
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    String fileName = args[3];
                    executeCommitFileCheckout(commitId, fileName);
                    break;
                }
            case "commit":

                if (args.length < 2) {
                    System.out.println("Please enter a commit message.");
                    return;
                }

                String message = args[1];

                if (message.equals("")) {
                    System.out.println("Please enter a commit message.");
                }

                String parentCommit = "";
                HashMap<String, String> commitMap = new HashMap<String,String>();

                Date date = new Date();
                Commit commit = new Commit(message, date, parentCommit, commitMap);

                executeCommit(commit);

                break;
            case "add":

                if (args.length < 2) {
                    throw new GitletException("Invalid command");
                }

                String fileName = args[1];
                File file = new File(CWD.getPath() + "/" + fileName);

                if (!file.exists()) {
                    System.out.println("File does not exist.");
                    return;
                }
                executeAdd(file);
                break;

            case "init":
                executeInit();
                break;

            default:
                System.out.println("No command with that name exists.");
        }
        return;
    }

    private static void executeMerge(String mergedBranch) {
        repo.doMerge(mergedBranch);
    }

    private static void executeReset(String hashId) {
        repo.reset(hashId, false);
    }

    private static void removeBranch(String branchName) {
        repo.removeBranch(branchName);
    }
    private static void removeFile(String file) {
        repo.removeFile(file);
    }

    private static void executeFind(String message) {
        repo.find(message);
    }

    private static void displayGlobalLog() {
        repo.displayGlobalLog();
    }

    private static void displayStatus() {
        repo.status();
    }


    private static void createBranch(String branchName) {
        repo.createBranch(branchName) ;
    }
    private static void displayLog() {
        repo.displayLog();
    }

    private static void executeBranchCheckout(String branchName) {
        repo.checkoutBranch(branchName);
    }

    private static void executeCommitFileCheckout(String commitId, String fileName) {
        repo.commitFileCheckout(commitId, fileName);
    }

    private static void executeFileCheckout(String fileName) {
        repo.checkoutFile(fileName);
    }


    private static void executeInit() {
        repo.init();
    }

    private static void executeAdd(File file) {
        repo.add(file);
    }

    private static void executeCommit(Commit commit) {
        repo.commit(commit);
    }



    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(-1);
    }

    private static boolean checkInitialized(String [] args) {
        if (args[0].equals("init")) {
            return true;
        }
        if (!args[0].equals("init")) {
            String[] files = CWD.list();
            for (String file : files) {
                if (file.equals(".gitlet")) {
                    return true;
                }
            }
        }

        System.out.println("Not in an initialized Gitlet directory.");
        return false;
    }

}
