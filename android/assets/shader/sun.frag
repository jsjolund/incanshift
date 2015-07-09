uniform vec2 resolution;
uniform vec3 pos_sun;

varying vec4 vColor;
varying vec2 vTexCoord;

void main() {
	float tx = 0.5 - vTexCoord.x;
	float ty = 0.5 - vTexCoord.y;
	
	float radius = 0.1;
	float rad_sun_corona = 0.25;
	float dst_center = float(pow(tx, 2.0) + pow(ty, 2.0));
	
	if (dst_center <= radius) {
		gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
		
	} else if (dst_center <= rad_sun_corona) {
		float alpha = 1.0 - (dst_center - radius)/(rad_sun_corona - radius);
		gl_FragColor = vec4(1.0, 1.0, 1.0, alpha);
	} else {
		gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);	
	}

}