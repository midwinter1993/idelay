## Log format

Log file `<Thread ID>.log`: each line is a tuple

> Timestamp, Op type, Operand, Object ID, Location

- Timestamp: Nanosecond
- ObjectID: Unique ID of objects
- Op type: `R/W/C` for Read/Write/Call
- Operand: Field name/Function signature
- Location: Instrucmentation point location in source code (used for debugging)
