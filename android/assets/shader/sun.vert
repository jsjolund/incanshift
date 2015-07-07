//incoming Position attribute from our SpriteBatch, vec4(posX, posY, 0.0, 1.0);
attribute vec4 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

//the transformation matrix of our SpriteBatch
uniform mat4 u_projTrans;

varying vec4 vColor;
varying vec2 vTexCoord;
varying vec2 vPosition;

void main() {
	//transform our 2D screen space position into 3D world space
	
 	gl_Position = u_projTrans * a_position;
 	
 	vTexCoord = a_texCoord0;
 	vColor = a_color;
 	vPosition = a_position.xy;
}