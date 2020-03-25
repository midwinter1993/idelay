from typing import List


class LiteLog():
    @staticmethod
    def parse(line : str) -> 'LiteLog':
        tup = line.strip().split('|')
        return LiteLog(tup)

    def __init__(self, tup):
        if len(tup) != 5:
            for t in tup:
                print(t)
        assert len(tup) == 5

        self.tsc_ = int(tup[0].strip())
        self.object_id_ = tup[1].strip()
        self.op_type_ = tup[2].strip()
        self.operand_ = tup[3].strip()
        self.location_  = tup[4].strip()

    def __str__(self):
        s = (f'Tsc: {self.tsc_}',
            f'ThreadID: {self.thread_id_}',
            f'Object ID: {self.object_id_}',
            f'Op type: {self.op_type_}',
            f'Operand: {self.operand_}',
            f'Location: {self.location_}',
        )
        return '\n'.join(s)
    
    def isWrite(self):
        return self.op_type_ == "Write"

def load_log(logpath: str) -> List[LiteLog]:
    log_list = []
            
    with open(logpath) as fd:
        for line in fd:
            log_list.append(LiteLog.parse(line))
    
    log_list.sort(key = lambda x: x.tsc_)
    return log_list


if __name__ == "__main__":
    log_list = load_log('../logexample/1.litelog')
    for l in log_list:
        print(l)
