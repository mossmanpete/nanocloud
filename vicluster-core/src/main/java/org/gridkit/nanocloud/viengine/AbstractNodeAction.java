package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class AbstractNodeAction implements NodeAction {

    private static ThreadLocal<PragmaWriter> context = new ThreadLocal<PragmaWriter>();
    
    private List<SimpleInArg> args = new ArrayList<SimpleInArg>();
    
 
    @SuppressWarnings("unchecked")
    protected <T> InArg<T> required(String... key) {
        if (key.length == 0) {
            throw new IllegalArgumentException();
        }
        SimpleInArg ia = new SimpleInArg();
        ia.keys = key;
        ia.required = true;
        args.add(ia);
        return (InArg<T>)ia;
    }

    protected <T> InArg<T> optional(String key) {
        return optional(key, null);
    }

    protected <T> InArg<T> optional(String key, T defaultValue) {
        return optional(new String[]{key}, defaultValue);
    }

    @SuppressWarnings("unchecked")
    protected <T> InArg<T> optional(String[] keys, T defaultValue) {
        if (keys.length == 0) {
            throw new IllegalArgumentException();
        }
        SimpleInArg ia = new SimpleInArg();
        ia.keys = keys;
        ia.required = false;
        ia.defaultValue = defaultValue;
        args.add(ia);
        return (InArg<T>) ia;
    }
    
    @Override
    public void run(PragmaWriter context) throws ExecutionException {
        AbstractNodeAction.context.set(context);
        try {
            boolean ready = true;
            for(SimpleInArg ia: args) {
                ready &= ia.verify(context);
            }
            if (ready || shouldRunIncomlete()) {
                run();
            }
            else {
                BootAnnotation.fatal(context, "Required params are missing for '%s'", this);
            }
        }
        finally {
            AbstractNodeAction.context.set(null);
        }
    }

    protected boolean shouldRunIncomlete() {
        return false;
    }

    protected PragmaWriter getContext() {
        return context.get();
    }
    
    protected abstract void run();

    @Override
    public String toString() {
        return getClass().getSimpleName();               
    }
    
    protected interface InArg<T> {
        
        public T get();
        
    }
    
    protected class SimpleInArg implements InArg<Object> {

        private String[] keys;
        private boolean required;

        private Object defaultValue;
        
        boolean verify(PragmaWriter context) {
            Object value = null;
            for(String key: keys) {
                value = context.get(key);
                if (value != null) {
                    return true;
                }
                else {
                    BootAnnotation.warning(context, "Key not found '%s'", key);
                }
            }
            if (required) {
                return false;
            }
            else {
                value = defaultValue;
                return true;
            }
        }
        
        @Override
        public Object get() {
            Object value = null;
            for(String key: keys) {
                value = context.get().get(key);
                if (value != null) {
                    return value;
                }
            }
            return defaultValue;
        }
    }
}
