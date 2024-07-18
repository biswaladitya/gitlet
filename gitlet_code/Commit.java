package gitlet;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import java.time.LocalDateTime;
public class Commit implements Serializable{

    private String commitMessage;
    private String commitTime;
    private String parentCommit;
    private HashMap<String, String> commitData;
    private String hashId;
    private String mergedParent;


    public Commit(String commitMessage, Date commitTime, String parentCommit, HashMap<String, String> commitData) {
        this.commitMessage = commitMessage;

        if (commitTime != null) {
            DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
            this.commitTime = formatter.format(commitTime);
        }

        this.parentCommit = parentCommit;
        this.commitData = new HashMap<String, String>();

        if (!this.commitData.equals(null)) {
            this.commitData.putAll(commitData);
        }
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public void setParentCommit(String parentHash) {
        parentCommit = parentHash;
    }

    public void updateCommitData(String fileName, String fileHash) {
        commitData.put(fileName, fileHash);
    }

    public void removeData(String fileName) {
        commitData.remove(fileName);
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public String getCommitTime() {
        return commitTime;
    }
    public String getParentHash() {
        return parentCommit;
    }

    public HashMap<String, String> getCommitData() {
        return commitData;
    }

    public String getHash() {
        return hashId;
    }

    public String getMergedParent() {
        return mergedParent;
    }

    public void setMergedParent(String mergedParent) {
        this.mergedParent = mergedParent;
    }

    public void setCommitTime(String commitTime) {
        this.commitTime = commitTime;
    }

    public String [] getCommitDetails() {
        String line1 = "commit " + hashId;
        String line2 = "Date: " + commitTime;
        String [] output = {line1, line2, commitMessage};
        return output;
    }

}
