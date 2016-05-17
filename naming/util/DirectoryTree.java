package naming.util;

import java.util.*;
import java.io.*;

import common.*;
import storage.*;
import naming.util.*;
import rmi.*;

public class DirectoryTree {
    DirectoryNode root;
    public DirectoryTree() {
        this.root = new DirectoryNode(new Path(), true);
    }

    public void lock(Path p, boolean exclusive, Scheduler scheduler)
                throws FileNotFoundException, RMIException {
        DirectoryNode curNode = this.root;
        while(true) {
            try {
                Path curPath = curNode.path;

                if(curPath.equals(p)) {
                    curNode.lock(exclusive, scheduler);
                    break;
                }
                else {
                    curNode.lock(false, scheduler);
                    curNode = curNode.getNextNode(p);
                    if(curNode == null) {
                        throw new FileNotFoundException();
                    }
                }
            }
            catch(InterruptedException e) {
                // if the thread is interrupted
            }
        }
    }

    public void unlock(Path p, boolean exclusive) throws FileNotFoundException {
        DirectoryNode curNode = this.root;
        while(true) {
            Path curPath = curNode.path;

            if(curPath.equals(p)) {
                curNode.unlock(exclusive);
                break;
            }
            else {
                curNode.unlock(false);
                curNode = curNode.getNextNode(p);
                if(curNode == null) {
                    throw new FileNotFoundException();
                }
            }
        }
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
                curNode = curNode.getNextNode(p);
                if(curNode == null) {
                    break;
                }
            }
        }
        return null;
    }

    /** Insert a file node into the tree, if it does not exist.
        This will success when the directory already exists. If not, please call creatDir first to
        create parent directory.

        <p>
        This only takes charge of creating a node without path components.
        @return <code>true</code>, if success.
                <code>false</code>, if the file path already exists or file is root.
        @throws FileNotFoundException If the parent directory does not exist or
     */
    public boolean insertNode(Path file, boolean isDirectory) throws FileNotFoundException {
        if(file.isRoot()) {
            return false;
        }

        Path pnt = file.parent();
        DirectoryNode dirNode = getNode(pnt);
        if(dirNode == null || !dirNode.isDirectory()) {
            throw new FileNotFoundException();
        }

        return dirNode.addSubDirNode(file, isDirectory);
    }

    /** Insert path component into a Path.
        This will success when the directory already exists. If not, please call creatDir first to
        create parent directory.
        Also, this method usually follows insertNode.

        <p>
        Take charge of insert path component into the Path, the caller need to call scheduler to
        get a storage server for operating.
        @throws FileNotFoundException If the path does not exist or the path is a directory.
     */
    public void insertPathComp(Path p, PathComponents pathComp) throws FileNotFoundException {
        DirectoryNode node = getNode(p);
        if(node == null  || node.isDirectory()) {
            throw new FileNotFoundException();
        }
        node.addDirComp(pathComp);
    }

    /** Create a directory.
     */

    private boolean createDir(Path directory) {
        if(getNode(directory) != null) {
            return true;
        }

        Stack<Path> pstack = new Stack<Path>();
        Path curPath = directory;

        // Find out the stack of non-created directories
        while(!curPath.isRoot()) {
            DirectoryNode node = getNode(curPath);

            if(node != null) {
                break;
            }
            else {
                pstack.push(curPath);
            }
            curPath = curPath.parent();
        }

        // Create all the directories
        try{
            while(!pstack.isEmpty()) {
                curPath = pstack.pop();
                getNode(curPath.parent()).addSubDirNode(curPath, true);
            }
        }
        catch(FileNotFoundException e) {
            return false;
        }
        return true;
    }

    /** API for registeration. Insert the stubs of storage server

        @return <code>true</code> if success.
                <code>false</code> if there is Path file already exists on the directory tree.
      */
    public boolean insertPathStubs(Path file, Storage storage, Command command){
        createDir(file.parent());
        if(getNode(file.parent()).sons.containsKey(file)){
            return false;
        }
        else {
            try {
                insertNode(file, false);
            }catch(FileNotFoundException e){
                return false;
            }
            DirectoryNode node = getNode(file);
            node.addDirComp(new PathComponents(storage, command));
            return true;
        }
    }

    /** Delete the node of Path p.

        <p>
        If the Path is not reachable, throw FileNotFoundException.
        @return <code>true</code>, if successiful;
                <code>false</code>, if the Path is root, return false.
      */
    public boolean deleteNode(Path p) throws FileNotFoundException, RMIException {
        if(p.isRoot()) {
            return false;
        }

        DirectoryNode dNode = getNode(p);
        if(dNode == null) {
            throw new FileNotFoundException();
        }

        Path pnt = p.parent();
        DirectoryNode dirNode = getNode(pnt);

        // System.out.println("\n---------- deleteNode: " + p + " " + dirNode);
        if(dirNode == null) {
            throw new FileNotFoundException();
        }


        // Deletion
        ArrayList<PathComponents> res = getComps(p);
        for(PathComponents pc: res) {
            pc.getCommandStub().delete(p);
        }
        dirNode.sons.remove(p);
        return true;
    }

    public ArrayList<PathComponents> getComps(Path path) {
        DirectoryNode node = getNode(path);

        if(node.isDirectory()) {
            ArrayList<PathComponents> res = new ArrayList<PathComponents>();
            for(Path sonpath: node.getSons().keySet()) {
                res = mergePathComps(res, getComps(sonpath));
            }
            return res;
        }
        else {
            return node.getPathComps();
        }

    }

    private ArrayList<PathComponents> mergePathComps(ArrayList<PathComponents> list1,
                                    ArrayList<PathComponents> list2) {
        ArrayList<PathComponents> res = new ArrayList<PathComponents>(list1);
        for(PathComponents p2: list2) {
            boolean exist = false;
            for(PathComponents p1: list1) {
                if(p1.getStorageStub().equals(p2.getStorageStub())) {
                    exist = true;
                    break;
                }
            }
            if(!exist) {
                res.add(p2);
            }
        }
        return res;
    }
}
