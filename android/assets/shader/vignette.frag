#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif
//our screen resolution, set from Java whenever the display is resized
uniform vec2 resolution;

uniform vec4 color;

//"in" attributes from our vertex shader
varying LOWP vec4 vColor;
varying vec2 vTexCoord;

//radius of our vignette, where 0.5 results in a circle fitting the screen
uniform float radius;

//softness of our vignette, between 0.0 and 1.0
uniform float softness;

void main() {

	// Vignette //
	vec2 position = vec2(vTexCoord.y, vTexCoord.x) - vec2(0.5, 0.5);
	
	// Circle
	//vec2 position1 = vec2(position.x, position.y * resolution.x/resolution.y);
	// Oval
	vec2 position1 = vec2(position.x, position.y * 0.9);
	float len = length(position1);

	float vignette = smoothstep(radius, radius - softness, len);

	float v = sin(1.0 - vignette);
	gl_FragColor = vec4(color.x, color.y, color.z, v);

}
