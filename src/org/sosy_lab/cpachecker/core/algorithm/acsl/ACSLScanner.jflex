package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java_cup.runtime.*;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

@javax.annotation.processing.Generated("JFlex")
@SuppressWarnings(value = {"all", "cast"})
%%

%class ACSLScanner
%unicode
%cup
%line
%column

%{
    StringBuffer buf = new StringBuffer();

    Queue<java_cup.runtime.Symbol> deq = new ArrayDeque<>(1);

    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }

    public java_cup.runtime.Symbol getNext() throws IOException {
        if (!deq.isEmpty()) {
            return deq.remove();
        } else {
            return next_token();
        }
    }

    private void addTokenToQueue(java_cup.runtime.Symbol sym) {
        deq.offer(sym);
    }
%}

LineBreak   = \r|\n|\r\n
Space       = \s|@
DecInt      = 0 | [1-9][0-9]*
String      = \".*\"
Identifier  = [_a-zA-Z][_a-zA-Z0-9]*

%state SINGLE_LINE_ANNOTATION, MULTI_LINE_ANNOTATION

%%

<YYINITIAL> {
   "//@"                {yybegin(SINGLE_LINE_ANNOTATION); return symbol(sym.NEXTCONTRACT);}
   "/*@"                {yybegin(MULTI_LINE_ANNOTATION); return symbol(sym.NEXTCONTRACT);}
   {LineBreak}          {/* do nothing */}
   .                    {/* do nothing */}
}

<SINGLE_LINE_ANNOTATION> {
    {LineBreak}         {yybegin(YYINITIAL);}
}

<MULTI_LINE_ANNOTATION> {
    "*/"                {yybegin(YYINITIAL);}
    {LineBreak}         {/* do nothing */}
}

<SINGLE_LINE_ANNOTATION, MULTI_LINE_ANNOTATION> {
    "\\true"            {return symbol(sym.TRUE);}
    "\\false"           {return symbol(sym.FALSE);}
    "["                 {return symbol(sym.LBRACKET);}
    "]"                 {return symbol(sym.RBRACKET);}
    "?"                 {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.QUESTION);}
    "!"                 {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.NEG);}
    "~"                 {return symbol(sym.BNEG);}
    "*"                 {return symbol(sym.STAR);}
    "/"                 {return symbol(sym.DIVIDE);}
    "%"                 {return symbol(sym.MOD);}
    "+"                 {return symbol(sym.PLUS);}
    "-"                 {return symbol(sym.MINUS);}
    "<<"                {return symbol(sym.LSHIFT);}
    ">>"                {return symbol(sym.RSHIFT);}
    "&"                 {return symbol(sym.AMPERSAND);}
    "|"                 {return symbol(sym.BOR);}
    "^"                 {return symbol(sym.BXOR);}
    "-->"               {return symbol(sym.BIMP);}
    "<-->"              {return symbol(sym.BEQV);}
    "&&"                {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.AND);}
    "||"                {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.OR);}
    "==>"               {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.IMP);}
    "<==>"              {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.EQV);}
    "^^"                {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.XOR);}
    "=="                {return symbol(sym.EQ);}
    "!="                {return symbol(sym.NEQ);}
    "<="                {return symbol(sym.LEQ);}
    ">="                {return symbol(sym.GEQ);}
    ">"                 {return symbol(sym.GT);}
    "<"                 {return symbol(sym.LT);}
    "("                 {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.LPAREN);}
    ")"                 {return symbol(sym.RPAREN);}
    "for"               {return symbol(sym.FOR);}
    "sizeof"            {return symbol(sym.SIZEOF);}
    "behavior"          {return symbol(sym.BEHAVIOR);}
    "complete behaviors"    {return symbol(sym.COMPLETE_BEHAVIORS);}
    "disjoint behaviors"    {return symbol(sym.DISJOINT_BEHAVIORS);}
    "ensures"           {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.ENS);}
    "requires"          {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.REQ);}
    "loop invariant"    {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.LINVARIANT);}
    "assert"            {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.ASSERT);}
    "check"             {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.CHECK);}
    "assumes"           {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.ASS);}
    ":"                 {addTokenToQueue(symbol(sym.PRED_START)); return symbol(sym.COLON);}
    ","                 {return symbol(sym.COMMA);}
    ";"                 {return symbol(sym.SEMI);}
    {DecInt}            {buf.setLength(0);
                        return symbol(sym.LITERAL, Integer.valueOf(buf.append(yytext()).toString()));}
    {Identifier}        {buf.setLength(0);
                        return symbol(sym.IDENTIFIER, buf.append(yytext()).toString());}
    {String}            {buf.setLength(0);
                        return symbol(sym.STRING_LITERAL, buf.append(yytext()).toString());}
    {Space}             {/* do nothing */}
}

    [^]                 {throw new Error("Illegal character: <" + yytext() + ">");}