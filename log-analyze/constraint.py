from typing import List, Dict, Set, Tuple
from litelog import LogEntry
# from pulp import LpVariable, lpSum, LpMinimize
# from pulp import LpProblem, LpConstraint, LpConstraintGE, LpStatus
from flipy import LpVariable
import flipy
import sys
import multiprocessing
import math
from litelog import LiteLog, LogEntry
import statistics

LocationId = int

class LpBuilder:
    cons_counter = 0

    @classmethod
    def var(cls, name: str, up_bound: int):
        return flipy.LpVariable(name, var_type=flipy.VarType.Integer,
                                low_bound=0, up_bound=up_bound)

    @classmethod
    def sum_expr_weight(cls, lp_var_list: List[LpVariable], increase_flag: bool):
        delta = 0.01
        min_weight = 0.5
        n = len(lp_var_list)

        if not increase_flag:
            return flipy.LpExpression(expression = {v : max(1 - idx*delta, min_weight) for idx,v in enumerate(lp_var_list)})

        return flipy.LpExpression(expression = {v : max(1 - (n - idx - 1)*delta, min_weight) for idx,v in enumerate(lp_var_list)})

    @classmethod
    def sum_expr_weight_1(cls, lp_var_list: List[LpVariable]):
        return flipy.LpExpression(expression={v: 1 for v in lp_var_list})

    @classmethod
    def const_expr(cls, value: int):
        return flipy.LpExpression(constant=value)

    @classmethod
    def problem(cls, name: str):
        return flipy.LpProblem(name)

    @classmethod
    def _cons_id(cls):
        cls.cons_counter += 1
        return cls.cons_counter

    @classmethod
    def constraint_sum_geq_weight_increase(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr_weight(lp_var_list, True) # true means increase
        rhs = cls.const_expr(value)

        return flipy.LpConstraint(lhs, 'geq', rhs, name=f'_C{cls._cons_id()}')
    
    @classmethod
    def constraint_sum_geq_weight_decrease(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr_weight(lp_var_list, False) # false means decrease
        rhs = cls.const_expr(value)
        
        return flipy.LpConstraint(lhs, 'leq', rhs, name=f'_C{cls._cons_id()}')
    
    @classmethod
    def constraint_sum_geq_weight_1(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr_weight_1(lp_var_list)
        rhs = cls.const_expr(value)

        return flipy.LpConstraint(lhs, 'geq', rhs, name=f'_C{cls._cons_id()}')

    @classmethod
    def constraint_sum_leq_weight_1(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr_weight_1(lp_var_list)
        rhs = cls.const_expr(value)

        return flipy.LpConstraint(lhs, 'leq', rhs, name=f'_C{cls._cons_id()}')

    @classmethod
    def constraint_sum_eq_weight_1(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr_weight_1(lp_var_list)
        rhs = cls.const_expr(value)

        return flipy.LpConstraint(lhs, 'eq', rhs, name=f'_C{cls._cons_id()}')
    
    @classmethod
    def constraint_vars_eq(cls, v1: List[LpVariable], v2: List[LpVariable]):
        lhs = cls.sum_expr_weight_1(v1)
        rhs = cls.sum_expr_weight_1(v2)

        return flipy.LpConstraint(lhs, 'eq', rhs, name=f'_C{cls._cons_id()}')
        


class Variable:
    variable_pool: Dict[str, 'Variable'] = {}
    map_api_loc: Dict[str, List[str]] = {}
    def __init__(self, log_entry: LogEntry, uid: int):
        # self.type_ = ty
        self.loc_ = log_entry.location_ 
        self.uid_ = uid
        self.description_ = log_entry.description_
        
        self.read_enforce_ = 0
        # for method call, it is_write_ and is_read_ are all false
        self.is_write_ = log_entry.is_write()
        self.is_read_ = log_entry.is_read()
        #
        # Count of occcurence in constraints
        #
        self.rel_occ_ = []
        self.acq_occ_ = []

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
    
    def set_reg_weight(self, dic: Dict[str,int], y: int):
        # x : the total occurence 
        # y : the occurence in window
        self.total_occ_ = len(dic)
        self.window_occ_ = y
        self.reg_weight_ = 1-float(self.window_occ_)/float(self.total_occ_);
        self.distribution_ = dic
    
    def acq_time_gap_score(self):
        l = LogEntry.map_api_timegap[self.description_]
        ave_time_gap = round(sum(l)/len(l),2)
        variance_time_gap = round(math.sqrt(sum((i - ave_time_gap) ** 2 for i in l) / len(l)),2)
        ave_score = max(1 - ave_time_gap/2048, 0)
        variance_score = max(1 - variance_time_gap/2048, 0) 
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
        #self.var_list_ = sorted(set(var_list))
        self.var_list_ = sorted(set(var_list))

    def key(self) -> str:
        return '-'.join([str(var.uid_) for var in self.var_list_])

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


class ConstaintSystem:
    def __init__(self):
        self.rel_constraints_: Set[VariableList] = set()
        self.acq_constraints_: Set[VariableList] = set()
        self.rel_cs_list_: List[List[Variable]] = []
        self.acq_cs_list_: List[List[Variable]] = []
        self.constraints_log: List = []

    def add_constraint(self, rel_list: List[LogEntry], acq_list: List[LogEntry]):
        #if len(rel_list) and len(acq_list):
        self.constraints_log.append((rel_list, acq_list))
        self.add_release_constraint(rel_list)
        self.add_acquire_constraint(acq_list)

    def add_release_constraint(self, log_list: List[LogEntry]):
        if len(log_list):
            var_set = [Variable.release_var(log_entry) for log_entry in log_list]
            self.rel_constraints_.add(VariableList(var_set))
            self.rel_cs_list_.append(var_set)
            #print("Load a releasing constraint with size",len(log_list))

    def add_acquire_constraint(self, log_list: List[LogEntry]):
        if len(log_list):
            var_set = [Variable.acquire_var(log_entry) for log_entry in log_list]
            self.acq_constraints_.add(VariableList(var_set))
            self.acq_cs_list_.append(var_set)

    def load_shrink_relwindow(self, cs2, acq_vars: List[Variable]):
        # reset everything
        Variable.variable_pool = {}
        LpBuilder.cons_counter  = 0
        print("Reset the variable pool and lpbuider counter")

        acq_descriptions = [var.description_ for var in acq_vars]
        shrink_size = []
        for constraint in cs2.constraints_log:
            rel_constraint_logs = constraint[0]
            acq_constraint_logs = constraint[1]
            # find the related acquiring variables
            acq_logs = [log for log in acq_constraint_logs if log.description_ in acq_descriptions and int(log.time_gap_) > 500]
            if not len(acq_logs):
                self.add_constraint(rel_constraint_logs, acq_constraint_logs)
                #print("Reload a original near-miss encode")
            else: 
                new_rel_constraint_logs = []
                for rel_log in rel_constraint_logs:
                    window = [acq_log for acq_log in acq_logs if rel_log.tsc_ >= acq_log.tsc_ and rel_log.tsc_ <= acq_log.tsc_ + acq_log.time_gap_ ]
                    if len(window):
                        new_rel_constraint_logs.append(rel_log)
                if len(new_rel_constraint_logs):
                    #print("Reload a new near-miss encode relsize ",len(rel_constraint_logs),"->",len(new_rel_constraint_logs))
                    if len(rel_constraint_logs) > len(new_rel_constraint_logs):
                        shrink_size.append(len(rel_constraint_logs) - len(new_rel_constraint_logs))
                    self.add_constraint(new_rel_constraint_logs, acq_constraint_logs)
                else:
                    print("Reload a original near-miss encode because new releasing window is 0")
                    for var in acq_logs:
                        print("    Related variable : ", var.description_)
                    #debug this situation
                    self.add_constraint(rel_constraint_logs, acq_constraint_logs)
        ave_shrink = 0
        if len(shrink_size):
            ave_shrink = sum(shrink_size)/len(shrink_size)
        print("Shink",len(shrink_size), " constraints ave ", ave_shrink)

            #print('Reload a constraint with size', len(constraint[0]))
            #print('Reload a constraint with size', len(constraint[1]))



    def set_reg_weight(self, d: Dict):
        for var in Variable.variable_pool.values():
            #var.set_reg_weight(len(d[var.description_]), len([x for x in d[var.description_] if x.in_window_ ]))
            var.set_reg_weight(d[var.description_], len(Variable.map_api_loc[var.description_]))


    def print_system(self):
        print("Variable Definition")
        print("releasing window constraints ", len(self.rel_constraints_))
        print("acquiring window constraints ", len(self.acq_constraints_))

    def _lp_count_occurence(self):
        for constraint in self.rel_cs_list_:
            #
            # Update the counting
            #
            cnt = {}
            for var in constraint:
                if var not in cnt:
                    cnt[var] = 0
                cnt[var] += 1
            
            for var in cnt: 
                var.inc_rel_cnt(cnt[var])
        
        for constraint in self.acq_cs_list_:
            cnt = {}
            for var in constraint:
                if var not in cnt:
                    cnt[var] = 0
                cnt[var] += 1
            
            for var in cnt:
                # if var.uid_ == 366:
                #    print("increase 366 with ",cnt2[var])
                var.inc_acq_cnt(cnt[var])
        
        for var in Variable.variable_pool.values():
            var.set_ave_occ()

    def _lp_encode_rel(self):
        for constraint in self.rel_constraints_:
            lp_var_list = [var.as_lp_rel() for var in constraint if not var.is_marked_acq()]

            if not lp_var_list:
                continue
            #
            # There is only one release operation
            #
            penalty = LpBuilder.var(f'Penalty{len(self.penalty_vars_)}', up_bound=100)
            self.penalty_vars_.append(penalty)

            lp_var_list.append(penalty)

            #self.prob_.add_constraint(LpBuilder.constraint_sum_geq_weight_decrease(lp_var_list, 100))
            self.prob_.add_constraint(LpBuilder.constraint_sum_geq_weight_1(lp_var_list, 100))
            
            # self.prob_ += lpSum(lp_var_list) <= 199

    def _lp_encode_acq(self):
        for constraint in self.acq_constraints_:
            lp_var_list = [var.as_lp_acq() for var in constraint if not var.is_marked_rel()]

            if not lp_var_list:
                continue
            #
            # There is only one acquire operation
            #
            penalty = LpBuilder.var(f'Penalty{len(self.penalty_vars_)}', up_bound=100)
            self.penalty_vars_.append(penalty)

            lp_var_list.append(penalty)

            #self.prob_.add_constraint(LpBuilder.constraint_sum_geq_weight_increase(lp_var_list, 100))
            self.prob_.add_constraint(LpBuilder.constraint_sum_geq_weight_1(lp_var_list, 100))
            
            # self.prob_ += lpSum(lp_var_list) <= 199

    def _lp_encode_all_vars(self):
        #
        # For each variable/location, P_rel + P_acq < 100?
        #
        for var in Variable.variable_pool.values():
            #
            # TBD
            #
            self.prob_.add_constraint(LpBuilder.constraint_sum_leq_weight_1([var.as_lp_acq(), var.as_lp_rel()], 100))

    def _lp_encode_all_vars_heuristic(self):
        
        #for var in Variable.variable_pool.values():
        #    if 'Monitor::Enter' in var.description_:
        #        var.mark_as_acq()
        #        self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_acq()], 100))
        #    if 'Monitor::Exit' in var.description_:
        #        var.mark_as_rel()
        #        self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_rel()], 100))

        for var in Variable.variable_pool.values():
            if var.is_read_:
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq_weight_1([var.as_lp_rel()], 0))

            if var.is_write_:
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq_weight_1([var.as_lp_acq()], 0))

            if '-Begin' in var.description_:
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq_weight_1([var.as_lp_rel()], 0))
            if '-End' in var.description_:
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq_weight_1([var.as_lp_acq()], 0)) 

 
    def _lp_encode_read_write_relation(self):
        for var in Variable.variable_pool.values():
            if 'Read|' in var.description_:
                wkey = var.description_.replace('Read','Write')
                if wkey not in Variable.variable_pool:
                    self.prob_.add_constraint(LpBuilder.constraint_sum_eq_weight_1([var.as_lp_acq()], 0))
                    var.read_enforce_ = 1
                else:
                    self.prob_.add_constraint(LpBuilder.constraint_vars_eq([var.as_lp_acq()], [Variable.variable_pool[wkey].as_lp_rel()]))
                    var.read_enforce_ = -1
        
        class_dict : Dict[str, List['Variable']] = {} 

        for var in Variable.variable_pool.values():
            
            cname = var.get_classname()

            if cname not in class_dict:
                class_dict[cname] = []

            class_dict[cname].append(var)
        
        for cn in class_dict:
            l = class_dict[cn]
            #print("Class name",cn)
            #for v in l:
            #    print("    ",v.uid_," ",v.description_)
            #'''
            lhs = [v.as_lp_acq() for v in l]
            rhs = [v.as_lp_rel() for v in l]
            penalty1 = LpBuilder.var(f'ClassPenalty{len(self.classname_penalty_vars_)}', up_bound=500)
            #self.penalty_vars_.append(penalty1)
            self.classname_penalty_vars_.append(penalty1)
            lhs.append(penalty1)

            penalty2 = LpBuilder.var(f'ClassPenalty{len(self.classname_penalty_vars_)}', up_bound=500)
            #self.penalty_vars_.append(penalty2)
            self.classname_penalty_vars_.append(penalty2)
            rhs.append(penalty2)
            self.prob_.add_constraint(LpBuilder.constraint_vars_eq(lhs,rhs))
            #'''
    def _lp_ave_occ_weight(self, x):
        #return x*x/10
        return 0.5 * x

    def _lp_encode_object_func(self):
        #
        # Object function: min(penalty +  k * all variables)
        #
        #obj_func_vars = self.penalty_vars_
        obj_func = {v : 1 for v in self.penalty_vars_}
        
        k = 0.2

        for var in Variable.variable_pool.values():
            obj_func[var.as_lp_acq()] = k * (1 + self._lp_ave_occ_weight(var.acq_ave_) + var.acq_time_gap_score())
            obj_func[var.as_lp_rel()] = k * (1 + self._lp_ave_occ_weight(var.rel_ave_))
        
            # obj_func[var.as_lp_acq()] = k* (1 +  var.acq_time_gap_score())  
            # obj_func[var.as_lp_rel()] = k* (1)

        for lpv in self.classname_penalty_vars_:
            obj_func[lpv] = k / 4

        obj = flipy.LpObjective(expression=obj_func, sense=flipy.Minimize)
        
        # obj = flipy.LpObjective(expression={v: 1 for v in obj_func_vars}, sense=flipy.Minimize)

        self.prob_.set_objective(obj)

    def print_debug_info(self):

        #
        # print the cnt of occurence of variable in constrains
        #
        
        for var in Variable.variable_pool.values():
            #n = len(Variable.map_api_loc[var.description_])
            #print(var.acq_occ)
            l = LogEntry.map_api_timegap[var.description_]
            ave_time_gap = round(sum(l)/len(l),2)
            variance_time_gap = round(math.sqrt(sum((i - ave_time_gap) ** 2 for i in l) / len(l)),2)
            ave_score = round(max(0.5-int(math.log(ave_time_gap, 8))*0.1, 0),2)
            #variance_score = round(max(0.5 - variance_time_gap/(2*ave_time_gap),0),2)
            variance_score = round(max(0,var.acq_time_gap_score()), 2)
            rel_occ_score = self._lp_ave_occ_weight(var.rel_ave_)
            acq_occ_score = self._lp_ave_occ_weight(var.acq_ave_)

            print(var.uid_,"R:",len(var.rel_occ_),"Roccw:",round(rel_occ_score,2),"RV",var.as_lp_rel().evaluate(),"A:",len(var.acq_occ_),"Aave:",round(acq_occ_score,2),"AV",var.as_lp_acq().evaluate() ,var.description_,f'[{ave_time_gap}_{ave_score},{variance_time_gap}_{variance_score}]',f"R = {var.is_read_},W = {var.is_write_}")
        
        call_time_gap = []
        heap_time_gap = []
        #for description in LogEntry.map_api_timegap:
        for var in Variable.variable_pool.values():
            description = var.description_
            if 'Call|' in description:
                call_time_gap += LogEntry.map_api_timegap[description]
            else:
                heap_time_gap += LogEntry.map_api_timegap[description]

        call_time_gap.sort()
        heap_time_gap.sort()

        #ave_call_time_gap = round(sum(call_time_gap)/len(call_time_gap),2)
        #ave_heap_time_gap = round(sum(heap_time_gap)/len(heap_time_gap),2)
        #print("AVE call gap",ave_call_time_gap, "   AVE heap gap",ave_heap_time_gap)
        if len(call_time_gap) == 0 or len(heap_time_gap) == 0:
            return 
        for i in range(20):
            call_i = int(len(call_time_gap) * i * 0.05)
            heap_i = int(len(heap_time_gap) * i * 0.05)
            #print(f'{call_i}/{len(call_time_gap)}', f'{heap_i}/{len(heap_time_gap)}')
            print(f'Call gap {i}*5% ', round(call_time_gap[call_i], 2), f'   Heap gap {i}*5% ', round(heap_time_gap[heap_i], 2))

        # med_call_time_gap = statistics.median(call_time_gap)
        # med_heap_time_gap = statistics.median(heap_time_gap)
        # print("MED call gap",med_call_time_gap, "   MED heap gap",med_heap_time_gap)

        
        with open('./time_gap.lp', 'a+') as fd:
            for var in Variable.variable_pool.values():
                l = LogEntry.map_api_timegap[var.description_]
                ave_time_gap = round(sum(l)/len(l),2)
                variance_time_gap = round(math.sqrt(sum((i - ave_time_gap) ** 2 for i in l) / len(l)),2)
                st = var.description_.split('.')
                short_name = st[len(st)-1]
                fd.write(f'{short_name}!{ave_time_gap}!{variance_time_gap}!{var.as_lp_rel().evaluate()}!{var.as_lp_acq().evaluate()}\n')
        
        return 

    def save_info(self):
        with open('./problem.lp', 'a+') as fd:
            fd.write('Sync Variable Definition\n===\n')

            for var in Variable.variable_pool.values():
                fd.write(f'{var.as_str_rel()}: {var.as_lp_rel().evaluate()} ')
                fd.write(f'{var.as_str_acq()}: {var.as_lp_acq().evaluate()}\n')

            for penalty in self.penalty_vars_:
                fd.write(f'{penalty.name}: {penalty.evaluate()}\n')
            for penalty in self.classname_penalty_vars_:
                fd.write(f'{penalty.name}: {penalty.evaluate()}\n')

    def _lp_solve(self):
        self.prob_ = LpBuilder.problem("HB_Infer")
        self.penalty_vars_ = []
        self.classname_penalty_vars_ = []

        #
        # Heuristic must be encoded first
        #
         
        self._lp_encode_all_vars_heuristic()

        self._lp_encode_rel()
        self._lp_encode_acq()
        self._lp_encode_all_vars()

        self._lp_encode_read_write_relation()
        self._lp_count_occurence()
        
        self._lp_encode_object_func()

        solver = flipy.CBCSolver()
        self.status = solver.solve(self.prob_)

    def return_result(self):
        l1 = [var for var in Variable.variable_pool.values() if var.as_lp_rel().evaluate() >= 95]
        l2 = [var for var in Variable.variable_pool.values() if var.as_lp_acq().evaluate() >= 95]
        return l1, l2

    def print_compare_result(self, req_vars: List[Variable], acq_vars: List[Variable]):
        l1, l2 = self.return_result()
        print('Solving Status:', self.status)
        print()
        print("Releasing sites: ")
        for var in l1:
            pair = [i for i in req_vars if i.description_ == var.description_]
            if len(pair):
                print(f'Old  {var.description_} => {var.as_str_rel()}, Occ={var.total_occ_}')
            else:
                print(f'New  {var.description_} => {var.as_str_rel()}, Occ={var.total_occ_}')
                for loc in Variable.map_api_loc[var.description_]:
                    print("  @",len(LogEntry.map_api_entry[var.description_]),loc)
                for loc in LogEntry.map_api_entry[var.description_]:
                    if loc not in Variable.map_api_loc[var.description_]:
                        print("  X",len(LogEntry.map_api_entry[var.description_]),loc)
        for var in req_vars:
            pair = [i for i in l1 if i.description_ == var.description_]
            if not len(pair):
                print(f'Drop {var.description_} => {var.as_str_rel()}, Occ={var.total_occ_}')
    
        print()
        print("Acquiring sites: ")
        for var in l2:
            pair = [i for i in acq_vars if i.description_ == var.description_]
            if len(pair):
                print(f'Old  {var.description_} => {var.as_str_acq()}, Occ={var.total_occ_}')
            else:
                print(f'New  {var.description_} => {var.as_str_acq()}, Occ={var.total_occ_}')
                for loc in Variable.map_api_loc[var.description_]:
                    print("  @",len(LogEntry.map_api_entry[var.description_]),loc)
                    for loc in LogEntry.map_api_entry[var.description_]:
                        if loc not in Variable.map_api_loc[var.description_]:
                            print("  X",len(LogEntry.map_api_entry[var.description_]),loc)

        for var in acq_vars:
            pair = [i for i in l2 if i.description_ == var.description_]
            if not len(pair):
                print(f'Drop {var.description_} => {var.as_str_acq()}, Occ={var.total_occ_}')


    
    def print_result(self):
        print()
        print("Constrains number : ",len(self.penalty_vars_))
        print()
        self.print_debug_info()
        print('Solving Status:', self.status)
        print()
        print("Releasing sites :")
        for name, var in Variable.variable_pool.items():
            if (var.as_lp_rel().evaluate() >= 95):
                print(f'{var.description_} => {var.as_str_rel()}, Occ={var.total_occ_}')
                for loc in Variable.map_api_loc[var.description_]:
                    print("  @",len(LogEntry.map_api_entry[var.description_]),loc)
                for loc in LogEntry.map_api_entry[var.description_]:
                    if loc not in Variable.map_api_loc[var.description_]:
                        print("  X",len(LogEntry.map_api_entry[var.description_]),loc)

        print()
        print("Acquiring sites :")
        for name, var in Variable.variable_pool.items():
            if (var.as_lp_acq().evaluate() >= 95):
                print(f'{var.description_} => {var.as_str_acq()}, Occ={var.total_occ_}')
                for loc in Variable.map_api_loc[var.description_]:
                    print("  @",len(LogEntry.map_api_entry[var.description_]),loc)
                for loc in LogEntry.map_api_entry[var.description_]:
                    if loc not in Variable.map_api_loc[var.description_]:
                        print("  X",len(LogEntry.map_api_entry[var.description_]),loc)
        
        
        self.prob_.write_lp(open('./problem.lp', 'w'))
        self.save_info()

