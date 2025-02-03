package imguiimplsdl24j.library;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.gamecontroller.SDL_GameController;
import io.github.libsdl4j.api.mouse.SDL_Cursor;
import io.github.libsdl4j.api.syswm.SDL_SysWMInfo;
import io.github.libsdl4j.api.video.SDL_Window;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.Platform;

import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.gamecontroller.SDL_GameControllerAxis.*;
import static io.github.libsdl4j.api.gamecontroller.SDL_GameControllerButton.*;
import static io.github.libsdl4j.api.gamecontroller.SdlGamecontroller.*;
import static io.github.libsdl4j.api.hints.SdlHints.SDL_SetHint;
import static io.github.libsdl4j.api.hints.SdlHintsConst.SDL_HINT_MOUSE_FOCUS_CLICKTHROUGH;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;
import static io.github.libsdl4j.api.keycode.SDL_Keymod.*;
import static io.github.libsdl4j.api.mouse.SDL_Button.*;
import static io.github.libsdl4j.api.mouse.SDL_SystemCursor.*;
import static io.github.libsdl4j.api.mouse.SdlMouse.*;
import static io.github.libsdl4j.api.syswm.SdlSysWM.SDL_GetWindowWMInfo;
import static io.github.libsdl4j.api.timer.SdlTimer.SDL_GetPerformanceCounter;
import static io.github.libsdl4j.api.timer.SdlTimer.SDL_GetPerformanceFrequency;
import static io.github.libsdl4j.api.video.SDL_WindowEventID.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_INPUT_FOCUS;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_MINIMIZED;
import static io.github.libsdl4j.api.video.SdlVideo.*;

/**
 * Java port of the C++ ImGuiImplSDL2.
 */
public final class ImGuiImplSDL2 {
	private static int mouseButtonsDown;
	private static int pendingMouseLeaveFrame;
	private static long time;
	private static boolean mouseCanUseGlobalState = false;
	private static SDL_Window window;
	private final static SDL_Cursor[] mouseCursors = new SDL_Cursor[ImGuiMouseCursor.COUNT];

	private ImGuiImplSDL2() {

	}

	private interface LibC extends Library {
		LibC INSTANCE = Native.load(Platform.get() == Platform.WINDOWS ? "msvcrt" : "c", LibC.class);

		int strncmp(CharSequence x, CharSequence y, int z);
	}

