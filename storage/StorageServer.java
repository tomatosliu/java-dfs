package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{

    private File root;
    private Skeleton<Storage> storageSkeleton;
    private Skeleton<Command> commandSkeleton;

    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
    {
        if(root == null)
            throw new NullPointerException();
        this.root = root;
        this.storageSkeleton = new Skeleton<Storage>(Storage.class, this, new InetSocketAddress(client_port));
        this.commandSkeleton = new Skeleton<Command>(Command.class, this, new InetSocketAddress(command_port));
    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
        which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        this(root, 0, 0);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        this.storageSkeleton.start();
        this.commandSkeleton.start();
        Storage storageStub = Stub.create(Storage.class, this.storageSkeleton, hostname);
        Command commandStub = Stub.create(Command.class, this.commandSkeleton, hostname);
        Path[] filesToDelete = naming_server.register(storageStub, commandStub, Path.list(this.root));
        for(Path f : filesToDelete){
            this.delete(f);
        }
        deleteEmptyDirectory(this.root);
    }

    public boolean deleteEmptyDirectory(File root){
        if(root == null)
            return ;
        boolean isEmpty = true;
        File[] files = root.listFiles();
        for(File f : files){
            if(!f.isDirectory()){
                isEmpty = false;
            }
            else
                isEmpty = isEmpty && deleteEmptyDirectory(f);
        }
        if(isEmpty)
            root.delete();
        return isEmpty;
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        this.storageSkeleton.stop();
        this.commandSkeleton.stop();
        this.stopped(null);
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File f = file.toFile(this.root);
        if(!f.exists() || f.isDirectory())
            throw new FileNotFoundException();
        return f.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        File f = file.toFile(this.root);
        if(!f.exists() || f.isDirectory())
            throw new FileNotFoundException();
        if((offset + length > f.length()) || (length < 0))
            throw new IndexOutOfBoundsException();
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        byte[] bytesRead = new byte[length];
        raf.seek(offset);
        raf.read(bytesRead, 0, length);
        raf.close();
        return bytesRead;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File f = file.toFile(this.root);
        if(!f.exists() || f.isDirectory())
            throw new FileNotFoundException();
        if(offset < 0)
            throw new IndexOutOfBoundsException();
        RandomAccessFile raf = new RandomAccessFile(f, "w");
        raf.seek(offset);
        raf.write(data, 0, data.length);
        raf.close();
    }

    // The following methods are documented in Command.java.
      /** Creates a file on the storage server.

        @param file Path to the file to be created. The parent directory will be
                    created if it does not exist. This path may not be the root
                    directory.
        @return <code>true</code> if the file is created; <code>false</code>
                if it cannot be created.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    @Override
    public synchronized boolean create(Path file)
    {
        if(file == null){
            throw new NullPointerException("file is null, failure to create");
        }

        if(file.isRoot()){
            system.out.println("file is root, failure to create.");
            return false;
        }

        Path parent = file.parent();
        File pFile = parent.toFile(this.root);

        if(!pFile.exists()){
            pFile.mkdirs();
        }

        File f = file.toFile(this.root);

        try{
            return f.createNewFile();
        } catch(Exception e){
            throw new RMIException(e.getCause());
        }

        return false;
    }


    /** Deletes a file or directory on the storage server.

        <p>
        If the file is a directory and cannot be deleted, some, all, or none of
        its contents may be deleted by this operation.

        @param path Path to the file or directory to be deleted. The root
                    directory cannot be deleted.
        @return <code>true</code> if the file or directory is deleted;
                <code>false</code> otherwise.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    @Override
    public synchronized boolean delete(Path path)
    {
        
        if(path == null){
            throw new NullPointerException("path is null, failure to delete");
        }

        if(path.isRoot()){
            return false;
        }

        File file = path.toFile(root);

        return deleteHelper(file);
    }

    private boolean deleteHelper(File file){
        boolean deleteDir = true;
        if(file.isFile()){
            return file.delete();
        }else if(file.isDirectory()){
            File[] sub = file.listFiles();

            for(fsub:sub){
                if(!deleteHelper(fsub)){
                   deleteDir = false;
                   break; 
                } 
            }

            return deleteDir;
        }else{
            return false;
        }


    }
    /** Copies a file from another storage server.

        @param file Path to the file to be copied.
        @param server Storage server from which the file is to be downloaded.
        @return <code>true</code> if the file is successfully copied;
                <code>false</code> otherwise.
        @throws FileNotFoundException If the file is not present on the remote
                                      storage server, or the path refers to a
                                      directory.
        @throws IOException If an I/O exception occurs either on the remote or
                            on this storage server.
        @throws RMIException If the call cannot be completed due to a network
                             error, whether between the caller and this storage
                             server, or between the two storage servers.
     */
    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        if(file == null || server == null){
            throw new NullPointerException("failure to copy"); 
        }
        
        // throws FileNotFoundException
        long size = server.size(file);
        
        delete(file);

        create(file);

        long offset = 0;
        long left = size;
        boolean compare = true;

        while(left > 0){
            int written;
            if(left > Integer.MAX_VALUE){
                written = Integer.MAX_VALUE;
            }else{
                written = left;
            }
            
            // return IOE/RMI exception
            byte[] data = server.read(file, offset, written);
            this.write(file,offset,data);

            byte[] local = this.read(file,offset,written);
            compare = compare && Arrays.equals(data,local);
            if(!compare){
                return false;
            }

            offset += written;
            left -= written;
        }

        return true;

    }
}











