package gitlet;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.util.*;
import java.io.*;

public class Repo {

    File CWD;
    File gitDir;
    File data;
    File commits;
    File staging;
    File stagingAdds;
    File stagingRemove;
    File logs;
    File branch;
    File branchMap;

    /** Repo constructor that goes ahead and sets up the directories as well as creating a master commit branch */

    public Repo (String path) {
        CWD = new File(path);
    }

    private void makeDirectoryPointers() {
        gitDir = Utils.join(CWD, "/.gitlet");

        this.data = Utils.join(gitDir, "/data");
        this.commits = Utils.join(gitDir, "/commits");
        this.staging = Utils.join(gitDir, "/staging");

        this.stagingAdds = Utils.join(staging,"/addition");
        this.stagingRemove = Utils.join(staging,"/removal");


        this.logs = Utils.join(gitDir, "/logs.txt");
        this.branch = Utils.join(gitDir, "/branch");
        this.branchMap = Utils.join(gitDir, "/branchMap");

    }

    /** Method to initialize a git repository and set it up */
    public void init() {
        makeDirectoryPointers();

        if (gitDir.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }

        gitDir.mkdir();
        data.mkdir();
        commits.mkdir();
        staging.mkdir();
        stagingAdds.mkdir();
        stagingRemove.mkdir();

        try {
            logs.createNewFile();
            branch.createNewFile();
            branchMap.createNewFile();
        } catch (IOException exp) {
        }

        HashMap<String, String> emptyMap = new HashMap<String, String>();


        LocalDateTime currentTime = LocalDateTime.now();
        Commit initialCommit = new Commit("initial commit", null, "", emptyMap);
        initialCommit.setCommitTime("Wed Dec 31 16:00:00 1969 -0800");

        byte[] initialCommitContents = Utils.serialize(initialCommit);
        String initialCommitHash = Utils.sha1(initialCommitContents);
        initialCommit.setHashId(initialCommitHash);

        File initialCommitFile = Utils.join(commits, "/" + initialCommitHash.substring(0,7));
        try {
            initialCommitFile.createNewFile();
            initialCommitContents = Utils.serialize(initialCommit);
            Utils.writeContents(initialCommitFile, initialCommitContents);
        } catch (IOException exp) {
        }

        try {
            FileWriter fw = new FileWriter(branch);
            fw.write("");
            fw.write(initialCommit.getHash() + "\n");
            fw.write("master");
            fw.close();

            HashMap<String, String> branches = new HashMap<String, String>();
            branches.put("master", initialCommit.getHash());
            byte [] branchesMapBytes = Utils.serialize(branches);
            Utils.writeContents(branchMap, branchesMapBytes);

        } catch (IOException exp) {
        }

    }

    /** handles the functionality of git add. the parameters to the function is a file object represnting the file
     * being added.
     * @param file
     */

    public void add (File file) {
        makeDirectoryPointers();
        List<String> stagingAddFiles = Utils.plainFilenamesIn(stagingAdds);
        File stagingAddFile = Utils.join(stagingAdds, "/" + file.getName());

        if (latestCommitTracks(file.getName())) {
            Commit lastCommit = getLastCommit();
            String fileHash = lastCommit.getCommitData().get(file.getName());
            byte [] contentInLastCommit = Utils.readContents(Utils.join(data, "/" + fileHash));
            byte [] currentContents = Utils.readContents(file);
            if (Arrays.equals(contentInLastCommit, currentContents)) {
                if (isStagedRemoval(file.getName())) {
                    File removedFile = Utils.join(stagingRemove, "/" + file.getName());
                    removedFile.delete();
                    return;
                }
                return;
            }
        }

        if (!stagingAddFile.exists()) {
            try {
                stagingAddFile.createNewFile();
            } catch (IOException exp) {
            }
        }

        byte[] fileContents = Utils.readContents(file);
        Utils.writeContents(stagingAddFile, fileContents);
    }



