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
	vTexCoord.x = 0.5-vTexCoord.x;
	vTexCoord.y = 0.5-vTexCoord.y;
	float radius = 0.1f;
	float rad_sun_corona = 0.25f;
	float dst_center = pow(vTexCoord.x, 2) + pow(vTexCoord.y, 2);
	
	if (dst_center <= radius) {
		gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
		
	} else if (dst_center <= rad_sun_corona) {
		float alpha = 1 - (dst_center - radius)/(rad_sun_corona-radius);
		gl_FragColor = vec4(1.0, 1.0, 1.0, alpha);
	}

}