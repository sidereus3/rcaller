/*
 *
 RCaller, A solution for calling R from Java
 Copyright (C) 2010-2016  Mehmet Hakan Satman

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Mehmet Hakan Satman - mhsatman@yahoo.com
 * http://www.mhsatman.com
 * Google code project: https://github.com/jbytecode/rcaller
 * Please visit the blog page with rcaller label:
 * http://stdioe.blogspot.com.tr/search/label/rcaller
 */
package com.github.rcaller.scriptengine;

import com.github.rcaller.EventHandler;
import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCode;
import com.github.rcaller.rstuff.ROutputParser;
import com.github.rcaller.util.CodeUtils;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class RCallerScriptEngine implements ScriptEngine, EventHandler, Invocable {

    private RCaller rcaller;
    private RCode rcode;
    private ROutputParser parser;

    public RCallerScriptEngine() {
        rcaller = RCaller.create();
        rcode = new RCode();

        rcode.addRCode("result <- list(a=0)");
        rcaller.setRCode(rcode);
        rcaller.runAndReturnResultOnline("result");
    }

    @Override
    public Object eval(String code, ScriptContext sc) throws ScriptException {
        return (this.eval(code));
    }

    @Override
    public Object eval(Reader reader, ScriptContext sc) throws ScriptException {
        return (this.eval(reader));
    }

    @Override
    public Object eval(String code) throws ScriptException {
        rcode.clearOnline();
        rcode.addRCode(code);
        rcode.addRCode("result <- list(a=0)");
        rcaller.setRCode(rcode);
        rcaller.runAndReturnResultOnline("result");
        return (null);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            while (true) {
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                stringBuilder.append(line);
            }
        } catch (IOException ioe) {
            throw new ScriptException("Error while reading from reader: " + ioe.toString());
        }
        return (this.eval(stringBuilder.toString()));
    }

    @Override
    public Object eval(String code, Bindings bndngs) throws ScriptException {
        return (this.eval(code));
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return (this.eval(reader));
    }

    @Override
    public void put(String name, Object o) {
        rcode.clearOnline();
        StringBuffer code = new StringBuffer();
        CodeUtils.addRespectToType(code, name, o, false);
        rcode.addRCode("result <- list(a=0)");
        rcode.setCode(code);

        rcaller.setRCode(rcode);
        rcaller.runAndReturnResultOnline("result");
    }

    @Override
    public Object get(String var) {
        int[] dimension;
        rcode.clearOnline();
        rcode.addRCode("result <- ls()");
        rcaller.runAndReturnResultOnline(var);
        parser = rcaller.getParser();
        parser.parse();
        try {
            dimension = parser.getDimensions(var);
        } catch (Exception e) {
            return (parser.getAsStringArray(var));
        }
        String vartype = parser.getType(var);
        if (dimension[0] > 1 && dimension[1] > 1) {
            return (parser.getAsDoubleMatrix(var));
        } else if (vartype.equals("numeric")) {
            return (parser.getAsDoubleArray(var));
        } else if (vartype.equals("character")) {
            return (parser.getAsStringArray(var));
        } else {
            return (parser.getAsStringArray(var)); // :o
        }
    }

    @Override
    public Bindings getBindings(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setBindings(Bindings bndngs, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Bindings createBindings() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScriptContext getContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setContext(ScriptContext sc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScriptEngineFactory getFactory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void messageReceived(String senderName, String msg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void close() {
        this.rcaller.StopRCallerOnline();
    }

    /**
    Methods for invokable interface
     */
    @Override
    public Object invokeMethod(Object o, String string, Object... os) throws ScriptException, NoSuchMethodException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object invokeFunction(String fname, Object... arguments) throws ScriptException, NoSuchMethodException {
        int[] dimension = null;
        String var = "fresult";

        rcode.clearOnline();
        rcode.addRCode(var + " <- " + fname + "(");
        for (int i = 0; i < arguments.length; i++) {
            NamedArgument named = (NamedArgument) arguments[i];
            CodeUtils.addRespectToType(rcode.getCode(), named.getName(), named.getO(), true);
            if (i != (arguments.length - 1)) {
                rcode.addRCode(",");
            }
        }
        rcode.addRCode(")");
        rcaller.setRCode(rcode);
        rcaller.runAndReturnResultOnline(var);

        ArrayList<NamedArgument> namedResults = new ArrayList<>();
        ArrayList<String> names = rcaller.getParser().getNames();
        for (String name : names) {
            var = name;
            try {
                dimension = parser.getDimensions(var);
            } catch (Exception e) {
                namedResults.add(NamedArgument.Named(var, parser.getAsStringArray(var)));
            }
            String vartype = parser.getType(var);
            assert dimension != null;
            if (dimension[0] > 1 && dimension[1] > 1) {
                namedResults.add(NamedArgument.Named(var, parser.getAsDoubleMatrix(var)));
            } else if (vartype.equals("numeric")) {
                namedResults.add(NamedArgument.Named(var, parser.getAsDoubleArray(var)));
            } else if (vartype.equals("character")) {
                namedResults.add(NamedArgument.Named(var, parser.getAsStringArray(var)));
            } else {
                namedResults.add(NamedArgument.Named(var, parser.getAsStringArray(var)));
            }
        }
        return (namedResults);
    }

    @Override
    public <T> T getInterface(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T getInterface(Object o, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
