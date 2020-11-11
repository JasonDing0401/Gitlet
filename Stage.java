package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/** The Gitlet Stage class.
 * @author Jason Ding
 */
public class Stage implements Serializable {

    /** HashMap that stores all files in the staging area. */
    private HashMap<String, String> _currStage;
    /** Stage for removing files. */
    private HashMap<String, String> _removeStage;
    /** The String branch of the staging area. */
    private String _branch;
    /** The file that stores the stage. */
    private File _file;

    /** The constructor of the class.
     * Take in BRANCH.
     */
    Stage(String branch) throws IOException {
        _currStage = new HashMap<>();
        _removeStage = new HashMap<>();
        _branch = branch;
        _file = Utils.join(Main.STAGE, _branch);
        _file.createNewFile();
        Utils.writeObject(_file, this);
    }

    /** Return the file of the stage. */
    public File getFile() {
        return _file;
    }

    /** Add files for removal. Take in commit's NAME and its BLOB. */
    public void toRemove(String name, String blob) {
        _removeStage.put(name, blob);
        update();
    }

    /** Put the file with BLOB representation in the HashMap
     * according to its NAME with.
     */
    public void put(String name, String blob) {
        _currStage.put(name, blob);
        update();
    }

    /** Remove the file from the HashMap according to its NAME. */
    public void remove(String name) {
        _currStage.remove(name);
        update();
    }

    /** Return the current HASHMAP.*/
    public HashMap<String, String> getCurrStage() {
        return _currStage;
    }

    /** Return the removal HASHMAP. */
    public HashMap<String, String> getRemoveStage() {
        return _removeStage;
    }

    /** Update the staging area in the file. */
    public void update() {
        Utils.writeObject(_file, this);
    }

    /** Reads in the Stage file according to its BRANCH. Return the STAGE. */
    public static Stage fromFile(String branch) {
        if (!Utils.join(Main.STAGE, branch).exists()) {
            throw new IllegalArgumentException(
                    "No stage of branch with this name found.");
        }
        return Utils.readObject(Utils.join(Main.STAGE, branch), Stage.class);
    }

}
