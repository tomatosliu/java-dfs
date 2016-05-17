package naming.util;

import java.util.*;
import java.io.*;

import common.*;
import storage.*;

/** Scheduler.

    <p>
    Scheduler of NamingServer for Storage servers.
  */
public class Scheduler {
    /* Sorted by file numbers */
    ArrayList<PathComponents> servers = new ArrayList<PathComponents>();
    static Random rand = new Random();

    /** Get a Storage server to create file or send to client

        @throw IllegalStateException, if no storage server connecting to the naming server.
      */
    public PathComponents pickStorageServer() throws IllegalStateException {
        if(this.servers.isEmpty()) {
            throw new IllegalStateException();
        }

        PathComponents serverStubs = this.servers.get(this.rand.nextInt(this.servers.size()));
        return serverStubs;
    }

    public static synchronized PathComponents pickStorageServer(ArrayList<PathComponents> fileservers) {
        PathComponents serverStubs = fileservers.get(rand.nextInt(fileservers.size()));
        return serverStubs;
    }

    public PathComponents pickCopyStorageServer(ArrayList<PathComponents> fileservers) {
        ArrayList<PathComponents> available = new ArrayList<PathComponents>();
        for(PathComponents p: this.servers) {
            boolean exist = false;
            for(PathComponents fs: fileservers) {
                if(fs.getStorageStub().equals(p.getStorageStub())
                        || fs.getCommandStub().equals(p.getCommandStub())) {
                    exist = true;
                    break;
                }
            }
            if(!exist) {
                available.add(p);
            }
        }
        if(available.size() > 0) {
            return pickStorageServer(available);
        }
        else {
            return null;
        }
    }

    /** API for registration. Add a Storage server.

        @throw IllegalStateException, if the storage server is already registered.
      */
    public void addStorageServer(Storage storage, Command command) {
        for(PathComponents s: this.servers) {
            if(s.getStorageStub().equals(storage)
                    || s.getCommandStub().equals(command)) {
                throw new IllegalStateException();
            }
        }
        this.servers.add(new PathComponents(storage, command));
    }
}
