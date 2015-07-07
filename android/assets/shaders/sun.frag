uniform float time;
uniform vec2 resolution;
uniform vec3 pos_sun;
uniform float radius_sun;

varying vec4 vColor;
varying vec2 vTexCoord;

void main() {
	// Screen coordinates
	float y = float(int(vTexCoord.y*resolution.y));
	float x = float(int(vTexCoord.x*resolution.x));
	
	// Distance of screen coordinates from center of sun
	float dst_pos_sun = pow(x-pos_sun.x, 2) + pow(y-pos_sun.y, 2);
	
	// Radius of the sun
	float bound_rad_sun = pow(radius_sun, 2);
	
	// Radius of the corona
	float rad_sun_corona = bound_rad_sun*2;
	
	if (dst_pos_sun <= bound_rad_sun) {
		gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
		
	} else if (dst_pos_sun <= rad_sun_corona) {
		float alpha = 1 - (dst_pos_sun - bound_rad_sun)/(rad_sun_corona-bound_rad_sun);
		gl_FragColor = vec4(1.0, 1.0, 1.0, alpha);
	}
	if (x < pos_sun.x) {
	gl_FragColor = vec4(1.0, 0.0, 0.0, 0.5);
	} else {
	gl_FragColor = vec4(0.0, 1.0, 0.0, 0.5);
	}
}