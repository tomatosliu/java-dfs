package naming;

import java.util.*;

/** Scheduler.

    <p>
    Scheduler of NamingServer for Storage servers.
  */
public class Scheduler {
    /* Sorted by file numbers */
    ArrayList<StorageServerStubs> servers = new ArrayList<StorageServerStubs>();
    Random rand = new Random();

    /** Get a Storage server to create file or send to client
    
        If the Path file already in map, it means there is a server that contains the file, so the
        action fails, returns null.
        If no storage server connecting to the naming server, then throw IllegalStateException.
      */
    public PathComponents pickStorageServer() throws IllegalStateException {
        if(this.servers.isEmpty()) {
            throw new IllegalStateException();
        }

        PathComponents pc = new PathComponents();
        StorageServerStubs serverStubs = this.servers.get(this.rand.nextInt(this.servers.size()));
        pc.addStorageStub(serverStubs.storage);
        pc.addCommandStub(serverStubs.command);

        return pc;
    }

    public void addStorageServer(Storage storage, Command command) {
        for(StorageServerStubs s: this.servers) {
            if(s.storage == storage || s.command == command) {
                throw new IllegalStateException();
            }
        }
        this.servers.add(new StorageServerStubs(storage, command));
    }


    /** Storage Server Stubs with number of using

        This is one element of priority queue.
      */
    public class StorageServerStubs {
        Storage storage;
        Command command;
        int numUsed;

        public StorageServerStubs(Storage storage, Command command) {
            this.storage = storage;
            this.command = command;
            this.numUsed = 0;
        }
    }

    /** Comparator for priority queue.
      */
    public class ServerComparator extends Comparator<StorageServerStubs> {
        public int compare(StorageServerStubs stub1, StorageServerStubs stub2) {
            if(stub1.numUsed < stub2.numUsed) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }
}
