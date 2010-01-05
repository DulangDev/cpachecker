package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class FunctionExit implements Filter {

  String mFuncName;
  
  public FunctionExit(String pFuncName) {
    assert(pFuncName != null);
    
    mFuncName = pFuncName;
  }
  
  public String getFunctionName() {
    return mFuncName;
  }
  
  @Override
  public String toString() {
    return "@EXIT(" + mFuncName + ")";
  }
  
  @Override
  public int hashCode() {
    return 4563 + mFuncName.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther != null) {
      return false;
    }
    
    if (pOther instanceof FunctionExit) {
      FunctionExit mFuncFilter = (FunctionExit)pOther;
      
      return mFuncName.equals(mFuncFilter.mFuncName);
    }
    
    return false;
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    pVisitor.visit(this);
  }

}
