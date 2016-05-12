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
                if(curPath.isSubpath(p)) {
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
        Path pnt = file.parent();
        DirectoryNode dirNode = getNode(pnt);
        if(dirNode == null) {
            throw new FileNotFoundException();
        }

        dirNode.insertNode(file, isDirectory);
        return false
    }

    /** Insert path component into a Path.

        <p>
        Take charge of insert path component into the Path, the caller need to call scheduler to
        get a storage server for operating.
        @throws FileNotFoundException If the path does not exist.
     */
    public boolean insertPathComp(Path p, PathComponents pathComp) throws FileNotFoundException {
        DirectoryNode node = getNode(p);
        if(node == null) {
            throw new FileNotFoundException();
        }

        node.insertPathComp(pathComp);
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
            this.sons.put(p, new DirectoryNode(p, isDirectory));
        }

        public void insertPathComp(PathComponents pathComp) {
            this.pathComps.add(pathComp);
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
        ArrayList<Storag> client_stubs = null;
        ArrayList<Command> command_stubs = null;

        public PathComponents(Storage client_stub, Command command_stub) {
            this.client_stub = client_stub;
            this.command_stub  = command_stub;
        }
    }
}
