package naming;

import java.util.*;

public class DirectoryTree {
    DirectoryNode root;
    public DirectoryTree() {
        this.root = new DirectoryNode(new Path(), true);
    }

    public DirectoryNode getNode(Path p) {
        DirectoryNode curNode = this.root;
        while(true) {
            Path curPath = curNode.root;
            if(curNode.root.equals(p)) {
                return curNode.root;
            }
            else {
                if(p.isSubpath(curPath)) {
                    curNode = curNode.sons.get(p);
                }
                else {
                    break;
                }
            }
        }
        return null;
    }

    /** Creates the given file, if it does not exist.

        <p>
        This only takes charge of creating a node without path components.
        @throws FileNotFoundException If the parent directory does not exist.
     */
    public boolean createNode(Path file, boolean isDirectory) throws FileNotFoundException {
        // TODO: if the directory does not exist, the directories need to be created.
        Path pnt = file.parent();
        DirectoryNode dirNode = getNode(pnt);
        if(dirNode == null) {
            throw new FileNotFoundException();
        }

        return dirNode.insertNode(file, isDirectory);
    }

    /** Insert path component into a Path.

        <p>
        Take charge of insert path component into the Path, the caller need to call scheduler to
        get a storage server for operating.
        @throws FileNotFoundException If the path does not exist.
     */
    public void insertPathComp(Path p, PathComponents pathComp) throws FileNotFoundException {
        DirectoryNode node = getNode(p);
        if(node == null) {
            throw new FileNotFoundException();
        }

        node.insertPathComp(pathComp);
    }

    public boolean insertPathStubs(Path file, Storage storage, Command command)
            throws FileNotFoundException {
        createNode(file, false);
        // TODO
        DirectoryNode node = getNode(p);
        if(node == null) {
            throw new FileNotFoundException();
        }
        node.pathComps.addStorageStub(storage);
        node.pathComps.addCommandStub(command);
    }

    /** Delete the node of Path p.

        <p>
        If the Path is not reachable, throw FileNotFoundException.
        If the Path is root, return false.
      */
    public boolean deleteNode(Path p) throws FileNotFoundException {
        if(p.isRoot()) {
            return false;
        }

        Path pnt = p.parent();
        DirectoryNode dirNode = getNode(pnt);
        if(dirNode == null) {
            throw new FileNotFoundException();
        }

        dirNode.sons.remove(p)
        return true;
    }

    /** DirectoryTree.

        <p>
      */
    public class DirectoryNode {
        Path root;
        HashMap<Path, DirectoryNode> sons;
        boolean isDirectory;
        ArrayList<PathComponents> pathComps; // TODO: Need a mapping

        public DirectoryNode(Path p, boolean isDirectory) {
            this.root = p;
            this.isDirectory = isDirectory;
            if(this.isDirectory) {
                this.sons = new HashMap<Path, DirectoryNode>();
            }
        }

        public boolean isDirectory() {
            return this.isDirectory;
        }

        public HashMap<Path, DirectoryNode> getSons() {
            return this.sons;
        }

        public String getPath() {
            return root.toString();
        }

        public boolean insertNode(Path p, boolean isDirectory) {
            if(this.sons.containsKey(p)) {
                return false;
            }
            else {
                this.sons.put(p, new DirectoryNode(p, isDirectory));
                return true;
            }
        }

        public void insertPathComp(PathComponents pathComp) {
            this.pathComps.addStorageStub(pathComp.getStorageStub());
            this.pathComps.addCommandStub(pathComp.getCommandStub());
        }
    }

    /** Helper class for NamingServer.

        <p>
        There are three components for a mapping from path:
        Storage
        Command
        Path[]
      */
    public class PathComponents {
        ArrayList<Storage> storageStubs = null;
        ArrayList<Command> commandStubs = null;

        public PathComponents() {
            this.storageStubs = new ArrayList<Storage>();
            this.commandStubs = new ArrayList<Command>();
        }

        public ArrayList<Storage> getStorageStub() {
            return this.storageStubs;
        }

        public ArrayList<Command> getCommandStub() {
            return this.commandStubs;
        }

        public void addStorageStub(Storage storage) {
            this.storageStubs.add(storage);
        }

        public void addCommandStub(Command command) {
            this.commandStubs.add(command);
        }
        public void addStorageStub(ArrayList<Storage> storage) {
            this.storageStubs.addAll(storage);
        }

        public void addCommandStub(ArrayList<Command> command) {
            this.commandStubs.addAll(command);
        }
    }
}
