#ifdef GL_ES
	#define LOWP lowp
	precision mediump float;
#else
	#define LOWP 
#endif

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform sampler2D u_texture2;

varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying LOWP vec4 v_color;

void main()
{
	vec4 tex0 = texture2D(u_texture0, v_texCoords0);
	vec4 tex1 = texture2D(u_texture1, v_texCoords1);
	vec4 tex2 = texture2D(u_texture2, v_texCoords2);
	vec4 merged = vec4(tex0.rgb * (1.0 - tex1.a) + tex1.rgb * tex1.a, min(1.0, tex0.a + tex1.a));
	merged = vec4(merged.rgb * (1.0 - tex2.a) + tex2.rgb * tex2.a, min(1.0, merged.a + tex2.a));
	gl_FragColor = merged * v_color;
}