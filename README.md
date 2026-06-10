# PeerMarket

A peer-to-peer marketplace built on a **Chord** distributed hash table. Nodes
(*servents*) join a ring, store marketplace items distributed by key, and let
peers **list**, **search**, **buy**, and **subscribe** to items — all while the
system tolerates node crashes through failure detection and item replication.

This is a distributed-systems course project (KIDS, Domaći 2). It uses no
external build tool or framework — only the Java standard library and raw TCP
sockets.

## Features

- **Chord DHT** — consistent-hashing ring with finger tables; items are routed
  to and stored on the node responsible for their key.
- **Distributed mutual exclusion** — the Ricart–Agrawala algorithm guarantees
  that concurrent `buy` operations on the same item are serialized.
- **Failure detection** — heartbeat (ping/pong) plus a suspect/confirm protocol
  detects crashed nodes and repairs the ring.
- **Replication** — every primary item is backed up to a successor, so data
  survives the loss of its owner.
- **Publish/subscribe** — buyers can `subscribe` to a seller and are notified
  when that seller lists new items.

## Project layout

```
PeerMarket/
├── src/                         # Java source
│   ├── app/                     # Bootstrap, config, Chord state, entry points
│   │   ├── BootstrapServer.java     # Tracks the ring, hands out join points
│   │   ├── ServentMain.java         # Entry point for a single node
│   │   ├── MultipleServentStarter.java  # Launches a whole test ring at once
│   │   ├── ChordState.java          # Finger table, key ownership, routing
│   │   └── AppConfig.java           # Global config + logging
│   ├── cli/                     # Interactive command parser + commands
│   ├── market/                  # Item model, market state, buy coordinator
│   ├── mutex/                   # Ricart–Agrawala mutual exclusion
│   ├── failure/                 # Failure detector
│   └── servent/                 # Networking: listener, messages, handlers
│       ├── message/             # Message types and (de)serialization
│       └── handler/             # One handler per message type
└── peermarket/                  # Runtime test scenario
    ├── servent_list.properties  # Ports, node count, Chord size, limits
    ├── input/                   # Scripted commands fed to each node (kept)
    ├── output/                  # Per-node stdout         (generated, ignored)
    └── error/                   # Per-node stderr         (generated, ignored)
```

## Configuration

`peermarket/servent_list.properties` drives a run:

```properties
servent_count=5      # number of nodes in the ring
chord_size=256       # Chord identifier space (must be a power of 2)
bs.port=2000         # bootstrap server port
weak_limit=4000      # failure-detector "suspect" threshold (ms)
strong_limit=10000   # failure-detector "dead" threshold (ms)
servent0.port=1100   # listener port per node (must be in 1000–2000)
servent1.port=1200
servent2.port=1300
servent3.port=1400
servent4.port=1500
```

## Build & run

Requires **JDK 11+** (uses `ProcessHandle.descendants()`).

### 1. Compile

From the `PeerMarket/` directory, compile all sources into an `out/` folder
(this folder is git-ignored):

```sh
# from PeerMarket/
javac -d out $(find src -name "*.java")
```

On Windows PowerShell:

```powershell
# from PeerMarket\
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

### 2. Run the whole ring (recommended)

`MultipleServentStarter` boots the bootstrap server and all servents, wiring
each node's input/output/error to the files under `peermarket/`. Run it from the
directory that contains both `out/` and the test folder:

```sh
java -cp out app.MultipleServentStarter peermarket
```

While it runs you can type into its console:

- `kill <i>` — forcibly crash node `i` (to test failure detection)
- `stop`     — terminate every node and the bootstrap server

Each node replays the scripted commands in `peermarket/input/serventN_in.txt`
and writes its log to `peermarket/output/serventN_out.txt`.

### 3. Run a single node manually

```sh
# 1. start the bootstrap server
java -cp out app.BootstrapServer 2000

# 2. start each servent (id = 0,1,2,…) in its own terminal
java -cp out app.ServentMain peermarket/servent_list.properties 0
```

## CLI commands

Typed on a node's stdin (or scripted via `input/serventN_in.txt`):

| Command | Arguments | Description |
|---------|-----------|-------------|
| `info` | — | Print this node's Chord id, neighbors, and state. |
| `list_item` | `<item_id> <name> <qty>` | List an item for sale (routed/stored via Chord). |
| `search` | `<name>` | Search the ring for items matching a name. |
| `buy` | `<item_id> <qty>` | Buy an item (guarded by distributed mutex). |
| `subscribe` | `<ip:port>` | Subscribe to a seller's new listings. |
| `pause` | `<ms>` | Wait (used in scripted scenarios). |
| `stop` | — | Cleanly shut this node down. |

Example scenario (from `input/servent0_in.txt`):

```
pause 48000
list_item 5 Laptop 10
pause 6000
search Laptop
pause 40000
stop
```

