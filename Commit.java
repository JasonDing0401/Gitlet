package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/** The Gitlet Commit class.
 * @author Jason Ding
 */
public class Commit implements Serializable {

    /** Timestamp of the commit. */
    private String _time;

    /** The commit message. */
    private String _message;

    /** The parent of the commit. */
    private String _parent;

    /** Parent 1 of the merged commit. */
    private String _parent1;

    /** Parent 2 of the merged commit. */
    private String _parent2;

    /** HashMap that points to all blobs.*/
    private HashMap<String, String> _blobs;

    /** The SHA1 of the commit. */
    private String _sha1;

    /** Boolean of whether the commit is merged. */
    private boolean _isMerged;

    /** Constructor of the Commit class. Take in MESSAGE and PARENT. */
    Commit(String message, String parent) throws IOException {
        _message = message;
        _parent = parent;
        _isMerged = false;
        SimpleDateFormat formatter
                = new SimpleDateFormat("EEE LLL d HH:mm:ss y Z");
        if (parent == null) {
            _time = formatter.format(new Date(0));
            _blobs = new HashMap<>();
        } else {
            _time = formatter.format(new Date(System.currentTimeMillis()));
            Commit c = fromFile(_parent);
            _blobs = c.getBlobs();
        }
        _sha1 = "c" + Utils.sha1(Utils.serialize(this));
        update();
    }

    /** Constructor of the merged Commit class. Take in MESSAGE,
     * PARENT1, and PARENT2.
     */
    Commit(String message, String parent1, String parent2) throws IOException {
        _message = message;
        _parent1 = parent1;
        _parent2 = parent2;
        _parent = _parent1 + _parent2;
        _isMerged = true;
        SimpleDateFormat formatter
                = new SimpleDateFormat("EEE LLL d HH:mm:ss y Z");
        _time = formatter.format(new Date(System.currentTimeMillis()));
        Commit c1 = fromFile(_parent1);
        _blobs = c1.getBlobs();
        _sha1 = "c" + Utils.sha1(Utils.serialize(this));
        update();
    }

    /** Commit the files from the staging area of the branch.
     * Take in the STAGE.
     */
    public void commit(Stage stage) throws IOException {
        Stage s = Utils.readObject(stage.getFile(),
                Stage.class);
        _blobs.putAll(s.getCurrStage());
        for (String str : s.getRemoveStage().keySet()) {
            _blobs.remove(str);
        }
        update();
    }

    /** Save or update the commit object. */
    public void update() throws IOException {
        File f = Utils.join(Main.OBJECTS, getSha1());
        f.createNewFile();
        Utils.writeObject(f, this);
    }

    /** Return whether the commit is merged. */
    public boolean isMerged() {
        return _isMerged;
    }

    /** Return the parent. */
    public String getParent() {
        return _parent;
    }

    /** Return parent1 of the merged commit. */
    public String getParent1() {
        return _parent1;
    }

    /** Return parent2 of the merged commit. */
    public String getParent2() {
        return _parent2;
    }

    /** Return time. */
    public String getTime() {
        return _time;
    }

    /** Return message. */
    public String getMessage() {
        return _message;
    }

    /** Return blobs of the commit. */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** Return its sha1 representation. */
    public String getSha1() {
        return _sha1;
    }

    /** Reads in and deserializes a commit from a file according
     * to its SHA1 code. Return the commit.
     */
    public static Commit fromFile(String sha1) {
        File commitFile = Utils.join(Main.OBJECTS, sha1);
        if (!commitFile.exists()) {
            throw new IllegalArgumentException(
                    "No file of commit with that name found.");
        }
        return Utils.readObject(commitFile, Commit.class);
    }

}
