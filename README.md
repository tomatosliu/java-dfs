# Java DFS

## First Milestone

### NamingServer

As long as one client want to create or delete file, it must interact with `NamingServer`. It means `NamingServer` maintains a complete structure of directory tree. `DirectoryTree` usually accepts a `Path` as input and outputs `DirectoryNode` or other information. Most of methods in `Service` need to utilize `DirectoryTree` to store and retrieve information of directories and files. 

At the same time, `DirectoryTree` makes the distributed file system hided from the view from clients, however, `NamingServer` handles the schedule of `StorageServer`. Therefore, I implement a module of `Scheduler`.  `Scheduler` has two kinds of function. First, it maintain a list of `StorageServer`. When a file creation is requested from client, `NamingServer` will call `Scheduler` to get a proper `StorageServer` to actually create the file. Second, it provides a algorithm module for choosing a `StorageServer` from a list of them. When a client asks for a file, we choose a specific `StorageServer` to return to the client, which allows the client to read and write.

#### Directory Tree
There are a list of APIs which can be used to modify directory tree.

The following is the set of methods I think which may be used in later locking and replication:
```java
/** Get a directory node in the tree by Path p.
    @return a directory node, if there is a node of Path p, otherwise, return <code>null</code>.
      */
public DirectoryNode getNode(Path p)

/** Create a directory.
     */
private boolean createDir(Path directory)

/** Delete the node of Path p.
    <p>
    If the Path is not reachable, throw FileNotFoundException.
    @return <code>true</code>, if successiful;
            <code>false</code>, if the Path is root, return false.
*/
public boolean deleteNode(Path p)

/** Insert a file node into the tree, if it does not exist.
    This will success when the directory already exists. If not, please call creatDir first to
    create parent directory.
    <p>
    This only takes charge of creating a node without path components.
    @return <code>true</code>, if success.
            <code>false</code>, if the file path already exists or file is root.
    @throws FileNotFoundException If the parent directory does not exist or
 */
public boolean insertNode(Path file, boolean isDirectory)
  
/** Insert path component into a Path.
    This will success when the directory already exists. If not, please call creatDir first to
    create parent directory.
    Also, this method usually follows insertNode.
    <p>
    Take charge of insert path component into the Path, the caller need to call scheduler to
    get a storage server for operating.
    @throws FileNotFoundException If the path does not exist or the path is a directory.
 */
public void insertPathComp(Path p, PathComponents pathComp)
```

#### Service Interface
##### `String[] list(Path directory)`
1. You need to make sure that directory exists and corresponds to a actually directory, not a file.

##### `boolean createFile(Path file)`
You need to do two things related to `DirectoryTree` and `Scheduler`.

1. Find a available `StorageServer` from `Scheduler` to actually create the file. 
2. Insert a file node into `DirectoryTree`

##### `boolean createDirectory(Path directory)`
You do not need to inform `StorageServer` to create a directory, because when creating files, the `StorageServer` will automatically create needed directories.

##### `Storage getStorage(Path file)`
It needs to make sure `file` is a file, not a directory. 

#### Registration Interface

##### `Path[] register(Storage client_stub, Command command_stub, Path[] files)`
You need to do two things related to `DirectoryTree` and `Scheduler`.

1. Insert the stubs into `Scheduler`.
2. Insert files into `DirectoryTree`.