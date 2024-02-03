
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_Texture;
uniform int m_SampleRadius;
uniform float m_Size;
uniform float m_Scale;
uniform float m_FalloffPower;

varying vec2 texCoord;

float mapRange(float value, float inMin, float inMax, float outMin, float outMax, float power) {
    float factor = (value-inMin)/(inMax-inMin);
    factor = pow(factor, power);
    return (outMax-outMin)*factor+outMin;
}

const vec2 hRange = vec2(0.16, 0.03);
const vec2 vRange = vec2(0.16, 0.03);

void main() { 
    
    #ifdef HORIZONTAL
        vec2 range = hRange;
    #else
        vec2 range = vRange;
    #endif
    
    float blurSize = m_Scale/m_Size;
    vec4 sum = vec4(0.0);
    
    bool pos = true;
    bool neg = true;
    for (int i = 1; i < m_SampleRadius && (pos || neg); i++) {
        float factor = mapRange(i, 1, m_SampleRadius, range.x, range.y, m_FalloffPower);
        //float factor = 0.5;
        #ifdef HORIZONTAL
            vec2 tPos = vec2(texCoord.x + blurSize * i, texCoord.y);
            vec2 tNeg = vec2(texCoord.x - blurSize * i, texCoord.y);
        #else
            vec2 tPos = vec2(texCoord.x, texCoord.y + blurSize * i);
            vec2 tNeg = vec2(texCoord.x, texCoord.y - blurSize * i);
        #endif
        sum += texture2D(m_Texture, tPos) * factor;
        sum += texture2D(m_Texture, tNeg) * factor;
    }
    
    gl_FragColor = sum;
    
}
