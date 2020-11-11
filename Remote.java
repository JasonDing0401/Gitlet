package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/** The remote class of the Gitlet.
 * @author Jason Ding
 */

public class Remote implements Serializable {

    /** Gitlet current working directory for remote. */
    private File _cwd;
    /** GitLet repo directory for remote. */
    private File _repo;
    /** GitLet object directory for remote. */
    private File _objects;
    /** GitLet branch directory for remote. */
    private File _branch;
    /** GitLet current branch directory for remote. */
    private File _currBranch;
    /** GitLet staging area directory for remote. */
    private File _stagingArea;

    /** The name of the remote. */
    private String _name;

    /** Constructor of remote with NAME and DIRECTORY. */
    public Remote(String name, String directory) throws IOException {
        _name = name;
        _repo = new File(directory);
        _cwd = Utils.join(_repo, "..");
        _objects = Utils.join(_repo, "objects");
        _branch = Utils.join(_repo, "branch");
        _stagingArea = Utils.join(_repo, "stage");
        _currBranch = Utils.join(_repo, "current-branch");
        saveRemote();
    }

    /** Return the name of the remote. */
    public String getName() {
        return _name;
    }

    /** Return the cwd of the remote. */
    public File getCwd() {
        return _cwd;
    }

    /** Return the repo of the remote. */
    public File getRepo() {
        return _repo;
    }

    /** Return the branch of the remote. */
    public File getBranch() {
        return _branch;
    }

    /** Return the object of the remote. */
    public File getObject() {
        return _objects;
    }

    /** Save the remote into a File. */
    public void saveRemote() throws IOException {
        File f = Utils.join(Main.REMOTE, _name);
        f.createNewFile();
        Utils.writeObject(f, this);
    }

    /** Reads in a blob from a file according
     * to its SHA1. Return the string content.
     */
    public String blobFromFile(String sha1) {
        File blobFile = Utils.join(_objects, sha1);
        if (!blobFile.exists()) {
            throw new IllegalArgumentException(
                    "No blob file with that name found.");
        }
        return Utils.readContentsAsString(blobFile);
    }

    /** Reads in a commit from a file according
     * to its SHA1. Return the COMMIT.
     */
    public Commit commitFromFile(String sha1) {
        File commitFile = Utils.join(_objects, sha1);
        if (!commitFile.exists()) {
            throw new IllegalArgumentException(
                    "No commit file with that name found.");
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Reads in a branch from a file according to
     * its NAME. Return the BRANCH.
     */
    public Branch branchFromFile(String name) {
        File branchFile = Utils.join(_branch, name);
        if (!branchFile.exists()) {
            throw new IllegalArgumentException(
                    "No branch file with that name found.");
        }
        return Utils.readObject(branchFile, Branch.class);
    }

    /** Reads in a stage from a file according to
     * its BRANCH. Return the STAGE.
     */
    public Stage stageFromFile(String branch) {
        File stageFile = Utils.join(_stagingArea, branch);
        if (!stageFile.exists()) {
            throw new IllegalArgumentException(
                    "No stage file with that branch found.");
        }
        return Utils.readObject(stageFile, Stage.class);
    }

    /** Reads in a remote from a file according to its NAME.
     * Return the REMOTE.
     */
    public static Remote fromFile(String name) {
        if (!Utils.join(Main.REMOTE, name).exists()) {
            throw new IllegalArgumentException(
                    "No remote file with that name found.");
        }
        return Utils.readObject(Utils.join(Main.REMOTE, name), Remote.class);
    }

}
