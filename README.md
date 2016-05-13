# Java DFS

## First Milestone

### Data Structures for NamingServer

1. Scheduler

        调度server。其中有一个list，保存所有register过的Storage Server的两个stubs。
        现在被使用的方面：client create file的时候，需要分类一个server去创建这个文件，这时候需要调度出来一个Storage server。另外，就是register的时候，需要把这个server加进去。

2. DirectoryTree

        根据Path找到directory的某节点。`DirectoryNode`
        每个节点保存几个内容：是不是directory，它的子节点list，还有就是这个文件在哪些Storage Server上有（存储了他们的stubs）。
        树是主要的数据结构，比如client请求读写，找到相应的stub。server注册的时候，插入初始的Path list等等。

这俩数据结构还需要优化，接口和设计等。
