package javassist.expr;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.JvstTypeChecker;
import javassist.compiler.ProceedHandler;
import javassist.compiler.ast.ASTList;

public class MonitorEnter extends Expr {
   protected MonitorEnter(int pos, CodeIterator i, CtClass declaring, MethodInfo m) {
      super(pos, i, declaring, m);
   }

   public CtBehavior where() {
      return super.where();
   }

   public int getLineNumber() {
      return super.getLineNumber();
   }

   public String getFileName() {
      return super.getFileName();
   }

   public CtClass[] mayThrow() {
      return super.mayThrow();
   }

   public void replace(String statement) throws CannotCompileException {
      this.thisClass.getClassFile();
      ConstPool constPool = this.getConstPool();
      int pos = this.currentPos;
      Javac jc = new Javac(this.thisClass);
      CodeAttribute ca = this.iterator.get();

      try {
         CtClass[] params = new CtClass[0];
         int paramVar = ca.getMaxLocals();
         jc.recordParams("java.lang.Object", params, true, paramVar, this.withinStatic());
         jc.recordProceed(new MonitorEnter.Proceed(paramVar));
         Bytecode bytecode = jc.getBytecode();
         storeStack(params, false, paramVar, bytecode);
         jc.recordLocalVariables(ca, pos);
         jc.compileStmnt(statement);
         this.replace0(pos, bytecode, 1);
      } catch (CompileError var9) {
         throw new CannotCompileException(var9);
      } catch (BadBytecode var10) {
         throw new CannotCompileException(var10);
      }
   }

   static class Proceed implements ProceedHandler {
      int targetVar;

      Proceed(int var) {
         this.targetVar = var;
      }

      public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args) throws CompileError {
         if (args != null && !gen.isParamListName(args)) {
            throw new CompileError("$proceed() cannot take a parameter for monitor enter");
         } else {
            bytecode.addAload(this.targetVar);
            bytecode.addOpcode(194);
            gen.setType(CtClass.voidType);
         }
      }

      public void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError {
         c.setType(CtClass.voidType);
      }
   }
}
