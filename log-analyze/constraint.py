from typing import List, Dict, Set, Tuple
# from pulp import LpVariable, lpSum, LpMinimize
# from pulp import LpProblem, LpConstraint, LpConstraintGE, LpStatus
from flipy import LpVariable
from litelog import LiteLog, LogEntry
from Variable import Variable,VariableList
from LpBuilder import LpBuilder

import flipy
import sys, math, os, statistics

class ConstaintSystem:
    def __init__(self):
        self.rel_constraints_: Set[VariableList] = set()
        self.acq_constraints_: Set[VariableList] = set()
        self.rel_cs_list_: List[List[Variable]] = []
        self.acq_cs_list_: List[List[Variable]] = []
        self.constraints_log: List = []

        self.sig_cons = {} #map from sig to the constraints
        self.concurrent_sigs = set()

    def load_checkpoint(self, cp_dir:str):
        # the order of loading checkpoint is very important
        #variable
        self.pre_rel_vars_ = []
        self.pre_acq_vars_ = []

        if os.path.exists(os.path.join(cp_dir,'vars.lp')):
            with open(os.path.join(cp_dir,'vars.lp')) as fvars:
                for cnt, line in enumerate(fvars):
                    Variable.from_checkpoint(line)
                print("Load",str(len(Variable.variable_pool)),"variables")

        #Constraints
        if os.path.exists(os.path.join(cp_dir,'constraints.lp')):
            with open(os.path.join(cp_dir,'constraints.lp')) as fcons:
                for cnt, line in enumerate(fcons):
                    vl = VariableList.from_checkpoint(line[2:])
                    if line[0] == 'R':
                        self.rel_constraints_.add(vl)
                    else:
                        self.acq_constraints_.add(vl)

        if os.path.exists(os.path.join(cp_dir,'rel_vars.lp')):
            with open(os.path.join(cp_dir,'rel_vars.lp')) as frel:
                self.pre_rel_vars_ = [Variable.variable_idref_dict[int(l.split()[0])] for l in frel.readlines()]

        if os.path.exists(os.path.join(cp_dir,'acq_vars.lp')):
            with open(os.path.join(cp_dir,'acq_vars.lp')) as facq:
                self.pre_acq_vars_ = [Variable.variable_idref_dict[int(l.split()[0])] for l in facq.readlines()]

        if os.path.exists(os.path.join(cp_dir,'concurrent_sigs.lp')):
            with open(os.path.join(cp_dir,'concurrent_sigs.lp')) as fcsig:
                for l in fcsig.readlines():
                    if len(l) > 1:
                        self.concurrent_sigs.add(l[:-1])
                        print("checkpointing : Adding concurrent signatures " + l[:-1])
                #self.pre_acq_vars_ = [Variable.variable_idref_dict[int(l.split()[0])] for l in facq.readlines()]

        return

    def add_constraint(self, rel_list: List[LogEntry], acq_list: List[LogEntry], objid: int):
        #if len(rel_list) and len(acq_list):
        self.constraints_log.append((rel_list, acq_list))
        self.add_release_constraint(rel_list, objid)
        self.add_acquire_constraint(acq_list, objid)

    def add_release_constraint(self, log_list: List[LogEntry], objid: int):
        if len(log_list):
            var_set = [Variable.release_var(log_entry) for log_entry in log_list if not log_entry.is_sleep_ ]
            self.rel_constraints_.add(VariableList(var_set, LogEntry.int_to_objid[objid]))
            self.rel_cs_list_.append(var_set)

    def add_acquire_constraint(self, log_list: List[LogEntry], objid: int):
        if len(log_list):
            var_set = [Variable.acquire_var(log_entry) for log_entry in log_list if not log_entry.is_sleep_]
            self.acq_constraints_.add(VariableList(var_set, LogEntry.int_to_objid[objid]))
            self.acq_cs_list_.append(var_set)

    def build_constraints(self):
        for sig in self.sig_cons:
            l = self.sig_cons[sig]
            length = [len(c[0]) + len(c[1]) for c in l]
            if sig in self.concurrent_sigs:
                print("Ignore concurrent op " + sig)
                continue

            if 0 in length:
                self.concurrent_sigs.add(sig)
                print("Processing adding concurrent signatures " + sig)
            else:
                for c in l:
                    rel_log_list    = c[0]
                    acq_log_list    = c[1]
                    start_log_entry = c[3]
                    end_log_entry   = c[4]
                    objid = c[2]
                    self.add_constraint(rel_log_list, acq_log_list, objid)
                    print()
                    print("Find a nearmiss : ")

                    print("Start op : ",start_log_entry.op_type_,"|",start_log_entry.operand_,"|",start_log_entry.location_, "|", start_log_entry.start_tsc_)
                    print("Releasing window : ")
                    s = '1 <= '
                    for log_entry in rel_log_list:
                        i = Variable.release_var(log_entry)
                        print(i.description_," ",i.loc_)
                        s += 'R' + str(i.uid_) + ' + '
                    print(s)

                    print("End   op : ",end_log_entry.op_type_, "|",end_log_entry.operand_,"|", end_log_entry.location_, "|", end_log_entry.start_tsc_)
                    print("Acquiring window : ")
                    s = '1 <= '
                    for log_entry in acq_log_list:
                        i = Variable.acquire_var(log_entry)
                        print(i.description_," ",i.loc_)
                        s += 'A' + str(i.uid_) + ' + '
                    print(s)
                    print()

    def _lp_count_occurence(self):
        for constraint in self.rel_cs_list_:
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
                var.inc_acq_cnt(cnt[var])

        for var in Variable.variable_pool.values():
            var.set_ave_occ()

    def _lp_encode_rel(self):
        for constraint in self.rel_constraints_:
            lp_var_list = [var.as_lp_rel() for var in constraint if not var.is_marked_acq()]
            if not lp_var_list:
                continue
            penalty = LpBuilder.var(f'Penalty{len(self.penalty_vars_)}', up_bound=100)
            self.penalty_vars_.append(penalty)
            lp_var_list.append(penalty)
            self.prob_.add_constraint(LpBuilder.constraint_sum_geq_weight_1(lp_var_list, 100))

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

    def _lp_encode_all_vars(self):
        #
        # For each variable/location, P_rel + P_acq < 100?
        #
        for var in Variable.variable_pool.values():
            self.prob_.add_constraint(LpBuilder.constraint_sum_leq_weight_1([var.as_lp_acq(), var.as_lp_rel()], 100))

    def _lp_encode_all_vars_heuristic(self):

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
            lhs = [v.as_lp_acq() for v in l]
            rhs = [v.as_lp_rel() for v in l]
            penalty1 = LpBuilder.var(f'ClassPenalty{len(self.classname_penalty_vars_)}', up_bound=5000)
            self.classname_penalty_vars_.append(penalty1)
            lhs.append(penalty1)

            penalty2 = LpBuilder.var(f'ClassPenalty{len(self.classname_penalty_vars_)}', up_bound=5000)
            self.classname_penalty_vars_.append(penalty2)
            rhs.append(penalty2)
            self.prob_.add_constraint(LpBuilder.constraint_vars_eq(lhs,rhs))

    def _lp_ave_occ_weight(self, x):
        return 0.2 * x

    def _lp_encode_object_func(self):
        #
        # Object function: min(penalty +  k * all variables)
        #
        obj_func = {v : 1 for v in self.penalty_vars_}

        k = 0.2

        for var in Variable.variable_pool.values():
            obj_func[var.as_lp_acq()] = k * (1 + self._lp_ave_occ_weight(var.acq_ave_) + var.acq_time_gap_score())
            obj_func[var.as_lp_rel()] = k * (1 + self._lp_ave_occ_weight(var.rel_ave_))


        for lpv in self.classname_penalty_vars_:
            obj_func[lpv] = k / 2

        obj = flipy.LpObjective(expression=obj_func, sense=flipy.Minimize)

        # obj = flipy.LpObjective(expression={v: 1 for v in obj_func_vars}, sense=flipy.Minimize)

        self.prob_.set_objective(obj)

    def print_debug_info(self):

        #
        # print the cnt of occurence of variable in constrains
        #
        '''
        for var in Variable.variable_pool.values():

            l = var.time_gaps_
            rel_occ_score = self._lp_ave_occ_weight(var.rel_ave_)
            acq_occ_score = self._lp_ave_occ_weight(var.acq_ave_)
            ave_time_gap = round(sum(l)/len(l),2)
            variance_time_gap = round(math.sqrt(sum((i - ave_time_gap) ** 2 for i in l) / len(l)),2)
            #variance_score = round(max(0.5 - variance_time_gap/(2*ave_time_gap),0),2)

            print(var.uid_,"R:",len(var.rel_occ_),"Roccw:",round(rel_occ_score,2),"RV",var.as_lp_rel().evaluate(),"A:",len(var.acq_occ_),"Aave:",round(acq_occ_score,2),"AV",var.as_lp_acq().evaluate() ,var.description_,f'[{ave_time_gap},{variance_time_gap}]',f"R = {var.is_read_},W = {var.is_write_}")
        '''

        #
        # print the protection information
        #
        '''
        print("Protection information")
        rels, acqs = self.return_result()
        for var in rels:
            print(var.description_)
            for vl in self.rel_constraints_:
                if vl.include(var):
                    print('   ',vl.objid_)

        for var in acqs:
            print(var.description_)
            for vl in self.acq_constraints_:
                if vl.include(var):
                    print('   ',vl.objid_)
        #'''

        return

    def save_info(self, dir: str):
        # dump the check point to specific directory
        # the next time we can load the from the checkpoint

        #Variables
        with open(os.path.join(dir,'vars.lp'), 'w+') as fvars:
            for var in Variable.variable_pool.values():
                fvars.write(var.to_checkpoint() + "\n")

        #print("checkpointing rel " + str(len(self.rel_constraints_)))
        #print("checkpointing acq " + str(len(self.acq_constraints_)))
        #Constraints
        with open(os.path.join(dir,'constraints.lp'),'w+') as fcons:

            for vl in self.rel_constraints_:
                fcons.write(f'R {vl.to_checkpoint()}\n')
            for vl in self.acq_constraints_:
                fcons.write(f'A {vl.to_checkpoint()}\n')

        #Results
        rel_vars, acq_vars = self.return_result()
        with open(os.path.join(dir,'rel_vars.lp'),'w+') as frel:

            for var in rel_vars:
                frel.write(str(var.uid_) + " " + var.description_ + " " + str(var.is_confirmed_) + "\n")

        with open(os.path.join(dir,'acq_vars.lp'),'w+') as facq:
            for var in acq_vars:
                facq.write(str(var.uid_) + " " + var.description_ + " " + str(var.is_confirmed_) + "\n")

        with open(os.path.join(dir,'concurrent_sigs.lp'),'w+') as fcsig:
            for sig in self.concurrent_sigs:
                fcsig.write(sig + "\n")

        return

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

    def print_basic(self):
        print("Variable Definition")
        print("releasing window constraints ", len(self.rel_constraints_))
        print("acquiring window constraints ", len(self.acq_constraints_))
        print("variable size : ",len(Variable.variable_pool))
        #self.print_debug_info()
        print('Solving Status:', self.status)
        print('Concurrent signatures:')
        for i in self.concurrent_sigs:
            print(i)

    def print_compare_result(self, req_vars: List[Variable], acq_vars: List[Variable]):
        self.print_basic()
        l1, l2 = self.return_result()
        print("Releasing sites: ")
        for var in l1:
            if var.is_confirmed_:
                print(f'{var.description_} => {var.as_str_rel()} CONFIRMED')
            else:
                pair = [i for i in req_vars if i.description_ == var.description_]
                if len(pair):
                    print(f'{var.description_} => {var.as_str_rel()} OLD')
                else:
                    print(f'{var.description_} => {var.as_str_rel()} NEW')

        for var in req_vars:
            pair = [i for i in l1 if i.description_ == var.description_]
            if not len(pair):
                print(f'{var.description_} => {var.as_str_rel()} DROP')

        print()
        print("Acquiring sites: ")
        for var in l2:
            if var.is_confirmed_:
                print(f'{var.description_} => {var.as_str_acq()} CONFIRMED')
            else:
                pair = [i for i in acq_vars if i.description_ == var.description_]
                if len(pair):
                    print(f'{var.description_} => {var.as_str_acq()} OLD')
                else:
                    print(f'{var.description_} => {var.as_str_acq()} NEW')

        for var in acq_vars:
            pair = [i for i in l2 if i.description_ == var.description_]
            if not len(pair):
                print(f'{var.description_} => {var.as_str_acq()} DROP')

    def print_result(self):
        self.print_basic()
        print("Releasing sites :")
        for name, var in Variable.variable_pool.items():
            if (var.as_lp_rel().evaluate() >= 95):
                print(f'{var.description_} => {var.as_str_rel}')

        print("Acquiring sites :")
        for name, var in Variable.variable_pool.items():
            if (var.as_lp_acq().evaluate() >= 95):
                print(f'{var.description_} => {var.as_str_acq}')
