# x-raft

**项目概述**

x-raft 是一个基于 Java 实现的 Raft 共识算法库，采用模块化设计，实现了 Raft 论文中的核心功能，包括 Leader 选举、日志复制(快照)和成员变更。

**模块结构**
大概结构
```code
x-raft/
├── api/                    # 公共 API 接口和协议定义
│   ├── protocol/           # RPC 协议 (VoteRequest, AppendEntriesRequest 等)
│   ├── log/                # 日志接口抽象 (RaftLog, LogEntry, TermIndex) 
│   ├── transport/          # 传输层接口抽象 (TransportClient, TransportServer)
│   ├── statemachine/       # 状态机接口抽象
│   ├── conf/               # 配置相关
│   └── RaftPeer.java       # 节点定义
│   └── RaftRole.java       # 角色枚举 (LEADER, FOLLOWER, CANDIDATE, LEARNER)
├── core/                   # 核心实现
│   ├── XRaftNode.java      # Raft 节点主类 (实现 RaftServerService, RaftClientService)
│   ├── XRaftServer.java    # Raft 服务器
│   ├── state/              # 状态管理
│   │   ├── RaftNodeState.java      # 节点状态 (term, votedFor, role, commitIndex)
│   │   ├── FollowerState.java      # Follower 状态 (选举超时检测)
│   │   ├── CandidateState.java     # Candidate 状态 (发起选举)
│   │   ├── LeaderState.java        # Leader 状态 (管理 LogReplicatorGroup)
│   │   ├── LearnerState.java       # Learner 状态
│   │   ├── LogReplicatorGroup.java # 日志复制组 (流水线复制实现)
│   │   └── BallotBox.java          # 投票箱
│   ├── log/                # 日志：动态加载日志实现
│   └── transport/          # 传输层：动态加载传输实现
├── transport/              # 具体传输模块实现 (XRemotingTransport)
└── storage/                # 具体存储模块实现
```

**代码规范**
- 使用 Spring Java Format 进行代码格式化
- 使用 Lombok 简化代码
- 使用 SLF4J 进行日志记录
- 避免使用hardcode和魔术数字
- 使用合适的设计模式