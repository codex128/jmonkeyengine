/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.FgStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * 
 * @author codex
 */
public class FrameGraph implements Savable {
    
    private final AssetManager assetManager;
    private final ResourceList resources;
    private final FGRenderContext context;
    private final LinkedList<RenderPass> passes = new LinkedList<>();
    private boolean rendered = false;

    public FrameGraph(AssetManager assetManager, RenderManager renderManager) {
        this.assetManager = assetManager;
        this.resources = new ResourceList(renderManager.getRenderObjectMap());
        this.context = new FGRenderContext(this, renderManager);
    }
    
    /**
     * Configures the framegraph rendering context.
     * 
     * @param vp viewport to render (not null)
     * @param prof profiler (may be null)
     * @param tpf time per frame
     */
    public void configure(ViewPort vp, AppProfiler prof, float tpf) {
        context.target(vp, prof, tpf);
    }
    /**
     * Pre-frame operations.
     */
    public void preFrame() {
        for (RenderPass p : passes) {
            p.preFrame(context);
        }
    }
    /**
     * Post-queue operations.
     */
    public void postQueue() {
        for (RenderPass p : passes) {
            p.postQueue(context);
        }
    }
    /**
     * Executes this framegraph.
     * <p>
     * The overall execution step occurs in 4 stages:
     * <ol>
     *   <li>Preparation.</li>
     *   <li>Culling.</li>
     *   <li>Rendering (execution).</li>
     *   <li>Clean (reset).</li>
     * </ol>
     * 
     * @return true if this is the first execution this frame
     */
    public boolean execute() {
        // prepare
        ViewPort vp = context.getViewPort();
        AppProfiler prof = context.getProfiler();
        if (prof != null) prof.vpStep(VpStep.FrameGraphSetup, vp, null);
        if (!rendered) {
            resources.beginRenderingSession();
        }
        for (RenderPass p : passes) {
            if (prof != null) {
                prof.fgStep(FgStep.Prepare, p.getProfilerName());
            }
            p.prepareRender(context);
        }
        // cull passes and resources
        if (prof != null) prof.vpStep(VpStep.FrameGraphCull, vp, null);
        for (RenderPass p : passes) {
            p.countReferences();
        }
        resources.cullUnreferenced();
        // execute
        if (prof != null) prof.vpStep(VpStep.FrameGraphExecute, vp, null);
        context.pushRenderSettings();
        for (RenderPass p : passes) {
            if (p.isUsed()) {
                if (prof != null) {
                    prof.fgStep(FgStep.Execute, p.getProfilerName());
                }
                p.executeRender(context);
                context.popRenderSettings();
            }
        }
        context.popFrameBuffer();
        // reset
        if (prof != null) prof.vpStep(VpStep.FrameGraphReset, vp, null);
        for (RenderPass p : passes) {
            if (prof != null) {
                prof.fgStep(FgStep.Reset, p.getProfilerName());
            }
            p.resetRender(context);
        }
        // cleanup resources
        resources.clear();
        if (rendered) return false;
        else return (rendered = true);
    }
    /**
     * Should be called only when all rendering for the frame is complete.
     */
    public void renderingComplete() {
        // notify passes
        for (RenderPass p : passes) {
            p.renderingComplete();
        }
        // reset flags
        rendered = false;
    }
    
    /**
     * Adds the pass to end of the pass queue.
     * 
     * @param <T>
     * @param pass
     * @return given pass
     */
    public <T extends RenderPass> T add(T pass) {
        passes.addLast(pass);
        pass.initializePass(this, passes.size()-1);
        return pass;
    }
    /**
     * Adds the pass at the index in the pass queue.
     * <p>
     * If the index is &gt;= the current queue size, the pass will
     * be added to the end of the queue. Passes above the added pass
     * will have their indexes shifted.
     * 
     * @param <T>
     * @param pass
     * @param index
     * @return 
     */
    public <T extends RenderPass> T add(T pass, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative.");
        }
        if (index >= passes.size()) {
            return add(pass);
        }
        passes.add(index, pass);
        pass.initializePass(this, index);
        for (RenderPass p : passes) {
            p.shiftExecutionIndex(index, true);
        }
        return pass;
    }
    /**
     * Gets the first pass that is of or a subclass of the given class.
     * 
     * @param <T>
     * @param type
     * @return first qualifying pass, or null
     */
    public <T extends RenderPass> T get(Class<T> type) {
        for (RenderPass p : passes) {
            if (type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    /**
     * Gets the first pass of the given class that is named as given.
     * 
     * @param <T>
     * @param type
     * @param name
     * @return first qualifying pass, or null
     */
    public <T extends RenderPass> T get(Class<T> type, String name) {
        for (RenderPass p : passes) {
            if (name.equals(p.getName()) && type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    /**
     * Gets the pass that holds the given id number.
     * 
     * @param <T>
     * @param type
     * @param id
     * @return pass of the id, or null
     */
    public <T extends RenderPass> T get(Class<T> type, int id) {
        for (RenderPass p : passes) {
            if (id == p.getId() && type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    /**
     * Removes the pass at the index in the queue.
     * <p>
     * Passes above the removed pass will have their indexes shifted.
     * 
     * @param i
     * @return removed pass
     * @throws IndexOutOfBoundsException if the index is less than zero or &gt;= the queue size
     */
    public RenderPass remove(int i) {
        if (i < 0 || i >= passes.size()) {
            throw new IndexOutOfBoundsException("Index "+i+" is out of bounds for size "+passes.size());
        }
        int j = 0;
        RenderPass removed = null;
        for (Iterator<RenderPass> it = passes.iterator(); it.hasNext();) {
            RenderPass p = it.next();
            if (removed != null) {
                p.disconnectFrom(removed);
                p.shiftExecutionIndex(i, false);
            } else if (j++ == i) {
                removed = p;
                it.remove();
            }
        }
        if (removed != null) {
            removed.cleanupPass(this);
        }
        return removed;
    }
    /**
     * Removes the given pass from the queue.
     * <p>
     * Passes above the removed pass will have their indexes shifted.
     * 
     * @param pass
     * @return true if the pass was removed from the queue
     */
    public boolean remove(RenderPass pass) {
        int i = 0;
        boolean found = false;
        for (Iterator<RenderPass> it = passes.iterator(); it.hasNext();) {
            RenderPass p = it.next();
            if (found) {
                // shift execution indices down
                p.disconnectFrom(pass);
                p.shiftExecutionIndex(i, false);
                continue;
            }
            if (p == pass) {
                it.remove();
                found = true;
            }
            i++;
        }
        if (found) {
            pass.cleanupPass(this);
            return true;
        }
        return false;
    }
    /**
     * Clears all passes from the pass queue.
     */
    public void clear() {
        for (RenderPass p : passes) {
            p.cleanupPass(this);
        }
        passes.clear();
    }
    
    /**
     * 
     * @return 
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }
    /**
     * Gets the ResourceList that manages resources for this framegraph.
     * 
     * @return 
     */
    public ResourceList getResources() {
        return resources;
    }
    /**
     * Gets the framegraph rendering context.
     * 
     * @return 
     */
    public FGRenderContext getContext() {
        return context;
    }
    /**
     * 
     * @return 
     */
    public RenderManager getRenderManager() {
        return context.getRenderManager();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(passes.toArray(RenderPass[]::new), "passes", new RenderPass[0]);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        RenderPass[] array = (RenderPass[])in.readSavableArray("passes", new RenderPass[0]);
        passes.addAll(Arrays.asList(array));
    }
    
}
