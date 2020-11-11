package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

/** The Gitlet Branch class.
 * @author Jason Ding
 */
public class Branch implements Serializable {

    /** The name of the branch. */
    private String _name;
    /** The head commit of the branch. */
    private String _head;
    /** The LinkedList representation of commits in the branch. */
    private LinkedList<String> _commits = new LinkedList<>();

    /** The constructor of the class.
     * Take in String NAME and Commit HEAD.
     */
    Branch(String name, String head) {
        _name = name;
        _head = head;
        _commits.add(head);
    }

    /** Get the commits and return LinkedList of commits. */
    public LinkedList<String> getCommits() {
        return _commits;
    }

    /** Get the head. and return the head commit. */
    public String getHead() {
        return _head;
    }

    /** Get the name. and return the name of the branch. */
    public String getName() {
        return _name;
    }

    /** Set the head of the branch to COMMIT. */
    public void setHead(String commit) {
        _head = commit;
    }

    /** Change the name of the branch to NAME. */
    public void changeName(String name) {
        _name = name;
    }

    /** Change the commits of the branch. Take in LST. */
    public void changeCommits(LinkedList<String> lst) {
        _commits = lst;
    }

    /** Change the current head of the branch according to the HEAD. */
    public void changeHead(String head) {
        _head = head;
        _commits.add(head);
        File f = Utils.join(Main.BRANCH, _name);
        Utils.writeObject(f, this);
    }

    /** Save the branch to a file in the BRANCH directory. */
    public void saveBranch() throws IOException {
        File f = Utils.join(Main.BRANCH, _name);
        f.createNewFile();
        Utils.writeObject(f, this);
    }

    /** Reads the branch according to its NAME.
     * Return the BRANCH with corresponding name. */
    public static Branch fromFile(String name) {
        if (name == null) {
            if (!Main.CURRENT_BRANCH.exists()) {
                throw new IllegalArgumentException(
                        "No current branch file found.");
            }
            String currentBranch = Utils.readContentsAsString(
                    Main.CURRENT_BRANCH);
            return fromFile(currentBranch);
        } else {
            if (!Utils.join(Main.BRANCH, name).exists()) {
                throw new IllegalArgumentException(
                        "No branch file with that name found.");
            }
            return Utils.readObject(Utils.join(Main.
                    BRANCH, name), Branch.class);
        }
    }

}
