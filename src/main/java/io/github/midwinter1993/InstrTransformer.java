package io.github.midwinter1993;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;


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
                        System.out.format("  [ Instrument method ] %s\n", name);
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