package io.github.midwinter1993;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;
import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;


public class InstrTransformer implements ClassFileTransformer {
    private static final LiteLogger logger = LiteLogger.getLogger("instr.log");

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
                logger.info("[ Different loader ] %s loading %s", classLoader, className);
                if (classLoader == null) {
                    return null;
                }

                // System.out.println("==========");

                // System.out.println("System classloader");
                // $.dumpClassLoader(ClassLoader.getSystemClassLoader());
                // System.out.println("----------");

                // System.out.println("Runtime classloader");
                // $.dumpClassLoader(InstrRuntime.class.getClassLoader());
                // System.out.println("----------");

                // System.out.println("current classloader");
                // $.dumpClassLoader(classLoader);
                // return null;
            }

            if (Constant.IS_LOG_INSTRUMENT) {
                // System.out.format("[ Instrument class ] %s\n", className);
                logger.info("[ Instrument class ] %s", className);
            }
            // Javassist
            try {

                // ClassPool cp = new ClassPool();
                ClassPool cp = ClassPool.getDefault();
                cp.appendClassPath(new LoaderClassPath(classLoader));
                // cp.appendClassPath(new LoaderClassPath(InstrRuntime.class.getClassLoader()));

                CtClass cc = cp.makeClass(new ByteArrayInputStream(bytes));

                if (cc.isInterface()) {
                    logger.info("[ NOT Instrument interface ] %s", className);
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

                    if (Constant.IS_LOG_INSTRUMENT) {
                        logger.info("  [ Instrument method ] %s", name);
                    }

                    try {
                        method.instrument(new InstrEditor());
                    } catch (CannotCompileException cce) {
                        System.err.format("[ Instrument&Compile failure in method ] `%s`\n", name);
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