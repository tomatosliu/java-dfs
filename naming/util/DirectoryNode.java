package naming.util;

import java.util.*;
import java.io.*;

import common.*;
import storage.*;

/** DirectoryTree.

    <p>
  */
public class DirectoryNode {
    Path path;
    HashMap<Path, DirectoryNode> sons;
    boolean isDirectory;
    ArrayList<PathComponents> pathComps;

    public DirectoryNode(Path p, boolean isDirectory) {
        this.path = p;
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
        return path.toString();
    }

    public ArrayList<PathComponents> getPathComps() {
        return this.pathComps;
    }

    /** add a sub directory into current directory node.

        @return <code>false</code> if the added node already exists in the sub directory list.
      */
    public boolean addSubDirNode(Path p, boolean isDirectory) throws FileNotFoundException {
        if(!p.isSubpath(this.path)) {
            throw new FileNotFoundException();
        }
        if(this.sons.containsKey(p)) {
            return false;
        }
        else {
            this.sons.put(p, new DirectoryNode(p, isDirectory));
            return true;
        }
    }

    /** add a set of path components into current directory node.

      */
    public void addDirComp(PathComponents pathComp) {
        this.pathComps.add(pathComp);
    }

    // TODO: Maybe there needs some deletion methods.
}
