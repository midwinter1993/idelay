package io.github.midwinter1993;

import com.google.ortools.linearsolver.MPVariable;

public class MPVariablePair {
    private SyncVar[] vars = new SyncVar[2];
    private SyncType[] types = new SyncType[2];

    public MPVariablePair(SyncVar first, SyncType firstType,
                          SyncVar second, SyncType secondType) {
        vars[0] = first;
        types[0] = firstType;
        vars[1] = second;
        types[1] = secondType;
    }

    public MPVariable getFirst() {
        switch (types[0]) {
            case RELEASE:
                return vars[0].asLpRel();
            case ACQUIRE:
                return vars[0].asLpAcq();
            default:
                break;
        }
        System.err.println("Sync Type Error");
        return null;
    }

    public MPVariable getSecond() {
        switch (types[1]) {
            case RELEASE:
                return vars[1].asLpRel();
            case ACQUIRE:
                return vars[1].asLpAcq();
            default:
                break;
        }
        System.err.println("Sync Type Error");
        return null;
    }

    private int code = 0;

    @Override
    public int hashCode() {
        if (code == 0) {
            int x = 0;

            x |= (types[0] == SyncType.RELEASE ? 1 : 0);
            x |= ((types[1] == SyncType.RELEASE ? 1 : 0) << 1);

            x |= (vars[0].hashCode() << 2);
            x |= (vars[0].hashCode() << 17);

            code = x;
        }
        // System.out.println(Integer.toBinaryString(code));
        return code;
    }

    @Override
    public boolean equals(Object o) {
        // if (!(o instanceof MPVariablePair)) {
            // return false;
        // }
        MPVariablePair another = (MPVariablePair)o;

        if (types[0] == another.types[0] && types[1] == another.types[1] &&
            vars[0].equals(another.vars[0]) && vars[1].equals(vars[1])) {
            return true;
        }
        return false;
    }
}