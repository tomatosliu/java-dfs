package naming.util;

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
    Storage storageStub = null;
    Command commandStub = null;

    public PathComponents(Storage storageStub, Command commandStub) {
        this.storageStub = storageStub;
        this.commandStub = commandStub;
    }

    public Storage getStorageStub() {
        return this.storageStub;
    }

    public Command getCommandStub() {
        return this.commandStub;
    }
}
