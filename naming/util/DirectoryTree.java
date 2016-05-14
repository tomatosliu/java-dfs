package naming.util;

import java.util.*;
import java.io.*;

import common.*;
import storage.*;

public class DirectoryTree {
    DirectoryNode root;
    public DirectoryTree() {
        this.root = new DirectoryNode(new Path(), true);
    }

    /** Get a directory node in the tree by Path p.

        @return a directory node, if there is a node of Path p, otherwise, return <code>null</code>.
      */
    public DirectoryNode getNode(Path p) {
        DirectoryNode curNode = this.root;
        while(true) {
            Path curPath = curNode.path;
            if(curPath.equals(p)) {
                return curNode;
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

    /** Insert a directory node into the tree, if it does not exist.

        <p>
        This only takes charge of creating a node without path components.
        @return <code>true</code>, if successiful
                <code>false</code>, if the file path already exists.
        @throws FileNotFoundException If the parent directory does not exist.
     */
    public boolean insertNode(Path file, boolean isDirectory) throws FileNotFoundException {
        // TODO: if the directory does not exist, the directories need to be created.

        Path pnt = file.parent();
        DirectoryNode dirNode = getNode(pnt);
        if(dirNode == null) {
            throw new FileNotFoundException();
        }

        return dirNode.addSubDirNode(file, isDirectory);
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
        node.addDirComp(pathComp);
    }

    /** API for registeration.
      */
    public boolean insertPathStubs(Path file, Storage storage, Command command)
            throws FileNotFoundException {
        insertNode(file, false);
        DirectoryNode node = getNode(file);
        if(node == null) {
            throw new FileNotFoundException();
        }
        node.addDirComp(new PathComponents(storage, command));
        return true;
    }

    /** Delete the node of Path p.

        <p>
        If the Path is not reachable, throw FileNotFoundException.
        @return <code>true</code>, if successiful;
                <code>false</code>, if the Path is root, return false.
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

        dirNode.sons.remove(p);
        return true;
    }

    // TODO: when writing, you may need to delete all other stubs except that storage server.
}
