## Log format

Log file `<Thread ID>.log`: each line is a tuple

> Timestamp | Op type | Operand | Object ID | Location

That is interpreted as "When `timestamp`, `Object ID` operates `Op type` on `Operand` at `Location`.

- Timestamp: Nanosecond
- Object ID: Unique ID of objects
- Op type: `R/W/C` for Read/Write/Call
- Operand: Field name/Function signature
- Location: Instrumentation point location in source code (used for debugging)
