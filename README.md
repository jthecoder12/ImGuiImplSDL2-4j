# ImGuiImplSDL2-4j
ImGuiImplSDL2 rewritten for Java.

**This library is meant to be used with LWJGL, SpaiR's binding of ImGui, and libsdl4j using it's OpenGL context.**
<br>
Rendering on the SDL GL context works with ImGuiImplGl3, however, inputs won't work as the ImGui bindings for Java only have a GLFW binding. This library solves that as it is a Java version of the original ImGuiImplSDL.

See the example in <code>src/test/java/Main.java</code> for information on how to use the library.

**Credits:**
<br>
[ImGuiImplSDL (Original)](https://github.com/ocornut/imgui/blob/088ddef98aa703b0fe8e1ad91884b6110bee8add/backends/imgui_impl_sdl.cpp)
<br>
[ImGui (Original)](https://github.com/ocornut/imgui/tree/088ddef98aa703b0fe8e1ad91884b6110bee8add)
<br>
[ImGui (Java binding by SpaiR)](https://github.com/SpaiR/imgui-java)
<br>
[libsdl4j](https://github.com/libsdl4j/libsdl4j)
<br>
[JNA](https://github.com/java-native-access/jna)

**There may be bugs since this is a Java rewrite of the original library.**
