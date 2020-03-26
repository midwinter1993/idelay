from typing import List


class LogEntry():
    @staticmethod
    def parse(line: str) -> 'LogEntry':
        tup = line.strip().split('|')
        return LogEntry(tup)

    def __init__(self, tup):
        if len(tup) != 5:
            for t in tup:
                print(t)
        assert len(tup) == 5

        self.tsc_ = int(tup[0].strip())
        self.object_id_ = tup[1].strip()
        self.op_type_ = tup[2].strip()
        self.operand_ = tup[3].strip()
        self.location_ = tup[4].strip()
        self.thread_id_ = -1  # Fixed after log is loaded

    def __str__(self):
        s = (f'Tsc: {self.tsc_}',
             f'ThreadID: {self.thread_id_}',
             f'Object ID: {self.object_id_}',
             f'Op type: {self.op_type_}',
             f'Operand: {self.operand_}',
             f'Location: {self.location_}',
             )
        return '\n'.join(s)

    def is_write(self) -> bool:
        return self.op_type_ == "Write"

    def is_conflict(self, another: 'LogEntry') -> bool:
        if ((self.thread_id_ != another.thread_id_) and
            (self.is_write() or another.is_write())):
            return True

        return False


def load_log(logpath: str) -> List[LogEntry]:
    log_list = []

    with open(logpath) as fd:
        for line in fd:
            log_list.append(LogEntry.parse(line))

    log_list.sort(key=lambda x: x.tsc_)
    return log_list