    public void removeFile(String fileName) {
        makeDirectoryPointers();

        if (!isStagedAddition(fileName) && !latestCommitTracks(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }

        if (isStagedAddition(fileName)) {
            File addedFile = Utils.join(stagingAdds, "/" + fileName);
            addedFile.delete();
        }

        if (latestCommitTracks(fileName)) {
            File removedFile = Utils.join(stagingRemove, "/" + fileName);
            try {
                removedFile.createNewFile();
            } catch (IOException exp) {
            }
            File f = Utils.join(CWD, "/" + fileName);
            f.delete();
        }
    }

    public void commit(Commit com) {

        makeDirectoryPointers();

        String logContents = Utils.readContentsAsString(branch);
        String[] logContentArr = logContents.split(" ");
        String parentHash = logContentArr[0];
        com.setParentCommit(parentHash);

        String[] fileNames;
        fileNames = stagingAdds.list();

        String [] removedFiles = stagingRemove.list();

        if (fileNames.length == 0 && removedFiles.length == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        copyParentData(com);

        for (String removedFile : stagingRemove.list()) {
            if (com.getCommitData().containsKey(removedFile)) {
                com.getCommitData().remove(removedFile);
            }
        }


        for (String name : fileNames) {
            File stagingFile = Utils.join(stagingAdds, "/" + name);
            byte[] fileContents = Utils.readContents(stagingFile);
            String fileHash = Utils.sha1(fileContents).substring(0, 7);
            com.updateCommitData(name, fileHash);

            File dataFile = Utils.join(data, "/" + fileHash);

            try {
                dataFile.createNewFile();
            } catch (IOException exp) {
            };

            Utils.writeContents(dataFile, fileContents);
        }


        for (File stagingAddFile : stagingAdds.listFiles()) {
            if (stagingAddFile.isFile()) {
                stagingAddFile.delete();
            }
        }

        for (File stagingAddFile : stagingRemove.listFiles()) {
            if (stagingAddFile.isFile()) {
                stagingAddFile.delete();
            }
        }

        byte[] commitContents = Utils.serialize(com);
        String commitHash = Utils.sha1(commitContents);
        com.setHashId(commitHash);


        File commitFile = Utils.join(commits, "/" + commitHash.substring(0, 7));

        try {
            commitFile.createNewFile();
            commitContents = Utils.serialize(com);
            Utils.writeContents(commitFile, commitContents);
        } catch (IOException exp) {
        }

        try {
            Scanner scan = new Scanner(branch);
            scan.next();
            String b = scan.next();

            FileWriter fw = new FileWriter(branch);
            fw.write("");
            fw.write(com.getHash() + "\n");
            fw.write(b);
            fw.close();

            HashMap<String, String> branches = Utils.readObject(branchMap, HashMap.class);
            branches.put(b, com.getHash());
            Utils.writeContents(branchMap, Utils.serialize(branches));

        } catch (IOException exp) {
        }

    }

    private boolean commitTracks(Commit commit, String fileName) {


        File f = Utils.join(CWD, "/" + fileName);

        if (!commit.getCommitData().containsKey(fileName)) {
            return false;
        }

        if (commit.getCommitData().containsKey(fileName) && !f.exists()) {
            return true;
        }

        byte [] CWDFileContents = Utils.readContents(f);

        String fileHash = commit.getCommitData().get(fileName);
        byte [] commitFileContents = Utils.readContents(Utils.join(data, "/" + fileHash));

        if (!Arrays.equals(CWDFileContents, commitFileContents)) {
            return false;
        }

        return true;
    }

    /** restores fileName's contents to that which belongs to the specific commitID*/

    public void commitFileCheckout(String commitId, String fileName) {

        makeDirectoryPointers();
        File commitPath = Utils.join(commits,"/" + commitId.substring(0, 7));
        if (!commitPath.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit commit = Utils.readObject(commitPath, Commit.class);
        
        HashMap<String, String> commitMap = commit.getCommitData();

        if (!commitMap.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        String fileHash = commitMap.get(fileName);
        File file = Utils.join(data, "/" + fileHash.substring(0,7));
        byte [] fileContents = Utils.readContents(file);

        File filePath = Utils.join(CWD, "/" + fileName);

        if (!filePath.exists()) {
            try {
                filePath.createNewFile();
            } catch (IOException exp) {
            }
        }

        Utils.writeContents(filePath, fileContents);

    }

    public void checkoutFile(String fileName) {
        makeDirectoryPointers();
        String latestCommitHash = "";
        Commit latestCommit = getLastCommit();

        HashMap<String, String> latestCommitMap = latestCommit.getCommitData();
        if (!latestCommitMap.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String fileHash = latestCommitMap.get(fileName);


        File file = Utils.join(data, "/" + fileHash.substring(0,7));
        byte [] fileContents = Utils.readContents(file);
        File filePath = Utils.join(CWD, "/" + fileName);

        if (!filePath.exists()) {
            try {
                filePath.createNewFile();
            } catch (IOException exp) {
            }
        }

        Utils.writeContents(filePath, fileContents);
    }

    public void createBranch(String branchName) {
        makeDirectoryPointers();
        HashMap<String, String> branches = Utils.readObject(branchMap, HashMap.class);
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branches.put(branchName, getLastCommit().getHash());
        Utils.writeContents(branchMap, Utils.serialize(branches));
    }

    public Commit getLastCommit() {
        makeDirectoryPointers();
        String commitHash = "" ;
        try {
            Scanner scan = new Scanner(branch);
            commitHash = scan.next();
        } catch (FileNotFoundException fnf) {
        }

        Commit commit = Utils.readObject(Utils.join(commits,"/" + commitHash.substring(0, 7)), Commit.class);
        return commit;

    }

    private boolean isStaged(String fileName) {
        if (isStagedAddition(fileName)) {
            return true;
        } else if (isStagedRemoval(fileName)) {
            return true;
        }
        return false;
    }

    public void reset(String commitHash, boolean usedInCheckoutBranch) {
        makeDirectoryPointers();
        File commitPath = Utils.join(commits, "/" + commitHash.substring(0,7));
        if (!commitPath.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit targetCommit = Utils.readObject(commitPath, Commit.class);
        Commit currentBranchCommit = getLastCommit();

        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        Iterator itr = cwdFiles.iterator();
        String fileName = "";

        while (itr.hasNext()) {
            fileName = (String) itr.next();

            if (!fileName.endsWith("txt")) {
                continue;
            }

            if (isStaged(fileName)) {
                File f = Utils.join(CWD, "/" + fileName);
                f.delete();
            } else if (!commitTracks(currentBranchCommit, fileName) && !commitTracks(targetCommit, fileName)
            && !isStaged(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            } else if (commitTracks(currentBranchCommit, fileName) && !commitTracks(targetCommit, fileName)) {
                File f = Utils.join(CWD, "/" + fileName);
                f.delete();
            } else if (commitTracks(targetCommit, fileName)) {
                String hash = targetCommit.getCommitData().get(fileName).substring(0,7);
                File dataFile = Utils.join(data, "/" + hash);
                byte [] targetFileContents = Utils.readContents(dataFile);
                File cwdFile = Utils.join(CWD, "/" + fileName);
                Utils.writeContents(cwdFile, targetFileContents);
            }
        }

        HashMap<String, String> targetCommitMap = targetCommit.getCommitData();
        Iterator keysIter = targetCommitMap.keySet().iterator();
        String s = "";

        while (keysIter.hasNext()) {
            s = (String) keysIter.next();
            File file = Utils.join(CWD, "/" + s);

            if (!file.exists()) {
                try{
                    file.createNewFile();

                    File targetDataFile = Utils.join(data, "/" + targetCommitMap.get(s).substring(0,7));
                    byte [] checkedFileContents = Utils.readContents(targetDataFile);
                    Utils.writeContents(file, checkedFileContents);

                } catch (IOException exp) {
                }
            }
        }



        File [] stagingAddFiles = stagingAdds.listFiles();
        File [] stagingRemoveFiles = stagingRemove.listFiles();

        for (File addedFile: stagingAddFiles) {
            addedFile.delete();
        }

        for (File removedFile: stagingAddFiles) {
            removedFile.delete();
        }

        if (!usedInCheckoutBranch) {
            try {
                String b = getCurrentBranch();

                FileWriter fw = new FileWriter(branch);
                fw.write("");
                fw.write(commitHash + "\n");
                fw.write(b);
                fw.close();

                HashMap<String, String> branchHashMap = Utils.readObject(branchMap, HashMap.class);
                branchHashMap.put(getCurrentBranch(), commitHash);
                Utils.writeContents(branchMap, Utils.serialize(branchHashMap));

            } catch (IOException exp) {
            }
        }

    }



    public void checkoutBranch(String branchName) {
        makeDirectoryPointers();

        Commit currentBranchCommit = getLastCommit();

        HashMap<String, String> branches = Utils.readObject(branchMap, HashMap.class);

        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists");
            return;
        } else if (getCurrentBranch().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        String commitHash = branches.get(branchName);

        reset(commitHash, true);

        try {
            FileWriter fw = new FileWriter(branch);
            fw.write("");
            fw.write(commitHash + "\n");
            fw.write(branchName);
            fw.close();

        } catch (IOException exp) {
        };
    }

    private boolean isTracked (String fileName) {
        makeDirectoryPointers();
        HashMap<String, String> branches = Utils.readObject(branchMap, HashMap.class);
        Iterator itr = branches.keySet().iterator();
        String branch = "";
        String commitHash = "";

        while (itr.hasNext()) {
            branch = (String) itr.next();
            commitHash = branches.get(branch);
            Commit commit = Utils.readObject(Utils.join(commits,"/" + commitHash.substring(0, 7)), Commit.class);
            if (commitTracks(commit, fileName)) {
                return true;
            }
        }
        return false;

    }


    private String getCurrentBranch() {
        String name = "";
        try {
            Scanner scan = new Scanner(branch);
            scan.next();
            name = scan.next();
            return name;
        } catch (FileNotFoundException fnf) {
        }
        return name;

    }

    public void displayGlobalLog() {

        makeDirectoryPointers();

        File [] commitArr = commits.listFiles();

        for (File commitFile: commitArr) {
            Commit commit = Utils.readObject(commitFile, Commit.class);
            String [] commitDetails = commit.getCommitDetails();
            System.out.println("===");
            System.out.println(commitDetails[0]);
            System.out.println(commitDetails[1]);
            System.out.println(commitDetails[2]);
            System.out.println();
        }

    }

    public void find(String message) {
        makeDirectoryPointers();
        File [] commitsArr = commits.listFiles();
        boolean anyCommitsFound = false;

        for(File commitFile: commitsArr) {
            Commit commit = Utils.readObject(commitFile, Commit.class);
            if (commit.getCommitMessage().equals(message)) {
                anyCommitsFound = true;
                System.out.println(commit.getHash());
            }
        }
        if (!anyCommitsFound) {
            System.out.println("Found no commit with that message.");
            return;
        }
    }

    public void displayLog() {
        makeDirectoryPointers();

        Commit commit = getLastCommit();
        String commitHash = commit.getHash();

        while (!commitHash.equals("")) {
            String[] commitDetails = commit.getCommitDetails();

            System.out.println("===");
            System.out.println(commitDetails[0]);
            System.out.println(commitDetails[1]);
            System.out.println(commitDetails[2]);
            System.out.println();


            if (commit.getParentHash().equals("")) {
                break;
            }

            File parentCommit = Utils.join(commits, "/" + commit.getParentHash().substring(0, 7));
            commit = Utils.readObject(parentCommit, Commit.class);
            commitHash = commit.getHash();

        }
    }

    private Commit getParentCommit(Commit commit) {
        String parentHash = commit.getParentHash().substring(0, 7);
        File parentCommitFile = Utils.join(commits, "/" + parentHash);
        Commit parentCommit = Utils.readObject(parentCommitFile, Commit.class);
        return parentCommit;
    }

    private void copyParentData(Commit commit) {
        Commit parentCommit = getParentCommit(commit);
        for (Map.Entry<String, String> entry: parentCommit.getCommitData().entrySet()) {
            commit.updateCommitData(entry.getKey(), entry.getValue());
        }

    }

    private boolean isStagedAddition(String fileName) {
        String [] fileNames = stagingAdds.list();
        HashSet<String> fileNamesSet = new HashSet<String>(Arrays.asList(fileNames));
        return fileNamesSet.contains(fileName);
    }

    private boolean isStagedRemoval(String fileName) {
        String [] fileNames = stagingRemove.list();
        HashSet<String> fileNamesSet = new HashSet<String>(Arrays.asList(fileNames));
        return fileNamesSet.contains(fileName);
    }

    private boolean latestCommitTracks(String fileName) {
        Commit commit = getLastCommit();
        return commit.getCommitData().containsKey(fileName);
    }

    private boolean existsInCWD(String fileName) {
        List<String> workingDirFiles = Utils.plainFilenamesIn(CWD);
        Iterator itr = workingDirFiles.iterator();

        while (itr.hasNext()) {
            if (itr.next().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    private boolean changedInCWD(String fileName) {

        if (!existsInCWD(fileName)) {
            return false;
        }

        Commit commit = getLastCommit();
        File dataFile;
        if (!commit.getCommitData().containsKey(fileName)) {
            dataFile = Utils.join(stagingAdds, "/" + fileName);
        } else {
            dataFile = Utils.join(data, "/" + commit.getCommitData().get(fileName));
        }

        byte [] contentsInLastCommit = Utils.readContents(dataFile);
        byte [] contentsInWorkingDir = Utils.readContents(Utils.join(CWD, "/" + fileName));
        return !Arrays.equals(contentsInLastCommit, contentsInWorkingDir);
    }

    public void removeBranch(String branchName) {
        makeDirectoryPointers();

        HashMap<String, String> existingBranches = Utils.readObject(branchMap, HashMap.class);

        if (!existingBranches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (getCurrentBranch().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        existingBranches.remove(branchName);
        Utils.writeContents(branchMap, Utils.serialize(existingBranches));

    }

    public void status() {

        makeDirectoryPointers();

        PriorityQueue<String> stagedFiles = new PriorityQueue<String>();
        PriorityQueue<String> modifiedFiles = new PriorityQueue<String>();
        PriorityQueue<String> untrackedFiles = new PriorityQueue<String>();

        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        Iterator itr = cwdFiles.iterator();
        String fileName = "";

        while (itr.hasNext()) {
            fileName = (String) itr.next();
            if (latestCommitTracks(fileName) && changedInCWD(fileName) && !isStagedAddition(fileName)) {
                modifiedFiles.add(fileName + " (modified)");
            } else if ((isStagedAddition(fileName) || isStagedRemoval(fileName)) && changedInCWD(fileName)) {
                modifiedFiles.add(fileName + " (modified)");
            } else if (!latestCommitTracks(fileName) && !(isStagedAddition(fileName) || isStagedRemoval(fileName))) {
                untrackedFiles.add(fileName);
            }
        }

        String [] stagedAddition = stagingAdds.list();

        for (String staged: stagedAddition) {
            stagedFiles.add(staged);
            if (!existsInCWD(fileName)) {
                modifiedFiles.add(fileName + " (deleted)");
            }
        }

        Commit lastCommit = getLastCommit();
        Set<String> trackedFiles = lastCommit.getCommitData().keySet();
        Iterator trackedItr = trackedFiles.iterator();


        while(trackedItr.hasNext()) {
            fileName = (String) trackedItr.next();
            if (!existsInCWD(fileName) && !isStagedRemoval(fileName)) {
                modifiedFiles.add(fileName + " (deleted)");
            }
        }

        PriorityQueue<String> branches = new PriorityQueue<String>();
        Iterator keySetIterator = Utils.readObject(branchMap, HashMap.class).keySet().iterator();
        String branchName = "";

        while (keySetIterator.hasNext()) {
            branchName = (String) keySetIterator.next();
            if (branchName.equals(getCurrentBranch())) {
                branchName = "*" + branchName;
            }
            branches.add(branchName);
        }

        PriorityQueue<String> removedFiles = new PriorityQueue<String>();

        String [] removed = stagingRemove.list();
        for (String r: removed) {
            removedFiles.add(r);
        }

        System.out.println("=== Branches ===");
        while (!branches.isEmpty()) {
            System.out.println(branches.poll());
        }
        System.out.println();
        System.out.println("=== Staged Files ===");

        while (!stagedFiles.isEmpty()) {
            System.out.println(stagedFiles.poll());
        }

        System.out.println();
        System.out.println("=== Removed Files ===");

        while (!removedFiles.isEmpty()) {
            System.out.println(removedFiles.poll());
        }

        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");

        while (!modifiedFiles.isEmpty()) {
            System.out.println(modifiedFiles.poll());
        }

        System.out.println();
        System.out.println("=== Untracked Files ===");

        while (!untrackedFiles.isEmpty()) {
            System.out.println(untrackedFiles.poll());
        }
    }

    private HashSet<String> doBFS(String startHash) {

        HashSet<String> visited = new HashSet<String>();
        Queue<String> queue = new LinkedList<String>();
        HashSet<String> finalOutput = new HashSet<String>();

        visited.add(startHash.substring(0, 7));
        queue.add(startHash.substring(0, 7));
        String currentHash = "";

        while (!queue.isEmpty()) {
            currentHash = queue.poll();

            if (currentHash.equals("")) {
                break;
            }
            finalOutput.add(currentHash.substring(0, 7));

            Commit currentCommit = Utils.readObject(Utils.join(commits, "/" + currentHash.substring(0, 7)), Commit.class);
            String parentHash = currentCommit.getParentHash();
            if (parentHash.length() > 7) {
                parentHash = parentHash.substring(0, 7);
            }

            ArrayList<String> adjacent = new ArrayList<String>(Arrays.asList(parentHash));

            if (currentCommit.getMergedParent() != null) {
                adjacent.add(currentCommit.getMergedParent().substring(0, 7));
            }

            for (String adjacentHash : adjacent) {
                if (!visited.contains(adjacentHash)) {
                    visited.add(adjacentHash);
                    queue.add(adjacentHash);
                }
            }
        }
        return finalOutput;
    }

    private String getSplitCommit (String branch1, String branch2) {

        HashMap<String, String> branches = Utils.readObject(branchMap, HashMap.class);

        HashSet<String> branch1Nodes = doBFS(branches.get(branch1).substring(0, 7));
        String splitPoint = "";

        Queue<String> queue = new LinkedList();
        HashSet<String> visited = new HashSet<String>();

        queue.add(branches.get(branch2));

        if (branch1Nodes.contains(branches.get(branch2).substring(0, 7))) {
            return branches.get(branch2);
        }

        visited.add(branches.get(branch2).substring(0, 7));
        String currentHash = "";


        while (!queue.isEmpty()) {
            currentHash = queue.poll();
            if (currentHash.equals("")) {
                break;
            }
            Commit currentCommit = Utils.readObject(Utils.join(commits, "/" + currentHash.substring(0, 7)), Commit.class);

            String parentHash = currentCommit.getParentHash();
            if (parentHash.length() > 7) {
                parentHash = parentHash.substring(0, 7);
            }

            ArrayList<String> adjacent = new ArrayList<String>(Arrays.asList(parentHash));


            if (currentCommit.getMergedParent() != null) {
                adjacent.add(currentCommit.getMergedParent().substring(0, 7));
            }

            for (String adjacentHash : adjacent) {
                if (branch1Nodes.contains(adjacentHash)) {
                    return adjacentHash;
                }
                if (!visited.contains(adjacentHash)) {
                    visited.add(adjacentHash);
                    queue.add(adjacentHash);
                }
            }
        }

        return splitPoint;

    }


    public void doMerge(String mergedBranch) {
        makeDirectoryPointers();

        String [] addedFiles = stagingAdds.list();
        String [] removedFiles = stagingRemove.list();

        if (addedFiles.length > 0 || removedFiles.length > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        HashMap<String, String> branches =
                Utils.readObject(branchMap, HashMap.class);

        if (!branches.containsKey(mergedBranch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (mergedBranch.equals(getCurrentBranch())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        String currBranch = getCurrentBranch();

        String splitHash = getSplitCommit
                (mergedBranch, currBranch).substring(0, 7);

        File splitFile = Utils.join(commits, "/" + splitHash);
        Commit splitCommit = Utils.readObject(splitFile, Commit.class);

        Commit cbHead = Utils.readObject(Utils.join(commits, "/"
                + branches.get(currBranch).substring(0, 7)), Commit.class);
        Commit mbHead = Utils.readObject(Utils.join(commits, "/"
                + branches.get(mergedBranch).substring(0, 7)), Commit.class);

        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);

        if (splitCommit.getHash().equals(mbHead.getHash())) {
            String s = "Given branch is an ancestor";
            s += " of the current branch.";
            System.out.println(s);
            return;
        } else if (splitCommit.getHash().equals(cbHead.getHash())) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(mergedBranch);
            return;
        }

        for (String fileName: cwdFiles) {

            if (!commitTracks(cbHead, fileName)) {
                String s = "";
                s = "There is an untracked file in the way;";
                s += " delete it, or add and commit it first.";
                System.out.println(s);
                return;
            }
        }

        HashSet<String> uniqueFiles = new HashSet<String>();
        uniqueFiles.addAll(splitCommit.getCommitData().keySet());
        uniqueFiles.addAll(cbHead.getCommitData().keySet());
        uniqueFiles.addAll(mbHead.getCommitData().keySet());

        Iterator<String> fileItr = uniqueFiles.iterator();
        String currentFile = "";
        String [] stateArr = new String[3];

        String fileHash;
        File dataFile;

        while (fileItr.hasNext()) {
            currentFile = fileItr.next();
            stateArr[0] = getHash(splitCommit, currentFile);
            stateArr[1] = getHash(cbHead, currentFile);
            stateArr[2] = getHash(mbHead, currentFile);
            String result = evaluateState(stateArr);

            if (result.equals("merge conflict")) {
                String cbFileContents = "";
                String mbFileContents = "";

                if (!stateArr[1].equals("dne")) {
                    cbFileContents = Utils.readContentsAsString(
                            Utils.join(data, "/" + stateArr[1]));
                }

                if (!stateArr[2].equals("dne")) {
                    mbFileContents = Utils.readContentsAsString(
                            Utils.join(data, "/" + stateArr[2]));
                }

                try {
                    FileWriter fw = new FileWriter(
                            Utils.join(CWD, "/" + currentFile));
                    fw.write("<<<<<<< HEAD" + "\n" + cbFileContents
                            + "=======" + "\n" + mbFileContents + ">>>>>>>\n");
                    fw.close();

                } catch (IOException exp) {
                    System.out.println("IOException");
                    return;
                }
                System.out.println("Encountered a merge conflict.");
                add(Utils.join(CWD, "/" + currentFile));
            } else if (result.equals("stage for removal")) {
                removeFile(currentFile);
            } else if (result.equals("")) {
                continue;
            } else if (!stateArr[1].equals(result)) {
                if (result.equals("dne")) {
                    removeFile(currentFile);
                } else {
                    File f = Utils.join(data, "/" + result);
                    File fCWD = Utils.join(CWD, "/" + currentFile);

                    if (!f.exists()) {
                        add(fCWD);
                        return;
                    } else if (!f.isFile()) {
                        return;
                    }
                    byte [] fContents = Utils.readContents(f);
                    Utils.writeContents(fCWD, fContents);
                    add(fCWD);
                }
            }

        }
        String parentCommit = cbHead.getHash();
        HashMap<String, String> commitMap = new HashMap<String, String>();
        Date date = new Date();
        String message = "Merged " + mergedBranch
                + " into " + getCurrentBranch() + ".";
        Commit mergeCommit = new Commit(message, date, parentCommit, commitMap);
        mergeCommit.setMergedParent(mbHead.getHash());
        commit(mergeCommit);
    }

    private String getHash(Commit commit, String fileName) {

        if (!commit.getCommitData().containsKey(fileName)) {
            return "dne";
        }

        return commit.getCommitData().get(fileName);
    }

    private String evaluateState(String [] stateArr) {

        if (case1(stateArr)) {
            return stateArr[2];
        } else if (case2(stateArr)) {
            return stateArr[1];
        } else if (case3(stateArr)) {
            return "";
        } else if (case4(stateArr)) {
            return "merge conflict";
        } else if (case5(stateArr)) {
            return stateArr[1];
        } else if (case6(stateArr)) {
            return stateArr[2];
        } else if (case7(stateArr)) {
            return "stage for removal";
        } else if (case8(stateArr)) {
            return "";
        }
        return "";

    }

    /* keep contents as they are in the merged branch */
    private boolean case1(String [] stateArr) {
        return stateArr[0].equals(stateArr[1])
                &&
                !stateArr[0].equals(stateArr[2]);
    }

    /* keep contents as they are in the current branch commit */
    private boolean case2(String [] stateArr) {
        return stateArr[0].equals(stateArr[2])
                &&
                !stateArr[0].equals(stateArr[1]);
    }

    /* do nothing case */
    private boolean case3(String [] stateArr) {
        return !stateArr[0].equals(stateArr[1])
                &&
                !stateArr[0].equals(stateArr[2])
                &&
                stateArr[1].equals(stateArr[2]);
    }

    /*Merge Conflict case */
    private boolean case4(String [] stateArr) {
        return !stateArr[0].equals(stateArr[1])
                &&
                !stateArr[0].equals(stateArr[2])
                &&
                !stateArr[1].equals(stateArr[2]);

    }

    /*keep contents as they are in the current branch */
    private boolean case5(String [] stateArr) {
        return stateArr[0].equals("dne")
                &&
                !stateArr[1].equals("dne")
                &&
                stateArr[2].equals("dne");
    }

    /*keep contents as they are in the merged branch */
    private boolean case6(String [] stateArr) {
        return stateArr[0].equals("dne")
                &&
                stateArr[1].equals("dne")
                &&
                !stateArr[2].equals("dne");
    }

    /* stage file for removal */
    private boolean case7(String [] stateArr) {
        return stateArr[0].equals(stateArr[1]) && stateArr[2].equals("dne");
    }

    /* file remains removed */
    private boolean case8(String [] stateArr) {
        return stateArr[0].equals(stateArr[2]) && stateArr[1].equals("dne");
    }






}
