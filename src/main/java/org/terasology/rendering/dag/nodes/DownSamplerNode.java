/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.terasology.assets.ResourceUrn;
import org.terasology.context.Context;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.dag.ConditionDependentNode;
import org.terasology.rendering.dag.gsoc.FboConnection;
import org.terasology.rendering.dag.stateChanges.BindFbo;
import org.terasology.rendering.dag.stateChanges.EnableMaterial;
import org.terasology.rendering.dag.stateChanges.SetInputTextureFromFbo;
import org.terasology.rendering.dag.stateChanges.SetViewportToSizeOf;
import org.terasology.rendering.opengl.BaseFboManager;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FboConfig;

import static org.terasology.rendering.dag.stateChanges.SetInputTextureFromFbo.FboTexturesTypes.ColorTexture;
import static org.terasology.rendering.opengl.OpenGLUtils.renderFullscreenQuad;

/**
 * Instances of this class take the content of the color attachment of an input FBO
 * and downsamples it into the color attachment of a smaller output FBO.
 */
public class DownSamplerNode extends ConditionDependentNode {
    private static final String TEXTURE_NAME = "tex";
    private static final ResourceUrn DOWN_SAMPLER_MATERIAL_URN = new ResourceUrn("engine:prog.downSampler");

    private FBO outputFbo;
    private Material downSampler;
    private BaseFboManager inputFboManager;
    /**
     * Constructs the DownSamplerNode instance.
     *
     * @param inputFboManager the FBO manager from which to retrieve the input FBO
     * @param outputFboConfig an FboConfig instance describing the output FBO, to be retrieved from the FBO manager
     * @param outputFboManager the FBO manager from which to retrieve the output FBO
     */
    public DownSamplerNode(String nodeUri, Context context,
                           BaseFboManager inputFboManager,
                           FboConfig outputFboConfig, BaseFboManager outputFboManager) {
        super(nodeUri, context);

        // OUT
        // TODO get rid of this? why load input fbo from dependency when I still need this
        this.inputFboManager = inputFboManager;
        outputFbo = requiresFbo(outputFboConfig, outputFboManager);
    }

    /**
     * Processes the input FBO downsampling its color attachment into the color attachment of the output FBO.
     */
    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/" + getUri());

        downSampler.setFloat("size", outputFbo.width(), true);

        renderFullscreenQuad();

        PerformanceMonitor.endActivity();
    }

    @Override
    public void setDependencies(Context context) {
        addOutputFboConnection(1, outputFbo);
        addDesiredStateChange(new BindFbo(outputFbo));
        addDesiredStateChange(new SetViewportToSizeOf(outputFbo));
        addDesiredStateChange(new SetInputTextureFromFbo(0, this.getInputFboData(1), ColorTexture, inputFboManager,
                DOWN_SAMPLER_MATERIAL_URN, TEXTURE_NAME));

        addDesiredStateChange(new EnableMaterial(DOWN_SAMPLER_MATERIAL_URN));
        downSampler = getMaterial(DOWN_SAMPLER_MATERIAL_URN);
    }
}
