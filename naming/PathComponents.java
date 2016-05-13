package naming;

import java.util.*;
import java.io.*;

import common.*;
import storage.*;

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
