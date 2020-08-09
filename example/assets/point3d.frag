#ifdef GL_ES
	#define LOWP lowp
	precision mediump float;
#else
	#define LOWP 
#endif

uniform sampler2D u_texture0;

varying LOWP vec4 v_color;

void main()
{
	gl_FragColor = texture2D(u_texture0, gl_PointCoord) * v_color;
}