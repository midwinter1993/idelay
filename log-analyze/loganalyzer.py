
import argparse
import os
from parse import load_log
from system import ConstainedSystem

def organize_by_objid(threadlog):
    objidlog = {}

    for f in threadlog:
        for log_entry in threadlog[f]:
            if log_entry.object_id_ not in objidlog :
                objidlog[log_entry.object_id_] = []
            objidlog[log_entry.object_id_].append(log_entry)
            log_entry.thread_id_ = f

    for obj in objidlog:
        objidlog[obj].sort(key = lambda x : x.tsc_)

    #debug output
    #for obj in objidlog:
    #    print(obj, " : ")
    #    for log_entry in objidlog[obj]:
    #        print("       ", log_entry)

    return objidlog

def near_miss_detection(threadlog, objidlog):
    #rconstrains = []
    #aconstrains = []
    cs = ConstainedSystem()

    for obj in objidlog:
        l = objidlog[obj]
        n = len(l)

        for i in range(n):
            for j in range(i-1,-1,-1):
                if (close_enough(l[i].tsc_,l[j].tsc_)):
                    if ((l[i].thread_id_ != l[j].thread_id_) and (l[i].isWrite() or l[j].isWrite())):
                        #print("Found nearmiss :",l[i],l[j])
                        #rconstrains.append(for l in threadlog[l[j].thread_id_] if (l.tsc_>=l[j].tsc_)and(l.tsc_<=l[i].tsc_)and(l is not l[j]))
                        #aconstrains.append(for l in threadlog[l[j].thread_id_] if (l.tsc_>=l[j].tsc_)and(l.tsc_<=l[i].tsc_)and(l is not l[j]))
                        rlist = [entry for entry in threadlog[l[j].thread_id_] if (entry.tsc_>=l[j].tsc_)and(entry.tsc_<=l[i].tsc_)and(entry is not l[j])]
                        alist = [entry for entry in threadlog[l[i].thread_id_] if (entry.tsc_>=l[j].tsc_)and(entry.tsc_<=l[i].tsc_)and(entry is not l[i])]
                        cs.load_require_constrain(rlist)
                        cs.load_acquire_constrain(alist)
                else:
                    break

    return cs 

def close_enough(x, y):
    distance = 10000000
    return ((x<y) and (y< x+ distance)) or ((x>y) and (x < y+distance))

if __name__ == "__main__":
    dirparser = argparse.ArgumentParser()
    dirparser.add_argument('--dir', help = 'the log directory')
    args = dirparser.parse_args()
    print(args.dir)

    log_files = [f for f in os.listdir(args.dir) if f.endswith(".litelog")]
    print("found log files size : ",len(log_files))

    #load the lite log by threadID
    #to be paralleled
    threadlog = {f : load_log(os.path.join(args.dir,f)) for f in log_files}
    for f in threadlog:
        print(f, " log size:", len(threadlog[f]))

    #orginize the log by objectID
    #to be paralleled
    objidlog = organize_by_objid(threadlog)

    #search the nearmiss
    #to be paralleled
    constrains = near_miss_detection(threadlog, objidlog)
    constrains.print_system()



    
