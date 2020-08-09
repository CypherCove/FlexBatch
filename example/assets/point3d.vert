attribute vec4 a_position;
attribute vec4 a_color;
attribute float a_size;

uniform mat4 u_projTrans;
uniform float u_screenHeight;
uniform vec3 u_up;

varying vec4 v_color;

void main()
{
   v_color = a_color;
   gl_Position = u_projTrans * a_position;
   vec3 offset = u_up * a_size / 2.0;
   vec4 topClipSpace = (u_projTrans * vec4(a_position.xyz + u_up * a_size / 2.0, 1.0));
   gl_PointSize = (topClipSpace.y / topClipSpace.w - gl_Position.y / gl_Position.w) * u_screenHeight;
}