	/**
	 * Initializes ImGuiImplSDL2.
	 * @param window The SDL2 window.
	 */
	public static void init(SDL_Window window) {
		ImGuiImplSDL2.window = window;

		ImGuiIO io = ImGui.getIO();

		if(Platform.get() != Platform.MACOSX) {
			final String sdlBackend = SDL_GetCurrentVideoDriver();
			final String[] globalMouseWhitelist = {"windows", "cocoa", "x11", "DIVE", "VMAN"};
			for(String mouse : globalMouseWhitelist) if(LibC.INSTANCE.strncmp(sdlBackend, mouse, globalMouseWhitelist.length) == 0) mouseCanUseGlobalState = true;
		}

		io.setBackendPlatformName("imgui_impl_sdl");
		io.addBackendFlags(ImGuiBackendFlags.HasMouseCursors);
		io.addBackendFlags(ImGuiBackendFlags.HasSetMousePos);
		final IntByReference width = new IntByReference();
		final IntByReference height = new IntByReference();
		SDL_GetWindowSize(window, width, height);
		io.setDisplaySize(width.getValue(), height.getValue());

		mouseCursors[ImGuiMouseCursor.Arrow] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_ARROW);
		mouseCursors[ImGuiMouseCursor.TextInput] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_IBEAM);
		mouseCursors[ImGuiMouseCursor.ResizeAll] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_SIZEALL);
		mouseCursors[ImGuiMouseCursor.ResizeNS] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_SIZENS);
		mouseCursors[ImGuiMouseCursor.ResizeEW] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_SIZEWE);
		mouseCursors[ImGuiMouseCursor.ResizeNESW] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_SIZENESW);
		mouseCursors[ImGuiMouseCursor.ResizeNWSE] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_SIZENWSE);
		mouseCursors[ImGuiMouseCursor.Hand] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_HAND);
		mouseCursors[ImGuiMouseCursor.NotAllowed] = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_NO);
		
		if(Platform.get() == Platform.WINDOWS) {
			SDL_SysWMInfo info = new SDL_SysWMInfo();
			SDL_GetWindowWMInfo(window, info);
			if(SDL_GetWindowWMInfo(window, info)) ImGui.getMainViewport().setPlatformHandleRaw(Pointer.nativeValue(info.info.win.window));
		}

		SDL_SetHint(SDL_HINT_MOUSE_FOCUS_CLICKTHROUGH, "1");
	}

	/**
	 * Processes the SDL event. Put in main loop.
	 * @param event The SDL event.
	 */
	public static void processEvent(@NotNull SDL_Event event) {
		ImGuiIO io = ImGui.getIO();

		switch(event.type) {
			case SDL_MOUSEMOTION:
				io.addMousePosEvent((float)event.motion.x, (float)event.motion.y);
				break;

			case SDL_MOUSEWHEEL:
				float wheelX = Integer.compare(event.wheel.x, 0);
				float wheelY = Integer.compare(event.wheel.y, 0);
				io.addMouseWheelEvent(wheelX, wheelY);
				break;

			case SDL_MOUSEBUTTONDOWN:

			case SDL_MOUSEBUTTONUP:
				int mouseButton = -1;
				if(event.button.button == SDL_BUTTON_LEFT) mouseButton = 0;
				if(event.button.button == SDL_BUTTON_RIGHT) mouseButton = 1;
				if(event.button.button == SDL_BUTTON_MIDDLE) mouseButton = 2;
				if(event.button.button == SDL_BUTTON_X1) mouseButton = 3;
				if(event.button.button == SDL_BUTTON_X2) mouseButton = 4;
				if(mouseButton == -1) break;
				io.addMouseButtonEvent(mouseButton, (event.type == SDL_MOUSEBUTTONDOWN));
				mouseButtonsDown = (event.type == SDL_MOUSEBUTTONDOWN) ? (mouseButtonsDown | Integer.parseInt(("1"+mouseButton))) : (mouseButtonsDown & Integer.parseInt(("1"+mouseButton)));
				break;

			case SDL_TEXTINPUT:
				io.addInputCharactersUTF8(String.valueOf(new String(event.text.text).charAt(0)));
				break;

			case SDL_KEYDOWN:

			case SDL_KEYUP:
				final short keyMods = event.key.keysym.mod;

				io.addKeyEvent(ImGuiKey.ModCtrl, (keyMods & KMOD_CTRL) != 0);
				io.addKeyEvent(ImGuiKey.ModShift, (keyMods & KMOD_SHIFT) != 0);
				io.addKeyEvent(ImGuiKey.ModAlt, (keyMods & KMOD_ALT) != 0);
				io.addKeyEvent(ImGuiKey.ModSuper, (keyMods & KMOD_GUI) != 0);

				final int key = keyCodeToImGuiKey(event.key.keysym.sym);
				io.addKeyEvent(key, (event.type == SDL_KEYDOWN));
				io.setKeyEventNativeData(key, event.key.keysym.sym, event.key.keysym.scancode, event.key.keysym.scancode);
				break;

			case SDL_WINDOWEVENT:
				byte windowEvent = event.window.event;
				if(windowEvent == SDL_WINDOWEVENT_ENTER) pendingMouseLeaveFrame = 0;
				if(windowEvent == SDL_WINDOWEVENT_LEAVE) pendingMouseLeaveFrame = ImGui.getFrameCount() + 1;
				if(windowEvent == SDL_WINDOWEVENT_FOCUS_GAINED) io.addFocusEvent(true);
				if(windowEvent == SDL_WINDOWEVENT_FOCUS_LOST) io.addFocusEvent(false);
				break;
		}
	}

	private static int keyCodeToImGuiKey(int keycode)
	{
		switch (keycode)
		{
			case SDLK_TAB: return ImGuiKey.Tab;
			case SDLK_DELETE: return ImGuiKey.Delete;
			case SDLK_BACKSPACE: return ImGuiKey.Backspace;
			case SDLK_SPACE: return ImGuiKey.Space;
			case SDLK_RETURN: return ImGuiKey.Enter;
			case SDLK_ESCAPE: return ImGuiKey.Escape;
			case SDLK_QUOTE: return ImGuiKey.Apostrophe;
			case SDLK_COMMA: return ImGuiKey.Comma;
			case SDLK_MINUS: return ImGuiKey.Minus;
			case SDLK_PERIOD: return ImGuiKey.Period;
			case SDLK_SLASH: return ImGuiKey.Slash;
			case SDLK_EQUALS: return ImGuiKey.Equal;
			case SDLK_LEFTBRACKET: return ImGuiKey.LeftBracket;
			case SDLK_BACKSLASH: return ImGuiKey.Backslash;
			case SDLK_RIGHTBRACKET: return ImGuiKey.RightBracket;
			case SDLK_BACKQUOTE: return ImGuiKey.GraveAccent;
			case SDLK_0: return ImGuiKey._0;
			case SDLK_1: return ImGuiKey._1;
			case SDLK_2: return ImGuiKey._2;
			case SDLK_3: return ImGuiKey._3;
			case SDLK_4: return ImGuiKey._4;
			case SDLK_5: return ImGuiKey._5;
			case SDLK_6: return ImGuiKey._6;
			case SDLK_7: return ImGuiKey._7;
			case SDLK_8: return ImGuiKey._8;
			case SDLK_9: return ImGuiKey._9;
			case SDLK_A: return ImGuiKey.A;
			case SDLK_B: return ImGuiKey.B;
			case SDLK_C: return ImGuiKey.C;
			case SDLK_D: return ImGuiKey.D;
			case SDLK_E: return ImGuiKey.E;
			case SDLK_F: return ImGuiKey.F;
			case SDLK_G: return ImGuiKey.G;
			case SDLK_H: return ImGuiKey.H;
			case SDLK_I: return ImGuiKey.I;
			case SDLK_J: return ImGuiKey.J;
			case SDLK_K: return ImGuiKey.K;
			case SDLK_L: return ImGuiKey.L;
			case SDLK_M: return ImGuiKey.M;
			case SDLK_N: return ImGuiKey.N;
			case SDLK_O: return ImGuiKey.O;
			case SDLK_P: return ImGuiKey.P;
			case SDLK_Q: return ImGuiKey.Q;
			case SDLK_R: return ImGuiKey.R;
			case SDLK_S: return ImGuiKey.S;
			case SDLK_T: return ImGuiKey.T;
			case SDLK_U: return ImGuiKey.U;
			case SDLK_V: return ImGuiKey.V;
			case SDLK_W: return ImGuiKey.W;
			case SDLK_X: return ImGuiKey.X;
			case SDLK_Y: return ImGuiKey.Y;
			case SDLK_Z: return ImGuiKey.Z;

		}

		if(keycode == SDLK_LEFT) return ImGuiKey.LeftArrow;
		if(keycode == SDLK_RIGHT) return ImGuiKey.RightArrow;
		if(keycode == SDLK_UP) return ImGuiKey.UpArrow;
		if(keycode == SDLK_DOWN) return ImGuiKey.DownArrow;
		if(keycode == SDLK_PAGEUP) return ImGuiKey.PageUp;
		if(keycode == SDLK_PAGEDOWN) return ImGuiKey.PageDown;
		if(keycode == SDLK_HOME) return ImGuiKey.Home;
		if(keycode == SDLK_END) return ImGuiKey.End;
		if(keycode == SDLK_INSERT) return ImGuiKey.Insert;
		if(keycode == SDLK_F1) return ImGuiKey.F1;
		if(keycode == SDLK_F2) return ImGuiKey.F2;
		if(keycode == SDLK_F3) return ImGuiKey.F3;
		if(keycode == SDLK_F4) return ImGuiKey.F4;
		if(keycode == SDLK_F5) return ImGuiKey.F5;
		if(keycode == SDLK_F6) return ImGuiKey.F6;
		if(keycode == SDLK_F7) return ImGuiKey.F7;
		if(keycode == SDLK_F8) return ImGuiKey.F8;
		if(keycode == SDLK_F9) return ImGuiKey.F9;
		if(keycode == SDLK_F11) return ImGuiKey.F11;
		if(keycode == SDLK_F10) return ImGuiKey.F10;
		if(keycode == SDLK_F12) return ImGuiKey.F12;
		if(keycode == SDLK_SCROLLLOCK) return ImGuiKey.ScrollLock;
		if(keycode == SDLK_CAPSLOCK) return ImGuiKey.CapsLock;
		if(keycode == SDLK_NUMLOCKCLEAR) return ImGuiKey.NumLock;
		if(keycode == SDLK_PRINTSCREEN) return ImGuiKey.PrintScreen;
		if(keycode == SDLK_PAUSE) return ImGuiKey.Pause;
		if(keycode == SDLK_KP_0) return ImGuiKey.Keypad0;
		if(keycode == SDLK_KP_1) return ImGuiKey.Keypad1;
		if(keycode == SDLK_KP_2) return ImGuiKey.Keypad2;
		if(keycode == SDLK_KP_3) return ImGuiKey.Keypad3;
		if(keycode == SDLK_KP_4) return ImGuiKey.Keypad4;
		if(keycode == SDLK_KP_5) return ImGuiKey.Keypad5;
		if(keycode == SDLK_KP_6) return ImGuiKey.Keypad6;
		if(keycode == SDLK_KP_7) return ImGuiKey.Keypad7;
		if(keycode == SDLK_KP_8) return ImGuiKey.Keypad8;
		if(keycode == SDLK_KP_9) return ImGuiKey.Keypad9;
		if(keycode == SDLK_KP_PERIOD) return ImGuiKey.KeypadDecimal;
		if(keycode == SDLK_KP_DIVIDE) return ImGuiKey.KeypadDivide;
		if(keycode == SDLK_KP_MULTIPLY) return ImGuiKey.KeypadMultiply;
		if(keycode == SDLK_KP_MINUS) return ImGuiKey.KeypadSubtract;
		if(keycode == SDLK_KP_PLUS) return ImGuiKey.KeypadAdd;
		if(keycode == SDLK_KP_ENTER) return ImGuiKey.KeypadEnter;
		if(keycode == SDLK_KP_EQUALS) return ImGuiKey.KeypadEqual;
		if(keycode == SDLK_LCTRL) return ImGuiKey.LeftCtrl;
		if(keycode == SDLK_LSHIFT) return ImGuiKey.LeftShift;
		if(keycode == SDLK_LALT) return ImGuiKey.LeftAlt;
		if(keycode == SDLK_LGUI) return ImGuiKey.LeftSuper;
		if(keycode == SDLK_RCTRL) return ImGuiKey.RightCtrl;
		if(keycode == SDLK_RSHIFT) return ImGuiKey.RightShift;
		if(keycode == SDLK_RALT) return ImGuiKey.RightAlt;
		if(keycode == SDLK_RGUI) return ImGuiKey.RightSuper;
		if(keycode == SDLK_APPLICATION) return ImGuiKey.Menu;
        //noinspection ConstantValue
        if(keycode == SDLK_SEMICOLON) return ImGuiKey.Semicolon;

		return ImGuiKey.None;
	}

	/**
	 * Starts a new frame;
	 */
	public static void newFrame() {
		ImGuiIO io = ImGui.getIO();

		final IntByReference w = new IntByReference();
		final IntByReference h = new IntByReference();
		final IntByReference displayW = new IntByReference();
		final IntByReference displayH = new IntByReference();

		SDL_GetWindowSize(window, w, h);

		if((SDL_GetWindowFlags(window) & SDL_WINDOW_MINIMIZED) == SDL_WINDOW_MINIMIZED) {
			w.setValue(0);
			h.setValue(0);
		}
		SDL_GL_GetDrawableSize(window, displayW, displayH);
		io.setDisplaySize(w.getValue(), h.getValue());
		if(w.getValue() > 0 && h.getValue() > 0) io.setDisplayFramebufferScale((float) displayW.getValue() /w.getValue(), (float) displayH.getValue() /h.getValue());

		final long frequency = SDL_GetPerformanceFrequency();
		final long currentTime = SDL_GetPerformanceCounter();
		io.setDeltaTime(time > 0 ? (float) (currentTime - time) / frequency : 1f/60);
		time = currentTime;

		if(pendingMouseLeaveFrame >= ImGui.getFrameCount() && mouseButtonsDown == 0) {
			io.addMousePosEvent(-1, -1);
			pendingMouseLeaveFrame = 0;
		}

		updateMouseData();
		updateMouseCursor();
		updateGamepads();
	}

	private static void updateMouseData() {
		ImGuiIO io = ImGui.getIO();

		if((SDL_GetWindowFlags(window) & SDL_WINDOW_INPUT_FOCUS) == SDL_WINDOW_INPUT_FOCUS) {
			if(io.getWantSetMousePos()) SDL_WarpMouseInWindow(window, (int)io.getMousePosX(), (int)io.getMousePosY());

			if(mouseCanUseGlobalState && mouseButtonsDown == 0) {
				final IntByReference windowX = new IntByReference();
				final IntByReference windowY = new IntByReference();
				final IntByReference mouseXGlobal = new IntByReference();
				final IntByReference mouseYGlobal = new IntByReference();

				SDL_GetGlobalMouseState(mouseXGlobal, mouseYGlobal);
				SDL_GetWindowPosition(window, windowX, windowY);
				io.addMousePosEvent(mouseXGlobal.getValue() - windowX.getValue(), mouseYGlobal.getValue() - windowY.getValue());
			}
		}
	}

	private static void updateMouseCursor() {
		ImGuiIO io = ImGui.getIO();
		if(io.hasConfigFlags(ImGuiConfigFlags.NoMouseCursorChange)) return;

		final int imguiCursor = ImGui.getMouseCursor();
		if(io.getMouseDrawCursor() || imguiCursor == ImGuiMouseCursor.None) SDL_ShowCursor(0);
		else {
			SDL_SetCursor(mouseCursors[imguiCursor] != null ? mouseCursors[imguiCursor] : mouseCursors[ImGuiMouseCursor.Arrow]);
			SDL_ShowCursor(1);
		}
	}

	private static void updateGamepads() {
		ImGuiIO io = ImGui.getIO();
		if(!io.hasConfigFlags(ImGuiConfigFlags.NavEnableGamepad)) return;

		final SDL_GameController controller = SDL_GameControllerOpen(0);
		if(controller == null) return;

		io.addBackendFlags(ImGuiBackendFlags.HasGamepad);

		final int thumbDeadZone = 8000;
		mapButton(ImGuiKey.GamepadStart,           SDL_CONTROLLER_BUTTON_START, io, controller);
		mapButton(ImGuiKey.GamepadBack,            SDL_CONTROLLER_BUTTON_BACK, io, controller);
		mapButton(ImGuiKey.GamepadFaceDown,        SDL_CONTROLLER_BUTTON_A, io, controller);              // Xbox A, PS Cross
		mapButton(ImGuiKey.GamepadFaceRight,       SDL_CONTROLLER_BUTTON_B, io, controller);              // Xbox B, PS Circle
		mapButton(ImGuiKey.GamepadFaceLeft,        SDL_CONTROLLER_BUTTON_X, io, controller);              // Xbox X, PS Square
		mapButton(ImGuiKey.GamepadFaceUp,          SDL_CONTROLLER_BUTTON_Y, io, controller);              // Xbox Y, PS Triangle
		mapButton(ImGuiKey.GamepadDpadLeft,        SDL_CONTROLLER_BUTTON_DPAD_LEFT, io, controller);
		mapButton(ImGuiKey.GamepadDpadRight,       SDL_CONTROLLER_BUTTON_DPAD_RIGHT, io, controller);
		mapButton(ImGuiKey.GamepadDpadUp,          SDL_CONTROLLER_BUTTON_DPAD_UP, io, controller);
		mapButton(ImGuiKey.GamepadDpadDown,        SDL_CONTROLLER_BUTTON_DPAD_DOWN, io, controller);
		mapButton(ImGuiKey.GamepadL1,              SDL_CONTROLLER_BUTTON_LEFTSHOULDER, io, controller);
		mapButton(ImGuiKey.GamepadR1,              SDL_CONTROLLER_BUTTON_RIGHTSHOULDER, io, controller);
		mapAnalog(ImGuiKey.GamepadL2,              SDL_CONTROLLER_AXIS_TRIGGERLEFT,  0.0f, 32767, io, controller);
		mapAnalog(ImGuiKey.GamepadR2,              SDL_CONTROLLER_AXIS_TRIGGERRIGHT, 0.0f, 32767, io, controller);
		mapButton(ImGuiKey.GamepadL3,              SDL_CONTROLLER_BUTTON_LEFTSTICK, io, controller);
		mapButton(ImGuiKey.GamepadR3,              SDL_CONTROLLER_BUTTON_RIGHTSTICK, io, controller);
		mapAnalog(ImGuiKey.GamepadLStickLeft,      SDL_CONTROLLER_AXIS_LEFTX,  -thumbDeadZone, -32768, io, controller);
		mapAnalog(ImGuiKey.GamepadLStickRight,     SDL_CONTROLLER_AXIS_LEFTX,  +thumbDeadZone, +32767, io, controller);
		mapAnalog(ImGuiKey.GamepadLStickUp,        SDL_CONTROLLER_AXIS_LEFTY,  -thumbDeadZone, -32768, io, controller);
		mapAnalog(ImGuiKey.GamepadLStickDown,      SDL_CONTROLLER_AXIS_LEFTY,  +thumbDeadZone, +32767, io, controller);
		mapAnalog(ImGuiKey.GamepadRStickLeft,      SDL_CONTROLLER_AXIS_RIGHTX, -thumbDeadZone, -32768, io, controller);
		mapAnalog(ImGuiKey.GamepadRStickRight,     SDL_CONTROLLER_AXIS_RIGHTX, +thumbDeadZone, +32767, io, controller);
		mapAnalog(ImGuiKey.GamepadRStickUp,        SDL_CONTROLLER_AXIS_RIGHTY, -thumbDeadZone, -32768, io, controller);
		mapAnalog(ImGuiKey.GamepadRStickDown,      SDL_CONTROLLER_AXIS_RIGHTY, +thumbDeadZone, +32767, io, controller);
	}

	private static void mapButton(int keyNo, int buttonNo, @NotNull ImGuiIO io, SDL_GameController controller) {
		io.addKeyEvent(keyNo, SDL_GameControllerGetButton(controller, buttonNo) != 0);
	}

	private static void mapAnalog(int keyNo, int axisNo, float v0, float v1, @NotNull ImGuiIO io, SDL_GameController controller) {
		float vn = imSaturate((SDL_GameControllerGetAxis(controller, axisNo) - v0) / (v1 - v0));
		io.addKeyAnalogEvent(keyNo, vn > 0.1f, vn);
	}

	private static float imSaturate(float v) {
		return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
	}

	/**
	 * Disposes ImGuiImplSDL2.
	 */
	public static void shutdown() {
		for(SDL_Cursor cursor : mouseCursors) SDL_FreeCursor(cursor);
	}
}
