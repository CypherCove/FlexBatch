attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_texCoord1;
attribute vec2 a_texCoord2;

uniform mat4 u_projTrans;

varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying vec4 v_color;

void main()
{
   v_texCoords0 = a_texCoord0;
   v_texCoords1 = a_texCoord1;
   v_texCoords2 = a_texCoord2;
   v_color = a_color;
   gl_Position = u_projTrans * a_position;
}
