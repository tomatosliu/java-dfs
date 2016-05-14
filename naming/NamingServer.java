package naming;

import java.io.*;
import java.net.*;
import java.util.*;

import rmi.*;
import common.*;
import storage.*;

import naming.DirectoryTree.DirectoryNode;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
    /* Class member variables for mapping paths to components */
    DirectoryTree dirTree;

    /* Skeleton for Service Interface */
    Skeleton<Service> serviceSkeleton;

    /* Skeleton for Registration Interface*/
    Skeleton<Registration> registSkeleton;

    Scheduler scheduler;
    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        this.dirTree= new DirectoryTree();
        this.serviceSkeleton = new Skeleton<Service>(Service.class, this,
                                            new InetSocketAddress(NamingStubs.SERVICE_PORT));
        this.registSkeleton = new Skeleton<Registration>(Registration.class, this,
                                            new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
        this.scheduler = new Scheduler();
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        try {
            this.serviceSkeleton.start();
            this.registSkeleton.start();
        }
        catch(RMIException e) {
            throw e;
        }
    }

    /** Stops the naming server.

        <p>
        This method commands both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        this.serviceSkeleton.stop();
        this.registSkeleton.stop();
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following public methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        DirectoryNode node = this.dirTree.getNode(path);
        if(node == null) {
            throw new FileNotFoundException();
        }

        return node.isDirectory();
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        DirectoryNode node = this.dirTree.getNode(directory);
        if(node == null) {
            throw new FileNotFoundException();
        }

        List<String> res = new ArrayList<String>();
        for(DirectoryNode n: node.getSons().values()) {
            res.add(n.getPath());
        }
        return (String[])res.toArray();
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        // 1. Find a storage server
        //    If there is no storage server for the file, it returns null.
        //
        // Note: the directory tree is consistent with the files exists on the underlying filesystem
        //       so if it throws IllegalStateException, the only situation is that there is no
        //       storage server registered.
        PathComponents pathComp = scheduler.pickStorageServer();

        // 2. Create node in the directory tree
        //    If the parent does not exist, then throw FileNotFoundException
        //    If it returns false, it means the file exists.
        boolean created = this.dirTree.createNode(file, false);
        if(!created) {
            return false;
        }

        // 3. Insert the components into the tree
        this.dirTree.insertPathComp(file, pathComp);

        // 4. Create directory on Storage Server
        //    This may throw a RMIException.
        pathComp.getCommandStub().get(0).create(file);

        return true;
    }

    @Override
    public boolean createDirectory(Path directory) throws RMIException, FileNotFoundException
    {
        // 1. Create node in the directory tree
        //    If the parent does not exist, then throw FileNotFoundException
        //    If it returns false, it means the file exists.
        boolean created = this.dirTree.createNode(directory, true);
        if(!created) {
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        // 1. delete the node in the directory tree
        //    If the file not found, throw FileNotFoundException.
        //    If the path is root, then return false.
        boolean deleted = this.dirTree.deleteNode(path);
        if(!deleted) {
            return false;
        }

        return true;
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        return null;
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        throw new UnsupportedOperationException("not implemented");
    }

}
