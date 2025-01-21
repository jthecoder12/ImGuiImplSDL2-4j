import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imguiimplsdl24j.library.ImGuiImplSDL2;
import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.video.SDL_GLContext;
import io.github.libsdl4j.api.video.SDL_Window;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Platform;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.Sdl.SDL_Quit;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_VIDEO;
import static io.github.libsdl4j.api.error.SdlError.SDL_GetError;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_QUIT;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_WINDOWEVENT;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.video.SDL_WindowEventID.SDL_WINDOWEVENT_RESIZED;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideoConst.SDL_WINDOWPOS_CENTERED;
import static org.lwjgl.opengl.GL11.*;

public final class Main {
    public static void main(String[] args) {
        // Initialize SDL2 with SDL_INIT_VIDEO
        if(SDL_Init(SDL_INIT_VIDEO) != 0) throw new IllegalStateException("Failed to initialize SDL2: " + SDL_GetError());

        // Create an SDL2 window
        final SDL_Window window = SDL_CreateWindow("ImGuiImplSDL2-4j Demo", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, 1280, 720, SDL_WINDOW_SHOWN | SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE);
        if(window == null) throw new IllegalStateException("Failed to create SDL2 window: " + SDL_GetError());

        // Create an SDL GL context and create the LWJGL OpenGL capabilities
        SDL_GLContext glContext = SDL_GL_CreateContext(window);
        GL.createCapabilities();

        // Create an instance of ImGuiImplGl3
        ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
        // Create an ImGui context
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        // Enable the FreeType font renderer if we are not on MacOS
        if(Platform.get() != Platform.MACOSX) io.getFonts().setFreeTypeRenderer(true);
        io.getFonts().addFontDefault();
        io.getFonts().build();
        // Initialize ImGuiImplSDL2
        ImGuiImplSDL2.init(window);
        // Initialize ImGuiImplGl3
        imGuiGl3.init("#version 150");

        // Our SDL event
        SDL_Event event = new SDL_Event();
        boolean running = true;
        while(running) {
            while(SDL_PollEvent(event) != 0) {
                // Let ImGuiImplSDL2 process the event
                ImGuiImplSDL2.processEvent(event);
                if(event.type == SDL_QUIT) running = false;
                if(event.type == SDL_WINDOWEVENT && event.window.event == SDL_WINDOWEVENT_RESIZED) /*Resize the viewport when a window resize is detected*/ glViewport(0, 0, event.window.data1, event.window.data2);
            }

            // Clear the screen
            glClear(GL_COLOR_BUFFER_BIT);

            // Start a new frame in ImGuiImplSDL2
            ImGuiImplSDL2.newFrame();
            // Start a new frame in ImGuiImplGl3
            imGuiGl3.newFrame();
            // Start a new frame in ImGui
            ImGui.newFrame();
            // Show the demo window
            ImGui.showDemoWindow();
            // Render the data
            ImGui.render();
            // Let ImGuiImplGl3 render the draw data
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            // OpenGL: Swap the buffers on the window
            SDL_GL_SwapWindow(window);
        }

        // Shutdown ImGuiImplSDL2
        ImGuiImplSDL2.shutdown();
        // Shutdown ImGuiImplGl3
        imGuiGl3.shutdown();
        // Destroy the ImGui context
        ImGui.destroyContext();

        // Quit SDL2
        SDL_Quit();
        // Delete the GL context
        SDL_GL_DeleteContext(glContext);
    }
}
