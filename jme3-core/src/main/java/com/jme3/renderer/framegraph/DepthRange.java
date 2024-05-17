/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class DepthRange implements Savable {
    
    public static final DepthRange IDENTITY = new DepthRange();
    public static final DepthRange FRONT = new DepthRange(0, 0);
    public static final DepthRange REAR = new DepthRange(1, 1);
    
    private float start, end;

    public DepthRange() {
        set(0, 1);
    }
    public DepthRange(float start, float end) {
        set(start, end);
    }
    public DepthRange(DepthRange range) {
        set(range);
    }
    
    public final DepthRange set(float start, float end) {
        validateRange(start, end);
        this.start = start;
        this.end = end;
        return this;
    }
    
    public final DepthRange set(DepthRange range) {
        // no need to validate range here
        start = range.start;
        end = range.end;
        return this;
    }
    
    public final DepthRange setStart(float start) {
        validateRange(start, end);
        this.start = start;
        return this;
    }
    
    public final DepthRange setEnd(float end) {
        validateRange(start, end);
        this.end = end;
        return this;
    }
    
    public float getStart() {
        return start;
    }
    
    public float getEnd() {
        return end;
    }
    
    private void validateRange(float start, float end) {
        if (start > end) {
            throw new IllegalStateException("Depth start cannot be beyond depth end.");
        }
        if (start > 1 || start < 0 || end > 1 || end < 0) {
            throw new IllegalStateException("Depth parameters must be between 0 and 1 (inclusive).");
        }
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof DepthRange)) {
            return false;
        }
        DepthRange obj = (DepthRange)object;
        return start == obj.start && end == obj.end;
    }
    
    public boolean equals(DepthRange range) {
        return range != null && start == range.start && end == range.end;
    }
    
    public boolean equals(float start, float end) {
        return this.start != start && this.end != end;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Float.floatToIntBits(this.start);
        hash = 79 * hash + Float.floatToIntBits(this.end);
        return hash;
    }
    @Override
    public String toString() {
        return "DepthRange["+start+" -> "+end+"]";
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(start, "start", 0);
        out.write(end, "end", 1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        start = in.readFloat("start", 0);
        end = in.readFloat("end", 1);
    }
    
}
