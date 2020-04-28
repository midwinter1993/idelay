package io.github.midwinter1993;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.CodeAttribute;
import javassist.expr.Expr;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.MonitorEnter;
import javassist.expr.MonitorExit;


class InstrEditor extends ExprEditor {
    private static final Logger logger = LogManager.getLogger("instrLog");

    @Override
    public void edit(FieldAccess f) throws CannotCompileException {
        if (!Constant.IS_INSTRUMENT_ACCESS) {
            return;
        }

        if (Filter.filterClass(f.getClassName())) {
            return;
        }

        final StringBuffer buffer = new StringBuffer();

        String beforeCode = beforeFieldAccessCallback(f);
        buffer.append("{")
                .append(beforeCode)
                .append(f.isReader() ? "$_ = $proceed();" : "$proceed($$);")
                .append("}");

        if (Constant.IS_LOG_INSTRUMENT) {
            logger.info("    [ Instrument access ] {}.{} @ {}",
                        f.getClassName(),
                        f.getFieldName(),
                        f.getFileName());
        }

        try {
            f.replace(buffer.toString());
        } catch (CannotCompileException e) {
            logger.info("      [ Cannot Compile Exception ]");
            logger.info("        [ Reason ] {}", e.getReason());
            // System.err.println(buffer.toString());
            throw e;
        }
    }

    private String beforeCallback(String signature, Expr e) {
        String location = String.format("(%s:%d)",
                                        e.getFileName(),
                                        e.getLineNumber());

        return String.format(signature, location);
    }

    private String beforeFieldAccessCallback(FieldAccess f) {
        String location = String.format("(%s:%d)",
                                        f.getFileName(),
                                        f.getLineNumber());
        String fieldName = String.format("%s.%s", f.getClassName(), f.getFieldName());
        if (f.isReader()) {
            return String.format(Constant.BEFORE_READ_SIGNATURE, fieldName, location);
        } else {
            return String.format(Constant.BEFORE_WRITE_SIGNATURE, fieldName, location);
        }
    }

    // ===========================================

    @Override
    public void edit(MethodCall mCall) throws CannotCompileException {
        //
        // Some special method call?? E.g., java.lang.Object.wait()
        //
        if (Filter.filterClass(mCall.getClassName())) {
            return;
        }

        String calledMethodName = null;
        CtMethod calledMethod = null;
        try {
            calledMethod = mCall.getMethod();
        } catch (NotFoundException e) {
            System.err.format("[ Get method failure ] for `%s`\n", mCall.getMethodName());
        }

        if (calledMethod != null) {
            calledMethodName = calledMethod.getLongName();
        } else {
            calledMethodName = mCall.getMethodName();
        }

        CalleeInfo calleeInfo = CalleeInfoPool.getByName(calledMethodName);
        if (calleeInfo == null) {
            calleeInfo = new CalleeInfo(calledMethodName);
            CalleeInfoPool.addCalleeInfo(calleeInfo);
        }

        if (calledMethod != null) {
            //
            // Non-synchronized method
            //
            if ((calledMethod.getMethodInfo().getAccessFlags() & AccessFlag.SYNCHRONIZED) == 0) {
                //
                // If a method is too small or too large,
                // we do not instrument the method call
                // Maximum bytecode size of a method to be inlined is 35
                //
                CodeAttribute code = calledMethod.getMethodInfo().getCodeAttribute();

                if (code != null) {
                int codeSize = code.getCodeLength();
                    if (Constant.IS_SKIP_SMALL_METHOD &&
                        (codeSize < 35 || codeSize > 128)) {
                        logger.info("    [ Skip call ] {} size: {}",
                                    calledMethodName,
                                    codeSize);
                        return;
                    }
                }
            } else {
                calleeInfo.setSynchronized();
            }
        }

        if (Constant.IS_LOG_INSTRUMENT) {
            logger.info("    [ Instrument call ] {}", calledMethodName);
        }
        String enterMethod = enterMethodCallback(calleeInfo, mCall);
        String exitMethod = exitMethodCallback(calleeInfo, mCall);
        insertCode(mCall, enterMethod, exitMethod);
    }

    private void insertCode(MethodCall mCall, String enterMethod, String exitMethod) throws CannotCompileException {
        final StringBuffer buffer = new StringBuffer();

        buffer.append("{")
                .append(enterMethod)
                .append("$_ = $proceed($$);")
                .append(exitMethod)
                .append("}");

        try {
            mCall.replace(buffer.toString());
        } catch (CannotCompileException e) {
            logger.info("      [ Cannot Compile Exception ] {} {}", mCall.getMethodName(), mCall.getSignature());
            logger.info("        [ Reason ] {}", e.getReason());
            // System.err.println(buffer.toString());
            throw e;
        }
    }

    private String enterMethodCallback(CalleeInfo calleeInfo, MethodCall mCall) {
        String callLocation = String.format("(%s:%d)",
                                            mCall.getFileName(),
                                            mCall.getLineNumber());

        return String.format(Constant.METHOD_ENTER_SIGNATURE,
                             calleeInfo.getUid(),
                             callLocation);
    }

    private String exitMethodCallback(CalleeInfo calleeInfo, MethodCall mCall) {
        String location = String.format("(%s:%d)",
                                            mCall.getFileName(),
                                            mCall.getLineNumber());

        return String.format(Constant.METHOD_ENTER_SIGNATURE,
                             calleeInfo.getUid(),
                             location);
    }

    // ===========================================

    @Override
    public void edit(MonitorEnter e) throws CannotCompileException {
        if (!Constant.IS_INSTRUMENT_MONITOR) {
            return;
        }

        final StringBuffer buffer = new StringBuffer();

        String beforeMonitorEnterCode = beforeCallback(Constant.MONITOR_ENTER_SIGNATURE, e);
        buffer.append("{")
              .append(beforeMonitorEnterCode)
              .append("$proceed();")
              .append("}");

        if (Constant.IS_LOG_INSTRUMENT) {
            logger.info("    [ Instrument monitor enter] {}:{}",
                        e.getFileName(),
                        e.getLineNumber());
        }

        try {
            e.replace(buffer.toString());
        } catch (CannotCompileException cce) {
            logger.info("      [ Cannot Compile Exception ]");
            logger.info("        [ Reason ] {}", cce.getReason());
            throw cce;
        }
    }

    @Override
    public void edit(MonitorExit e) throws CannotCompileException {
        if (!Constant.IS_INSTRUMENT_MONITOR) {
            return;
        }

        final StringBuffer buffer = new StringBuffer();

        String beforeMonitorExitCode = beforeCallback(Constant.MONITOR_EXIT_SIGNATURE, e);
        buffer.append("{")
              .append(beforeMonitorExitCode)
              .append("$proceed();")
              .append("}");

        if (Constant.IS_LOG_INSTRUMENT) {
            logger.info("    [ Instrument monitor exit] {}:{}",
                        e.getFileName(),
                        e.getLineNumber());
        }

        try {
            e.replace(buffer.toString());
        } catch (CannotCompileException cce) {
            logger.info("      [ Cannot Compile Exception ]");
            logger.info("        [ Reason ] {}", cce.getReason());
            throw cce;
        }
    }
}
