package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/** The Gitlet Blob class.
 * @author Jason Ding
 */
public class Blob implements Serializable {

    /** The String content of the blob. */
    private String _blob;

    /** The SHA1 name of the blob. */
    private String _name;

    /** The constructor of the class.
     * Take in FILE and store it as a blob.
     */
    Blob(File file) throws IOException {
        _blob = Utils.readContentsAsString(file);
        _name = "b" + Utils.sha1(_blob);
        saveBlob();
    }

    /** Get the blob and return the string representation. */
    public String getBlob() {
        return _blob;
    }

    /** Get the name and return the string representation. */
    public String getName() {
        return _name;
    }

    /** Return Whether two blobs are different according to the sha1.
     * Take in BLOB.
     */
    public Boolean isDiff(String blob) {
        if (blob == null) {
            return true;
        }
        return !this._name.equals(blob);
    }

    /** Save blob in the Object directory. */
    public void saveBlob() throws IOException {
        File f = Utils.join(Main.OBJECTS, _name);
        f.createNewFile();
        Utils.writeContents(f, _blob);
    }

    /** Reads in a blob from a file according to its SHA1.
     * Return the STRING content.
     */
    public static String fromFile(String sha1) {
        File blobFile = Utils.join(Main.OBJECTS, sha1);
        if (!blobFile.exists()) {
            throw new IllegalArgumentException(
                    "No blob file with that name found.");
        }
        return Utils.readContentsAsString(blobFile);
    }

    /** Reads in a blob from a file according to its SHA1. Return the BLOB. */
    public static Blob blobFromFile(String sha1) {
        File blobFile = Utils.join(Main.OBJECTS, sha1);
        if (!blobFile.exists()) {
            throw new IllegalArgumentException(
                    "No blob file with that name found.");
        }
        return Utils.readObject(blobFile, Blob.class);
    }

}
