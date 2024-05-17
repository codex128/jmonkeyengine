/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.util.SafeArrayList;

/**
 *
 * @author codex
 */
public class PostProcessingPass extends RenderPass {

    @Override
    protected void initialize(FrameGraph frameGraph) {}
    @Override
    protected void prepare(FGRenderContext context) {}
    @Override
    protected void execute(FGRenderContext context) {
        SafeArrayList<SceneProcessor> processors = context.getViewPort().getProcessors();
        if (!processors.isEmpty()) {
            context.popFrameBuffer();
            AppProfiler prof = context.getProfiler();
            if (prof != null) {
                prof.vpStep(VpStep.PostFrame, context.getViewPort(), null);
            }
            for (SceneProcessor proc : processors.getArray()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostFrame, proc.getClass().getSimpleName());
                }
                proc.postFrame(context.getViewPort().getOutputFrameBuffer());
            }
            if (prof != null) {
                prof.vpStep(VpStep.ProcEndRender, context.getViewPort(), null);
            }
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return true;
    }
    
}
