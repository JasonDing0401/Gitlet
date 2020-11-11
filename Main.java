package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Jason Ding
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** GitLet repo directory. */
    static final File REPO = Utils.join(CWD, ".gitlet");
    /** GitLet object directory. */
    static final File OBJECTS = Utils.join(REPO, "objects");
    /** GitLet current branch directory. */
    static final File BRANCH = Utils.join(REPO, "branch");
    /** GitLet current branch file. */
    static final File CURRENT_BRANCH = Utils.join(REPO, "current-branch");
    /** GitLet staging area directory. */
    static final File STAGE = Utils.join(REPO, "stage");
    /** GitLet commit history file. */
    static final File LOGS = Utils.join(REPO, "logs");
    /** GitLet remote directory. */
    static final File REMOTE = Utils.join(REPO, "remote");

    /** The staging area of the Gitlet. */
    private static Stage _stagingArea;
    /** The branch of the Gitlet. */
    private static Branch _branch;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            error("Please enter a command.");
        }
        if (args[0].equals("init")) {
            init(args);
        } else {
            if (!REPO.exists()) {
                error("Not in an initialized Gitlet directory.");
            } else {
                _branch = Branch.fromFile(null);
                _stagingArea = Stage.fromFile(_branch.getName());
                helperMethod(args);
            }
        }
        System.exit(0);
    }

    /** Creates a new Gitlet version-control system in the current directory.
     * @param args Array in format: {'init'}
     */
    public static void init(String[] args) throws IOException {
        if (REPO.exists()) {
            error("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
        REPO.mkdir();
        OBJECTS.mkdir();
        STAGE.mkdir();
        REMOTE.mkdir();
        LOGS.mkdir();
        BRANCH.mkdir();
        Commit initial = new Commit("initial commit", null);
        initial.update();
        _stagingArea = new Stage("master");
        Branch b = new Branch("master", initial.getSha1());
        b.saveBranch();
        _branch = Branch.fromFile("master");
        CURRENT_BRANCH.createNewFile();
        Utils.writeContents(CURRENT_BRANCH, "master");
    }

    /** Helper Method that uses switch method to deal with input
     * ARGS from main.
     */
    public static void helperMethod(String... args) throws IOException {
        switch (args[0]) {
        case "add":
            add(args);
            break;
        case "commit":
            commit(args);
            break;
        case "rm":
            rm(args);
            break;
        case "log":
            log(args);
            break;
        case "global-log":
            globalLog(args);
            break;
        case "find":
            find(args);
            break;
        case "status":
            status(args);
            break;
        case "checkout":
            checkout(args);
            break;
        case "branch":
            branch(args);
            break;
        case "rm-branch":
            rmBranch(args);
            break;
        case "reset":
            reset(args);
            break;
        case "merge":
            merge(args);
            break;
        case "add-remote":
            addRemote(args);
            break;
        case "rm-remote":
            rmRemote(args);
            break;
        case "push":
            push(args);
            break;
        case "fetch":
            fetch(args);
            break;
        case "pull":
            pull(args);
            break;
        default:
            error("No command with that name exists.");
        }
    }

    /** Adds a copy of the file as it currently exists to the staging area.
     * @param args Array in format: {'add', fileName}
     */
    public static void add(String... args) throws IOException {
        File f = Utils.join(CWD, args[1]);
        if (!f.exists()) {
            error("File does not exist.");
        }
        Blob b = new Blob(f);
        if (_stagingArea.getCurrStage().containsKey(args[1])) {
            if (b.isDiff(_stagingArea.getCurrStage().get(args[1]))) {
                _stagingArea.remove(args[1]);
                _stagingArea.put(args[1], b.getName());
            }
            if (!b.isDiff(Commit.fromFile(_branch.getHead()).
                    getBlobs().get(args[1]))) {
                _stagingArea.remove(args[1]);
            }
        } else {
            if (b.isDiff(Commit.fromFile(_branch.getHead()).
                    getBlobs().get(args[1]))) {
                _stagingArea.put(args[1], b.getName());
            } else {
                if (_stagingArea.getRemoveStage().containsKey(args[1])) {
                    _stagingArea.getRemoveStage().remove(args[1]);
                    _stagingArea.update();
                }
            }
        }

    }

    /** Saves a snapshot of certain files in the current commit and staging
     * area so they can be restored at a later time, creating a new commit.
     * @param args Array in format: {'commit', message}
     */
    public static void commit(String[] args) throws IOException {
        if (_stagingArea.getCurrStage().isEmpty()
                && _stagingArea.getRemoveStage().isEmpty()) {
            error("No changes added to the commit.");
        }
        if (args.length == 1 || (args.length == 2
                && args[1].trim().isEmpty())) {
            error("Please enter a commit message.");
        }
        Commit head = Commit.fromFile(_branch.getHead());
        Commit c = new Commit(args[1], head.getSha1());
        c.commit(_stagingArea);
        _branch.changeHead(c.getSha1());
        _stagingArea.getCurrStage().clear();
        _stagingArea.getRemoveStage().clear();
        _stagingArea.update();
    }

    /** Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for
     * removal and remove the file from the working directory if
     * the user has not already done so.
     * @param args Array in format: {'rm', fileName}
     */
    public static void rm(String[] args) {
        boolean removed = false;
        if (!_stagingArea.getCurrStage().isEmpty()) {
            if (_stagingArea.getCurrStage().containsKey(args[1])) {
                removed = true;
                String b = _stagingArea.getCurrStage().get(args[1]);
                _stagingArea.remove(args[1]);
            }
        }
        if (Commit.fromFile(_branch.getHead()).
                getBlobs().containsKey(args[1])) {
            removed = true;
            String b = Commit.fromFile(_branch.getHead()).
                    getBlobs().get(args[1]);
            _stagingArea.toRemove(args[1], b);
            Utils.restrictedDelete(Utils.join(CWD, args[1]));
        }
        if (!removed) {
            error("No reason to remove the file.");
        }
    }

    /** Starting at the current head commit, display information about
     * each commit backwards along the commit tree until the initial commit,
     * following the first parent commit links, ignoring any second parents
     * found in merge commits.
     * @param args Array in format: {'log'}
     */
    public static void log(String[] args) {
        Commit c = Commit.fromFile(_branch.getHead());
        while (c != null) {
            System.out.println("===\ncommit " + c.getSha1());
            if (c.isMerged()) {
                System.out.println("Merge: " + c.getParent1().substring(0, 7)
                        + " " + c.getParent2().substring(0, 7));
            }
            System.out.println("Date: " + c.getTime()
                    + "\n" + c.getMessage());
            System.out.println();
            if (c.getParent() == null) {
                break;
            }
            if (c.isMerged()) {
                c = Commit.fromFile(c.getParent1());
            } else {
                c = Commit.fromFile(c.getParent());
            }
        }

    }

    /** Like log, except displays information about all commits ever made.
     * The order of the commits does not matter.
     * @param args Array in format: {'global-log'}
     */
    public static void globalLog(String[] args) {
        for (String c : OBJECTS.list()) {
            if (!c.startsWith("c")) {
                continue;
            }
            Commit com = Commit.fromFile(c);
            System.out.println("===\ncommit " + com.getSha1());
            if (com.isMerged()) {
                System.out.println("Merge: " + com.getParent1().substring(0, 7)
                        + " " + com.getParent2().substring(0, 7));
            }
            System.out.println("Date: " + com.getTime()
                    + "\n" + com.getMessage());
            System.out.println();
        }
    }

    /** Prints out the ids of all commits that have the
     * given commit message, one per line.
     * @param args Array in format: {'find', commitMessage}
     */
    public static void find(String[] args) {
        boolean found = false;
        for (String c : OBJECTS.list()) {
            if (!c.startsWith("c")) {
                continue;
            }
            Commit com = Commit.fromFile(c);
            if (com.getMessage().equals(args[1])) {
                System.out.println(com.getSha1());
                found = true;
            }
        }
        if (!found) {
            error("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist, and marks the current branch
     * with a *. Also displays what files have been staged for addition or
     * removal.
     * @param args Array in format: {'status'}
     */
    public static void status(String[] args) {
        System.out.println("=== Branches ===");
        ArrayList<String> arrBranch = new ArrayList<>();
        for (String b : BRANCH.list()) {
            if (b.equals(_branch.getName())) {
                System.out.println("*" + b);
            } else {
                arrBranch.add(b);
            }
        }
        Collections.sort(arrBranch);
        for (String b : arrBranch) {
            System.out.println(b);
        }
        System.out.println("\n=== Staged Files ===");
        for (String f : _stagingArea.getCurrStage().keySet()) {
            System.out.println(f);
        }
        System.out.println("\n=== Removed Files ===");
        for (String f : _stagingArea.getRemoveStage().keySet()) {
            System.out.println(f);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        Commit curr = Commit.fromFile(_branch.getHead());
        for (String f : curr.getBlobs().keySet()) {
            if (Utils.join(CWD, f).exists()) {
                if (!Blob.fromFile(curr.getBlobs().get(f)).
                        equals(Utils.readContentsAsString(Utils.join(CWD, f)))
                        && !_stagingArea.getCurrStage().containsKey(f)) {
                    System.out.println(f + " (modified)");
                }
            }
            if (!Utils.join(CWD, f).exists()) {
                if (!_stagingArea.getRemoveStage().containsKey(f)) {
                    System.out.println(f + " (deleted)");
                }
            }
        }
        for (String f : _stagingArea.getCurrStage().keySet()) {
            if (!Blob.fromFile(_stagingArea.getCurrStage().get(f)).
                    equals(Utils.readContentsAsString(Utils.join(CWD, f)))) {
                if (Utils.join(CWD, f).exists()) {
                    System.out.println(f + " (modified)");
                }
            }
            if (!Utils.join(CWD, f).exists()) {
                System.out.println(f + " (deleted)");
            }
        }
        System.out.println("\n=== Untracked Files ===");
        for (String f : CWD.list()) {
            if (!f.startsWith(".") && !Utils.join(CWD, f).isDirectory()
                    && !f.endsWith(".iml") && !f.equals("Makefile")
                    && !curr.getBlobs().containsKey(f)
                    && !_stagingArea.getCurrStage().containsKey(f)) {
                System.out.println(f);
            }
        }
        System.out.println();
    }



    /** Checkout is a kind of general command that can do a few different things
     *  depending on what its arguments are. There are 3 possible use cases.
     * @param args Array in three possible formats:
     *      {'checkout', '--', fileName}
     *      {'checkout', commitId, '--', fileName}
     *      {'checkout', branchName}
     */
    public static void checkout(String[] args) throws IOException {
        if (args.length == 2) {
            checkout2(args);
        } else if (args.length == 3) {
            checkout3(args);
        } else {
            checkout4(args);
        }
    }

    /** Helper method for checkout with 3 args.
     * @param args Array in format:
     *             {'checkout', '--', fileName}
     */
    public static void checkout3(String[] args) throws IOException {
        if (!args[1].equals("--")) {
            error("Incorrect operands.");
        }
        Commit c = Commit.fromFile(_branch.getHead());
        String content = null;
        boolean contained = false;
        if (c.getBlobs().containsKey(args[2])) {
            contained = true;
            content = Blob.fromFile(c.getBlobs().get(args[2]));
        }
        if (!contained) {
            error("File does not exist in that commit.");
        }
        File f = Utils.join(CWD, args[2]);
        f.createNewFile();
        Utils.writeContents(f, content);

    }

    /** Helper method for checkout with 4 args.
     * @param args Array in format:
     *             {'checkout', commitId, '--', fileName}
     */
    public static void checkout4(String[] args) throws IOException {
        if (!args[2].equals("--")) {
            error("Incorrect operands.");
        }
        boolean found = false;
        Commit commit = null;
        for (String c : OBJECTS.list()) {
            if (c.startsWith(args[1])) {
                found = true;
                commit = Commit.fromFile(c);
                break;
            }
        }
        if (!found) {
            error("No commit with that id exists.");
        }
        if (!commit.getBlobs().containsKey(args[3])) {
            error("File does not exist in that commit.");
        }
        String content = Blob.fromFile(commit.getBlobs().get(args[3]));
        File f = Utils.join(CWD, args[3]);
        f.createNewFile();
        Utils.writeContents(f, content);
    }

    /** Helper method for checkout with 3 args.
     * @param args Array in format:
     *             {'checkout', branchName}
     */
    public static void checkout2(String[] args) throws IOException {
        String remoteBr = args[1].replace("/", "-");
        if (!Arrays.asList(BRANCH.list()).contains(remoteBr)) {
            error("No such branch exists.");
        } else if (_branch.getName().equals(remoteBr)) {
            error("No need to checkout the current branch.");
        } else {
            for (String f : CWD.list()) {
                if (!f.startsWith(".") && !Commit.fromFile(_branch.getHead()).
                        getBlobs().containsKey(f)
                        && Commit.fromFile(Branch.fromFile(remoteBr).
                                getHead()).getBlobs().containsKey(f)
                        && !Blob.fromFile(Commit.fromFile(Branch.fromFile(
                                remoteBr).getHead()).getBlobs().get(f)).equals(
                                        Utils.readContentsAsString(
                                                Utils.join(CWD, f)))) {
                    error("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
            HashMap<String, String> commits = Commit.fromFile(Branch.
                    fromFile(remoteBr).getHead()).getBlobs();
            for (String f : commits.keySet()) {
                Utils.join(CWD, f).createNewFile();
                Utils.writeContents(Utils.join(CWD, f),
                        Blob.fromFile(commits.get(f)));
            }
            HashMap<String, String> prev = Commit.
                    fromFile(_branch.getHead()).getBlobs();
            for (String f : prev.keySet()) {
                if (!commits.containsKey(f)) {
                    Utils.join(CWD, f).delete();
                }
            }
            _branch = Branch.fromFile(remoteBr);
            Utils.writeContents(CURRENT_BRANCH, _branch.getName());
        }
        _stagingArea.getCurrStage().clear();
        _stagingArea.update();
        Stage stageArea = new Stage(_branch.getName());
        stageArea.update();
    }

    /** Creates a new branch with the given name, and
     * points it at the current head node.
     * @param args Array in format: {'branch', branchName}
     */
    public static void branch(String[] args) throws IOException {
        if (Arrays.asList(BRANCH.list()).contains(args[1])) {
            error("A branch with that name already exists.");
        }
        Branch newBr = new Branch(args[1], _branch.getHead());
        newBr.changeCommits(_branch.getCommits());
        newBr.saveBranch();
    }

    /** Deletes the branch with the given name. This only means to
     * delete the pointer associated with the branch; it does not
     * mean to delete all commits that were created under the
     * branch, or anything like that.
     * @param args Array in format: {'rm-branch', branchName}
     */
    public static void rmBranch(String[] args) {
        if (_branch.getName().equals(args[1])) {
            error("Cannot remove the current branch.");
        }
        if (!Arrays.asList(BRANCH.list()).contains(args[1])) {
            error("A branch with that name does not exist.");
        }
        Utils.join(BRANCH, args[1]).delete();
        Utils.join(STAGE, args[1]).delete();

    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that
     * commit. Also moves the current branch's head to that
     * commit node.
     * @param args Array in format: {'reset', commitId}
     */
    public static void reset(String[] args) throws IOException {
        boolean found = false;
        Commit commit = null;
        for (String c : OBJECTS.list()) {
            if (c.startsWith(args[1])) {
                found = true;
                commit = Commit.fromFile(c);
                break;
            }
        }
        if (!found) {
            error("No commit with that id exists.");
        }
        for (String f : CWD.list()) {
            if (!f.startsWith(".")
                    && !Commit.fromFile(_branch.getHead()).
                    getBlobs().containsKey(f)
                    && commit.getBlobs().containsKey(f)
                    && !Blob.fromFile(commit.getBlobs().get(f)).
                    equals(Utils.readContentsAsString(Utils.join(CWD, f)))) {
                error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        for (File file : CWD.listFiles()) {
            if (!file.isHidden()
                    && !file.getName().equals("Makefile")
                    && !file.getName().endsWith(".iml")) {
                file.delete();
            }
        }
        HashMap<String, String> commits = commit.getBlobs();
        for (String f : commits.keySet()) {
            Utils.join(CWD, f).createNewFile();
            Utils.writeContents(Utils.join(CWD, f),
                    Blob.fromFile(commits.get(f)));
        }
        _stagingArea.getCurrStage().clear();
        _stagingArea.update();
        if (!_branch.getCommits().contains(commit.getSha1())) {
            _branch.changeHead(commit.getSha1());
        } else {
            int index = _branch.getCommits().indexOf(commit.getSha1());
            int j = _branch.getCommits().size();
            for (int i = index + 1; i < j; i++) {
                _branch.getCommits().remove(i);
            }
            _branch.setHead(commit.getSha1());
        }
        _branch.saveBranch();

    }

    /** Merges files from the given branch into the current branch.
     * @param args Array in format: {'merge', branchName}
     */
    public static void merge(String[] args) throws IOException {
        if (!_stagingArea.getRemoveStage().isEmpty()
                || !_stagingArea.getCurrStage().isEmpty()) {
            error("You have uncommitted changes.");
        }
        if (!Arrays.asList(BRANCH.list()).contains(args[1])) {
            error("A branch with that name does not exist.");
        }
        if (_branch.getName().equals(args[1])) {
            error("Cannot merge a branch with itself.");
        }
        Branch mergeBr = Branch.fromFile(args[1]);
        Commit mergeCom = Commit.fromFile(mergeBr.getHead());
        for (String f : mergeCom.getBlobs().keySet()) {
            if (!f.startsWith(".") && !Commit.fromFile(_branch.getHead()).
                    getBlobs().containsKey(f) && Utils.join(CWD, f).exists()
                    && !Blob.fromFile(mergeCom.getBlobs().get(f)).
                    equals(Utils.readContentsAsString(Utils.join(CWD, f)))) {
                error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        String splitPoint = findSplit(args[1]);
        if (mergeBr.getHead().equals(splitPoint)) {
            error("Given branch is an ancestor of the current branch.");
        }
        if (_branch.getHead().equals(splitPoint)) {
            checkout2(new String[]{"checkout", args[1]});
            error("Current branch fast-forwarded.");
        }
        HashMap<String, String> mergeFile =
                Commit.fromFile(mergeBr.getHead()).getBlobs();
        HashMap<String, String> splitFile =
                Commit.fromFile(splitPoint).getBlobs();
        HashMap<String, String> currFile =
                Commit.fromFile(_branch.getHead()).getBlobs();
        HashMap<String, String> allFile = new HashMap<>();
        allFile.putAll(mergeFile);
        allFile.putAll(splitFile);
        allFile.putAll(currFile);
        help(allFile, splitFile, mergeFile, currFile, splitPoint, mergeBr);
        String rmBranch = args[1].replace("-", "/");
        Commit mergedCom = new Commit("Merged " + rmBranch + " into "
                + _branch.getName() + ".",
                _branch.getHead(), mergeBr.getHead());
        mergedCom.commit(_stagingArea);
        _branch.changeHead(mergedCom.getSha1());
        _stagingArea.getRemoveStage().clear();
        _stagingArea.getCurrStage().clear();
        _stagingArea.update();
        for (File file : CWD.listFiles()) {
            if (mergedCom.getBlobs() != null && !mergedCom.
                    getBlobs().containsKey(file.getName())
                    && !file.isHidden() && !file.isDirectory()
                    && !file.getName().equals("Makefile")
                    && !file.getName().endsWith(".iml")) {
                file.delete();
            }
        }
    }

    /** Helper method for merge. Take in HashMap ALLFILE, SPLITFILE,
     * MERGEFILE, and CURRFILE, String SPLITPOINT, and Branch MERGEBR.
     */
    public static void help(HashMap<String, String> allFile,
                            HashMap<String, String> splitFile,
                            HashMap<String, String> mergeFile,
                            HashMap<String, String> currFile,
                            String splitPoint, Branch mergeBr
                            ) throws IOException {
        Boolean conflict = false;
        for (String f : allFile.keySet()) {
            if (splitFile.containsKey(f)) {
                if (mergeFile.containsKey(f) && currFile.containsKey(f)
                        && checkModified(f, splitPoint, mergeBr.getHead())
                        && !checkModified(f, splitPoint, _branch.getHead())) {
                    checkout4(new String[]{"checkout",
                            mergeBr.getHead(), "--", f});
                    Blob b = new Blob(Utils.join(CWD, f));
                    _stagingArea.put(f, b.getName());
                } else if (currFile.containsKey(f) && !checkModified(f,
                        splitPoint, _branch.getHead())
                        && !mergeFile.containsKey(f)) {
                    rm(new String[]{"rm", f});
                }
            } else {
                if (!currFile.containsKey(f) && mergeFile.containsKey(f)) {
                    checkout4(new String[]{"checkout",
                            mergeBr.getHead(), "--", f});
                    Blob b = new Blob(Utils.join(CWD, f));
                    _stagingArea.put(f, b.getName());
                }
            }
            if (checkConflict(f, _branch.getHead(), mergeBr.getHead(),
                    splitPoint, mergeFile, splitFile, currFile)) {
                conflict = true;
                String current = currFile.containsKey(f)
                        ? Blob.fromFile(currFile.get(f))
                        : "";
                String merge = mergeFile.containsKey(f)
                        ? Blob.fromFile(mergeFile.get(f))
                        : "";
                String content = "<<<<<<< HEAD\n"
                        + current + "=======\n"
                        + merge + ">>>>>>>\n";
                Utils.join(CWD, f).createNewFile();
                Utils.writeContents(Utils.join(CWD, f), content);
                Blob b = new Blob(Utils.join(CWD, f));
                _stagingArea.put(f, b.getName());
            }
        }
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Helper method for finding a split point.
     * Take in MERGEBRANCH. Return sha1 of the commit. */
    public static String findSplit(String mergeBranch) {
        HashMap<String, Integer> split = new HashMap<>();
        Branch mB = Branch.fromFile(mergeBranch);
        for (String com : mB.getCommits()) {
            if (Commit.fromFile(com).isMerged()) {
                if (_branch.getCommits().contains(
                        Commit.fromFile(com).getParent2())) {
                    split.put(com, 1);
                    continue;
                }
            }
            if (_branch.getCommits().contains(com)) {
                split.put(com, 0);
            }
        }
        for (String com : _branch.getCommits()) {
            if (Commit.fromFile(com).isMerged()) {
                if (mB.getCommits().contains(
                        Commit.fromFile(com).getParent2())) {
                    split.put(com, 1);
                    continue;
                }
            }
            if (mB.getCommits().contains(com)) {
                split.put(com, 0);
            }
        }
        HashMap<String, Integer> result = new HashMap<>();
        for (String com : split.keySet()) {
            Commit c = Commit.fromFile(com);
            int dist;
            if (c.isMerged()) {
                dist = calcDist(com, c, mB);
                result.put(c.getParent2(), dist);
            } else {
                dist = Math.min(_branch.getCommits().size() - 1
                                - _branch.getCommits().indexOf(com),
                        mB.getCommits().size() - 1
                                - mB.getCommits().indexOf(com));
                result.put(com, dist);
            }
        }
        int min = Collections.min(result.values());
        String splitPoint = "";
        for (String point : result.keySet()) {
            if (result.get(point) == min) {
                splitPoint = point;
                break;
            }
        }
        return splitPoint;
    }

    /** Helper method for calculating distance. Take in String COM,
     * Commit C, and Branch MB. Return the distance.
     */
    public static int calcDist(String com, Commit c, Branch mB) {
        int dist;
        if (_branch.getCommits().contains(com)) {
            dist = Math.min(_branch.getCommits().size()
                            - _branch.getCommits().indexOf(com),
                    mB.getCommits().size() - 1
                            - mB.getCommits().indexOf(c.getParent2()));
        } else {
            dist = Math.min(mB.getCommits().size()
                            - mB.getCommits().indexOf(com),
                    _branch.getCommits().size() - 1
                            - _branch.getCommits().
                            indexOf(c.getParent2()));
        }
        return dist;
    }

    /** Helper method for checking modified. Take in file F,
     * commit COM1, and commit COM2. Return whether it's modified.
     */
    public static boolean checkModified(String f, String com1, String com2) {
        String content1 = Blob.fromFile(
                Commit.fromFile(com1).getBlobs().get(f));
        String content2 = Blob.fromFile(
                Commit.fromFile(com2).getBlobs().get(f));
        return !content1.equals(content2);
    }

    /** Helper method for checking conflict. Take in file F,
     * commit CURRCOM, commit MERGECOM, commit SPLITCOM, HashMap M,
     * HashMap S, and HashMap C. Return whether there is conflict.
     */
    public static boolean checkConflict(String f, String currCom,
                                        String mergeCom, String splitCom,
                                        HashMap<String, String> m,
                                        HashMap<String, String> s,
                                        HashMap<String, String> c) {
        if (c.containsKey(f)
                && s.containsKey(f) && m.containsKey(f)) {
            if (checkModified(f, splitCom, currCom)
                    && checkModified(f, splitCom, mergeCom)
                    && checkModified(f, currCom, mergeCom)) {
                return true;
            }
        }
        if (c.containsKey(f)
                && s.containsKey(f) && !m.containsKey(f)) {
            if (checkModified(f, splitCom, currCom)) {
                return true;
            }
        }
        if (!c.containsKey(f)
                && s.containsKey(f) && m.containsKey(f)) {
            if (checkModified(f, splitCom, mergeCom)) {
                return true;
            }
        }
        if (c.containsKey(f)
                && !s.containsKey(f) && m.containsKey(f)) {
            if (checkModified(f, mergeCom, currCom)) {
                return true;
            }
        }
        return false;
    }

    /** Saves the given login information under the given remote name.
     * Attempts to push or pull from the given remote name will then
     * attempt to use this .gitlet directory.
     * @param args Array in format: {'add-remote', remoteName,
     *             remoteDirectoryName/.gitlet}
     */
    public static void addRemote(String[] args) throws IOException {
        String remoteName = args[1];
        String remoteDir = args[2];
        File f = Utils.join(REMOTE, remoteName);
        if (f.exists()) {
            error("A remote with that name already exists.");
        } else {
            new Remote(remoteName, remoteDir);
        }
    }

    /** Remove information associated with the given remote name.
     * The idea here is that if you ever wanted to change a remote
     * that you added, you would have to first remove it
     * and then re-add it.
     * @param args Array in format: {'rm-remote', remoteName}
     */
    public static void rmRemote(String[] args) {
        File f = Utils.join(REMOTE, args[1]);
        if (!f.exists()) {
            error("A remote with that name does not exist.");
        }
        f.delete();
    }

    /** Attempts to append the current branch's commits to the end
     * of the given branch at the given remote.
     * @param args Array in format: {'push', remoteName, remoteBranchName}
     */
    public static void push(String[] args) throws IOException {
        Remote remote = Remote.fromFile(args[1]);
        if (!remote.getRepo().exists()) {
            error("Remote directory not found.");
        }
        Commit com = Commit.fromFile(_branch.getHead());
        HashMap<String, String> rmFiles = com.getBlobs();
        Branch rmBranch;
        if (!Utils.join(remote.getBranch(), args[2]).exists()) {
            rmBranch = new Branch(args[2], _branch.getHead());
            rmBranch.changeCommits(_branch.getCommits());
            Utils.join(remote.getBranch(), args[2]).createNewFile();
        } else {
            rmBranch = remote.branchFromFile(args[2]);
            if (!_branch.getCommits().contains(rmBranch.getHead())) {
                error("Please pull down remote changes before pushing.");
            }
            int index = _branch.getCommits().indexOf(rmBranch.getHead());
            for (int i = index + 1; i < _branch.getCommits().size(); i++) {
                rmBranch.getCommits().add(_branch.getCommits().get(i));
            }
        }
        rmBranch.changeHead(_branch.getHead());
        Utils.writeObject(Utils.join(remote.getBranch(), args[2]), rmBranch);
        for (String c : rmBranch.getCommits()) {
            Utils.writeObject(Utils.join(remote.getObject(), c),
                    Commit.fromFile(c));
        }
        if (remote.getCwd().list() != null) {
            for (File f : remote.getCwd().listFiles()) {
                if (!f.isHidden()
                        && !f.getName().equals("Makefile")
                        && !f.getName().endsWith(".iml")) {
                    f.delete();
                }
            }
        }
        for (String name : rmFiles.keySet()) {
            Utils.join(remote.getCwd(), name).createNewFile();
            Utils.writeContents(Utils.join(
                    remote.getCwd(), name), rmFiles.get(name));
        }

    }

    /** Brings down commits from the remote Gitlet repository into
     * the local Gitlet repository.
     * @param args Array in format: {'fetch',
     *             remoteName, remoteBranchName}
     */
    public static void fetch(String[] args) throws IOException {
        Remote remote = Remote.fromFile(args[1]);
        if (!remote.getRepo().exists()) {
            error("Remote directory not found.");
        }
        if (!Utils.join(remote.getBranch(), args[2]).exists()) {
            error("That remote does not have that branch.");
        }
        Branch rmBranch = remote.branchFromFile(args[2]);
        rmBranch.changeName(args[1] + "-" + args[2]);
        rmBranch.saveBranch();
        for (String c : rmBranch.getCommits()) {
            Utils.join(OBJECTS, c).createNewFile();
            Utils.writeObject(Utils.join(OBJECTS, c), remote.commitFromFile(c));
        }
        for (String c : rmBranch.getCommits()) {
            for (String b : remote.commitFromFile(c).getBlobs().values()) {
                Utils.join(OBJECTS, b).createNewFile();
                Utils.writeObject(Utils.join(OBJECTS, b),
                        remote.blobFromFile(b));
            }
        }
        Stage rmStage = remote.stageFromFile(args[2]);
        Utils.join(STAGE, args[1] + "-" + args[2]).createNewFile();
        Utils.writeObject(Utils.join(STAGE, args[1] + "-" + args[2]), rmStage);
    }

    /** Fetches branch [remote name]/[remote branch name] as for the
     * fetch command, and then merges that fetch into the current branch.
     * @param args Array in format: {'pull', remoteName, remoteBranchName}
     */
    public static void pull(String[] args) throws IOException {
        fetch(new String[]{"fetch", args[1], args[2]});
        merge(new String[]{"merge", args[1] + "-" + args[2]});
    }

    /** Print out error MESSAGE and exit with code 0. */
    public static void error(String message) {
        Utils.message(message);
        System.exit(0);
    }

}
