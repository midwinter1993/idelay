package io.github.midwinter1993;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.CodeAttribute;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

class InstrMethodCall extends ExprEditor {
    private static final Logger logger = LogManager.getLogger("instrLog");

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
                    if (codeSize < 35 || codeSize > 128) {
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

        if (Constant.logInstrument) {
            logger.info("    [ Instrument call ] {}", calledMethodName);
        }
        String beforeCallCode = enterMethodCallback(calleeInfo, mCall);
        insertCode(mCall, beforeCallCode);
    }

    private void insertCode(MethodCall mCall, String beforeCallCode) throws CannotCompileException {
        final StringBuffer buffer = new StringBuffer();

        buffer.append("{")
                .append(beforeCallCode)
                .append("$_ = $proceed($$);")
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

    private String threadStartCallback(MethodCall mCall) {
        String callLocation = String.format("Thread create(%s:%d)",
                                            mCall.getFileName(),
                                            mCall.getLineNumber());

        return String.format(Constant.THREAD_START_SIGNATURE,
                             callLocation);
    }

    private String threadJoinCallback(MethodCall mCall) {
        String callLocation = String.format("Thread join(%s:%d)",
                                            mCall.getFileName(),
                                            mCall.getLineNumber());

        return String.format(Constant.THREAD_JOIN_SIGNATURE,
                             callLocation);
    }
}
