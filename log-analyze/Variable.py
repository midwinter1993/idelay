from typing import List, Dict, Set, Tuple
# from pulp import LpVariable, lpSum, LpMinimize
# from pulp import LpProblem, LpConstraint, LpConstraintGE, LpStatus
from flipy import LpVariable
from litelog import LiteLog, LogEntry
from LpBuilder import LpBuilder

import flipy
import math

class Variable:
    variable_pool: Dict[str, 'Variable'] = {}
    map_api_loc: Dict[str, List[str]] = {}
    variable_idref_dict: Dict[int, 'Variable'] = {}


    def __init__(self, log_entry: LogEntry, uid: int):
        # self.type_ = ty
        # self.loc_ = log_entry.location_
        self.uid_ = uid
        Variable.variable_idref_dict[uid] = self
        self.read_enforce_ = 0

        if log_entry:
            self.description_ = log_entry.description_
            self.is_write_ = log_entry.is_write()
            self.is_read_ = log_entry.is_read()

        #
        # Count of occcurence in constraints
        #
        self.rel_occ_ = []
        self.acq_occ_ = []
        self.time_gaps_ = []

        #
        # We amplify the probability as integer values in [0, 100]
        #
        self.lp_rel_var_ = LpBuilder.var(self.as_str_rel(), up_bound=100)
        self.lp_acq_var_ = LpBuilder.var(self.as_str_acq(), up_bound=100)

        #
        # Used by heuristic
        #
        self.is_rel_ = False
        self.is_acq_ = False

    def to_checkpoint(self):
        #"uid_ description_ is_write_ is_read_ [rel_occ] [acq_occ] [time_gaps_]"
        s = str(self.uid_) + " " + self.description_ + " "+ str(self.is_write_) + " " + str(self.is_read_)
        s += " " + Variable.list_to_str_by_comma(self.rel_occ_) + " " + Variable.list_to_str_by_comma(self.acq_occ_) + " " + Variable.list_to_str_by_comma(self.time_gaps_)
        return s
    @staticmethod
    def from_checkpoint(s: str):
        #"uid_ description_ is_write_ is_read_ [rel_occ] [acq_occ] [time_gaps_]"
        tuples = s.split(" ")
        if len(tuples) < 7:
            print(s)

        v = Variable(None, int(tuples[0]))
        v.description_ = tuples[1]
        v.is_write_ = tuples[2] == 'True'
        v.is_read_ = tuples[3] == 'True'
        v.rel_occ_ = Variable.str_to_list_by_comma(tuples[4])
        v.acq_occ_ = Variable.str_to_list_by_comma(tuples[5])
        v.time_gaps_ = Variable.str_to_list_by_comma(tuples[6])

        Variable.variable_pool[v.description_] = v
        return v

    @staticmethod
    def list_to_str_by_comma(l: List[int]):
        if len(l) == 0:
            return ""
        threshold = 100
        if len(l) > threshold:
            l = l[0:threshold]
        return ','.join([str(i) for i in l])

    @staticmethod
    def str_to_list_by_comma(s:str):
        # return a list of Integer
        if len(s) < 1:
            return []
        st = s.split(",")
        #print("load split " + str(len(st)) + " " +s)
        return [int(i) for i in st]

    def __str__(self):
        return str(self.uid_)

    def __repr__(self):
        return str(self)

    def __eq__(self, other):
        if isinstance(other, Variable):
            return self.uid_ == other.uid_
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __lt__(self, other):
        return self.uid_ < other.uid_

    def __hash__(self):
        return hash(self.__repr__())

    def as_str_rel(self) -> str:
        return f'R{self.uid_}'

    def as_str_acq(self) -> str:
        return f'A{self.uid_}'

    def as_lp_rel(self) -> LpVariable:
        return self.lp_rel_var_

    def as_lp_acq(self) -> LpVariable:
        return self.lp_acq_var_

    def is_marked_acq(self) -> bool:
        return self.is_acq_

    def is_marked_rel(self) -> bool:
        return self.is_rel_

    def mark_as_acq(self):
        assert not self.is_rel_
        self.is_acq_ = True

    def mark_as_rel(self):
        assert not self.is_acq_
        self.is_rel_ = True

    def inc_acq_cnt(self, k: int):
        self.acq_occ_.append(k)

    def inc_rel_cnt(self, k: int):
        self.rel_occ_.append(k)

    def set_ave_occ(self):
        self.rel_ave_ = 0
        self.rel_variance_ = 0
        if len(self.rel_occ_):
            self.rel_ave_ = sum(self.rel_occ_)/len(self.rel_occ_)
            #self.rel_variance_ = sum((i - self.rel_ave_) ** 2 for i in self.rel_occ_) / len(self.rel_occ_)

        self.acq_ave_ = 0
        self.acq_variance_ = 0
        if len(self.acq_occ_):
            self.acq_ave_ = sum(self.acq_occ_)/len(self.acq_occ_)
            #self.acq_variance_ = sum((i - self.acq_ave_) ** 2 for i in self.acq_occ_) / len(self.acq_occ_)
    '''
    def set_reg_weight(self, dic: Dict[str,int], y: int):
        # x : the total occurence
        # y : the occurence in window
        self.total_occ_ = len(dic)
        self.window_occ_ = y
        self.reg_weight_ = 1-float(self.window_occ_)/float(self.total_occ_);
        self.distribution_ = dic
    '''
    def acq_time_gap_score(self):
        if self.description_ in LogEntry.map_api_timegap:
            self.time_gaps_.extend(LogEntry.map_api_timegap[self.description_])

        ave_time_gap = round(sum(self.time_gaps_)/len(self.time_gaps_),2)
        variance_time_gap = round(math.sqrt(sum((i - ave_time_gap) ** 2 for i in self.time_gaps_) / len(self.time_gaps_)),2)
        ave_score = max(1 - ave_time_gap/2000, 0)
        variance_score = max(2*(1 - variance_time_gap/2000), 0)
        #variance_score = max(2-math.log(variance_time_gap +1,8)*0.25, 0)
        #return ave_score + variance_score
        return variance_score

    def get_classname(self):
        #if 'Call' in self.description_:
        return self.description_.split(':')[0].split('<')[0].split('|')[1]

    @classmethod
    def get_variable(cls, log_entry: LogEntry) -> 'Variable':

        #if loc not in cls.variable_pool:
        #    cls.variable_pool[loc] = Variable(loc, len(cls.variable_pool), description)
        #return cls.variable_pool[loc]

        description = log_entry.description_
        loc = log_entry.location_

        if description not in cls.variable_pool:
            cls.variable_pool[description] = Variable(log_entry, len(cls.variable_pool))
            cls.map_api_loc[description] = [loc]
        else:
            if loc not in cls.map_api_loc[description]:
                cls.map_api_loc[description].append(loc)
        return cls.variable_pool[description]

    @classmethod
    def release_var(cls, log_entry: LogEntry) -> 'Variable':
        #with cls.variable_lock:
        log_entry.in_window_ = True
        return cls.get_variable(log_entry)

    @classmethod
    def acquire_var(cls, log_entry: LogEntry) -> 'Variable':
        #with cls.variable_lock:
        log_entry.in_window_ = True
        return cls.get_variable(log_entry)


class VariableList:

    def __init__(self, var_list: List[Variable]):
        self.var_list_ = sorted(set(var_list))
        self.length_ = len(var_list)

    @staticmethod
    def from_checkpoint(s: str):
        return VariableList([Variable.variable_idref_dict[int(t)] for t in Variable.str_to_list_by_comma(s)])

    def to_checkpoint(self):
        #return Variable.list_to_str_by_comma([i.uid_ for i in self.var_list_])
        return self.key()

    def __str__(self):
        return self.to_checkpoint()

    def key(self) -> str:
        return ','.join([str(var.uid_) for var in self.var_list_])

    def __hash__(self):
        return hash(self.key())

    def __eq__(self, other):
        if isinstance(other, VariableList):
            return hash(self) == hash(other)
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __iter__(self):
        return iter(self.var_list_)
