/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.util.NativeObject;
import java.util.BitSet;
import java.util.function.Consumer;

/**
 *
 * @author codex
 * @param <T>
 */
public class RenderObject <T> {
    
    private static final Consumer<Object> DEFAULT = object -> {};
    private static final Consumer<NativeObject> NATIVE = object -> object.dispose();
    
    private static long nextId = 0;
    
    private final long id;
    private final T object;
    private final BitSet reservations = new BitSet();
    private int timeoutDuration;
    private int timeout = 0;
    private boolean acquired = false;
    private boolean constant = true;
    private Consumer disposer;

    public RenderObject(ResourceDef<T> def, T object, int timeout) {
        this.id = nextId++;
        if (object == null) {
            throw new NullPointerException("Object cannot be null.");
        }
        this.object = object;
        this.timeoutDuration = def.getStaticTimeout();
        if (this.timeoutDuration < 0) {
            this.timeoutDuration = timeout;
        }
        disposer = def.getDisposalMethod();
        if (disposer != null);
        else if (object instanceof NativeObject) {
            this.disposer = NATIVE;
        } else {
            this.disposer = DEFAULT;
        }
    }
    
    public void acquire() {
        timeout = timeoutDuration;
        acquired = true;
    }
    public void release() {
        if (!acquired) {
            throw new IllegalStateException("Already released.");
        }
        acquired = false;
    }
    public void reserve(int index) {
        reservations.set(index);
    }
    public void dispose() {
        disposer.accept(object);
    }
    
    public void clearReservations() {
        reservations.clear();
    }
    public boolean tickTimeout() {
        return timeout-- > 0;
    }
    
    public void setConstant(boolean constant) {
        this.constant = constant;
    }
    
    public long getId() {
        return id;
    }
    public T getObject() {
        return object;
    }
    public boolean isAcquired() {
        return acquired;
    }
    public boolean isReservedAt(int index) {
        return reservations.get(index);
    }
    public boolean isReservedWithin(TimeFrame frame) {
        if (frame.getStartIndex() >= reservations.size()) {
            return false;
        }
        int n = Math.min(reservations.size()-1, frame.getEndIndex());
        for (int i = frame.getStartIndex(); i <= n; i++) {
            if (reservations.get(i)) {
                return true;
            }
        }
        return false;
    }
    public boolean isConstant() {
        return constant;
    }
    
    public static long getNextId() {
        return nextId;
    }
    
}
