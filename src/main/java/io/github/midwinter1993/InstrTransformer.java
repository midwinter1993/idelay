package io.github.midwinter1993;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

class InstrMethodCall extends ExprEditor {
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

        if (Constant.logInstrument) {
            System.out.format("    [ Instrument call ] %s\n", calledMethodName);
        }

        String callLocation = String.format("%s(%s:%d)",
                                            calledMethodName,
                                            mCall.getFileName(),
                                            mCall.getLineNumber());

        String beforeCallCode = String.format("io.github.midwinter1993.InstrRuntime.enterMethod($0, \"%s\", \"%s\", \"%s\");",
                                              calledMethodName,
                                              mCall.getSignature(),
                                              callLocation);
        // String afterCallCode = String.format("io.github.midwinter1993.InstrRuntime.exitMethod($0);");

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
}

class InstrTransformer implements ClassFileTransformer {


    @Override
    public byte[] transform(ClassLoader classLoader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] bytes) throws IllegalClassFormatException {

        if (!Filter.filterClass(className)) {
            if (Constant.logInstrument) {
                System.out.format("[ Instrument class ] %s\n", className);
            }
            // Javassist
            try {

                ClassPool cp = ClassPool.getDefault();
                // CtClass cc = cp.get(className.replace('/', '.'));
                CtClass cc = cp.makeClass(new ByteArrayInputStream(bytes));

                if (cc.isInterface()) {
                    System.out.format("[ NOT Instrument interface ] %s\n", className);
                    return null;
                }

                // if (cc.isFrozen()) {
                // }

                // CtMethod[] methods = cc.getDeclaredMethods();
                CtMethod[] methods = cc.getMethods();

                for (CtMethod method: methods) {
                    String name = method.getLongName();
                    if (Filter.filterMethod(name)) {
                        continue;
                    }

                    if (Constant.logInstrument) {
                        System.out.format("  [ Instrumenting method ] %s\n", name);
                    }

                    method.instrument(new InstrMethodCall());
                }
                byte[] byteCode = cc.toBytecode();
                cc.detach();
                return byteCode;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }
}