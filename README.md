# xsync (Where are synchronizations)

## Install

1. Download or-tools: https://developers.google.com/optimization/install#binary

2. Copy the `lib` directory (under or-tools) as `./libs/or-tools-lib`

3. `./install.sh`: install a customized javassist package and build java packages

4. `source setup`

## Usage of `xtracer`

Logging:

```bash
xtracer -log [-cp class-path] [jar | main-class]
```

This command will produce a log directory containing traces of each thread.

Logging with infer result file:

```bash
xtracer -verify,<path-to-file> [-cp class-path] [jar | main-class]
```

## Usage of `xinfer`

```bash
xinfer -d [log dir]
```

This will produce a directory `xinfer-result` containing results of inference `xinfer-result/infer.result`, `xinfer-result/infer.verify` used by xtracer and other information about inference.

## Log format

Log file `<Thread ID>.log`: each line is a tuple

> Timestamp | Object ID | Op type | Operand | Location

That is interpreted as "When `timestamp`, `Object ID` operates `Op type` on `Operand` at `Location`.

- Timestamp: Nanosecond
- Object ID: Unique ID of objects
- Op type: `R/W/C` for Read/Write/Call
- Operand: Field name/Function signature
- Location: Instrumentation point location in source code (used for debugging)
