#version 110

uniform sampler2D DiffuseSampler;

uniform float Multiplier;

varying vec2 texCoord;
varying vec2 oneTexel;

void main() 
{
	float distance = pow(distance(texCoord.st, vec2(0.5)), 3.0);

    vec4 rValue = texture2D(DiffuseSampler, texCoord.st + vec2(Multiplier * distance, 0.0));  
    vec4 gValue = texture2D(DiffuseSampler, texCoord.st);
    vec4 bValue = texture2D(DiffuseSampler, texCoord.st - vec2(Multiplier * distance, 0.0));

    gl_FragColor = vec4(rValue.r, gValue.g, bValue.b, 1.0);
}

