package io.github.midwinter1993;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javassist.CannotCompileException;
import javassist.NotFoundException;
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

        // System.out.println("======" + mCall.getSignature());
        String calledMethodName = null;
        try {
            // System.out.println("======" + mCall.getMethod().getLongName());
            calledMethodName = mCall.getMethod().getLongName();
        } catch (NotFoundException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            System.err.println("    [Get method long name failure]");
            calledMethodName = mCall.getMethodName();
        }

        if (calledMethodName.equals("java.lang.Thread.start()")) {
            String beforeCallCode = threadStartCallback(mCall);
            insertCode(mCall, beforeCallCode);
        } else if (calledMethodName.equals("java.lang.Thread.join()")) {
            String beforeCallCode = threadJoinCallback(mCall);
            insertCode(mCall, beforeCallCode);
        } else {
            if (Constant.logInstrument) {
                logger.info("    [ Instrument call ] {}", calledMethodName);
            }
            String beforeCallCode = enterMethodCallback(calledMethodName, mCall);
            insertCode(mCall, beforeCallCode);
        }
    }

    private void insertCode(MethodCall mCall, String beforeCallCode) {
        final StringBuffer buffer = new StringBuffer();

        buffer.append("{")
                .append(beforeCallCode)
                .append("$_ = $proceed($$);")
                .append("}");

        try {
            mCall.replace(buffer.toString());
        } catch (Exception e) {
            System.err.println(buffer.toString());
            e.printStackTrace();
        }
    }

    private String enterMethodCallback(String calledMethodLongName, MethodCall mCall) {
        String callLocation = String.format("%s(%s:%d)",
                                            calledMethodLongName,
                                            mCall.getFileName(),
                                            mCall.getLineNumber());

        return String.format(Constant.METHOD_ENTER_SIGNATURE,
                             calledMethodLongName,
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
