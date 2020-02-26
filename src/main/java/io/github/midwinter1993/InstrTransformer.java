package io.github.midwinter1993;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class InstrTransformer implements ClassFileTransformer {
    private static final Logger logger = LogManager.getLogger("instrLog");

    @Override
    public byte[] transform(ClassLoader classLoader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] bytes) throws IllegalClassFormatException {

        if (!Filter.filterClass(className)) {
            //
            // If the class loader is different to our Runtime,
            // ClassNotFound exception will be thrown after instrumenting.
            //
            if (!InstrRuntime.class.getClassLoader().equals(classLoader)) {
                logger.info("[ Different loader ] {} loading {}", classLoader, className);
                return null;
            }

            if (Constant.logInstrument) {
                // System.out.format("[ Instrument class ] %s\n", className);
                logger.info("[ Instrument class ] {}", className);
            }
            // Javassist
            try {

                ClassPool cp = ClassPool.getDefault();
                // CtClass cc = cp.get(className.replace('/', '.'));
                CtClass cc = cp.makeClass(new ByteArrayInputStream(bytes));

                if (cc.isInterface()) {
                    logger.info("[ NOT Instrument interface ] {}", className);
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
                        logger.info("  [ Instrument method ] {}", name);
                    }

                    try {
                        method.instrument(new InstrMethodCall());
                    } catch (CannotCompileException cce) {
                        System.err.format("Instrument&Compile failure `%s`\n", name);
                    }
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