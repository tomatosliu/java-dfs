package naming.util;

import java.util.*;
import java.io.*;
import java.util.concurrent.*;

import common.*;
import storage.*;
import naming.util.*;
import rmi.*;


/** DirectoryTree.

    <p>
  */
public class DirectoryNode {
    Path path;
    HashMap<Path, DirectoryNode> sons;
    boolean isDirectory;
    ArrayList<PathComponents> pathComps;
    Semaphore readwriteSemaphore;
    int readNum = 0;
    final int READMAX = 100;

    public DirectoryNode(Path p, boolean isDirectory) {
        this.path = p;
        this.isDirectory = isDirectory;
        readwriteSemaphore = new Semaphore(READMAX, true);
        if(this.isDirectory) {
            this.sons = new HashMap<Path, DirectoryNode>();
        }
        else {
            this.pathComps = new ArrayList<PathComponents>();
        }
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public HashMap<Path, DirectoryNode> getSons() {
        return this.sons;
    }

    public Path getPath() {
        return this.path;
    }

    public ArrayList<PathComponents> getPathComps() {
        return this.pathComps;
    }

    public DirectoryNode getNextNode(Path path) {
        DirectoryNode res = null;
        if(path == null || sons == null || sons.size() == 0) {
            return null;
        }

        for(Path p: sons.keySet()) {
            if(path.isSubpath(p)) {
                return sons.get(p);
            }
        }
        return null;
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

    public void lock(boolean exclusive, Scheduler scheduler)
            throws InterruptedException, RMIException {
        if(exclusive) {
            // write lock
            this.readwriteSemaphore.acquire(READMAX);

            //TODO: delete servers but one
            if(!this.isDirectory()) {
                PathComponents comp = scheduler.pickStorageServer(this.pathComps);
                for(PathComponents p: this.pathComps) {
                    if(!p.getStorageStub().equals(comp.getStorageStub())) {
                        p.getCommandStub().delete(this.path);
                    }
                }
                this.pathComps = new ArrayList<PathComponents>();
                this.pathComps.add(comp);
            }
        }
        else {
            // read lock
            this.readwriteSemaphore.acquire();
            if(!isDirectory) {
                this.readNum ++;
                if(this.readNum == 20) {
                    readNum = 0;
                    replicate(scheduler); // start a thread to make a copy
                }
            }
        }
    }

    public void unlock(boolean exclusive) {
        if(exclusive) {
            // write lock
            this.readwriteSemaphore.release(READMAX);
        }
        else {
            // read lock
            this.readwriteSemaphore.release();
        }
    }

    public void replicate(Scheduler scheduler) {
        (new CopyThread(this.path, this.pathComps, scheduler)).start();
    }

    public class CopyThread extends Thread {
        ArrayList<PathComponents> pathComps;
        Path file;
        Scheduler scheduler;

        public CopyThread(Path file, ArrayList<PathComponents> pathComps, Scheduler scheduler) {
            this.file = file;
            this.pathComps = pathComps;
            this.scheduler = scheduler;
        }

        public void run() {
            PathComponents comp = scheduler.pickCopyStorageServer(this.pathComps);
            if(comp != null) {
                try{
                    comp.getCommandStub().copy(this.file,
                            scheduler.pickStorageServer(this.pathComps).getStorageStub());
                    this.pathComps.add(comp);
                }
                catch(FileNotFoundException e) {
                    // throw new FileNotFoundException();
                }
                catch(RMIException e) {
                    // throw new RMIException(e);
                }
                catch(IOException e) {
                    // this.skeleton.listen_error(e);
                }
            }
        }
    }
}
