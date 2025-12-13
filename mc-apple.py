import ctypes
import time
import threading
import keyboard
import customtkinter as ctk
from ctypes import wintypes
import pyautogui
import json
import os
import webbrowser

START_KEY = "f8"
EXIT_KEY = "f9"

REPAIR_EVERY = 100
PLACE_DELAY = 0.05
SLOT_ROTATE_EVERY = 500

DISCORD_URL = "https://discord.gg/getnotify"
GITHUB_URL = "https://github.com/xqbkubiak/"

CONFIG_FILE = "mc-apple_config.json"

import base64 as _b
_A = lambda x: _b.b64decode(x).decode()
_AUTHOR_LABEL = _A("Q3JlYXRlZCBieTo=")
_AUTHOR_NAME = _A("cnUuc3U=")
_CHAT_MSG = _A("SmFrIHN6eWJrbyB6YXJvYmnEhyBuYSBFQVJUSFNNUCB5dDogQHJhanplaCA=")

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("blue")

pyautogui.FAILSAFE = False


class ConfigManager:
    """Manages saving and loading configuration."""
    
    DEFAULT_CONFIG = {
        'use_repair': True,
        'use_free_repair': False,
        'repair_every': 100,
        'place_delay': 0.05,
        'use_slot_rotation': False,
        'slot_rotate_every': 500,
        'use_eating': False,
        'eat_every': 1000,
    }
    
    def __init__(self, config_file=CONFIG_FILE):
        self.config_file = config_file
        self.config = self.load()
    
    def load(self):
        """Loads configuration from JSON file."""
        if os.path.exists(self.config_file):
            try:
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    loaded = json.load(f)
                    config = self.DEFAULT_CONFIG.copy()
                    config.update(loaded)
                    return config
            except Exception as e:
                print(f"Config load error: {e}")
        return self.DEFAULT_CONFIG.copy()
    
    def save(self, config_dict):
        """Saves configuration to JSON file."""
        try:
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(config_dict, f, indent=2, ensure_ascii=False)
            self.config = config_dict.copy()
            return True
        except Exception as e:
            print(f"Config save error: {e}")
            return False
    
    def get(self, key, default=None):
        """Gets value from configuration."""
        return self.config.get(key, default)

user32 = ctypes.windll.user32

WM_KEYDOWN = 0x0100
WM_KEYUP = 0x0101
WM_CHAR = 0x0102
WM_LBUTTONDOWN = 0x0201
WM_LBUTTONUP = 0x0202
WM_RBUTTONDOWN = 0x0204
WM_RBUTTONUP = 0x0205

VK_RETURN = 0x0D
VK_T = 0x54
VK_1 = 0x31
VK_2 = 0x32

MK_LBUTTON = 0x0001
MK_RBUTTON = 0x0002

EnumWindows = user32.EnumWindows
EnumWindowsProc = ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
GetWindowText = user32.GetWindowTextW
GetWindowTextLength = user32.GetWindowTextLengthW
PostMessage = user32.PostMessageW
SendMessage = user32.SendMessageW
IsWindowVisible = user32.IsWindowVisible

MapVirtualKey = user32.MapVirtualKeyW
MAPVK_VK_TO_VSC = 0


class POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]

ClientToScreen = user32.ClientToScreen

def make_lparam(vk, down=True):
    scan_code = MapVirtualKey(vk, MAPVK_VK_TO_VSC)
    if down:
        return (scan_code << 16) | 1
    else:
        return (scan_code << 16) | 0xC0000001


class BlockPlacer:
    SLOT_KEYS = [0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39]
    
    def __init__(self, gui_callback=None):
        self.hwnd = None
        self.window_title = ""
        self.running = False
        self.paused = True
        self.thread = None
        self.gui_callback = gui_callback
        self.cycle_count = 0
        
        self.use_repair = True
        self.use_free_repair = False
        self.repair_every = REPAIR_EVERY
        self.place_delay = PLACE_DELAY
        
        self.use_slot_rotation = False
        self.slot_rotate_every = SLOT_ROTATE_EVERY
        self.current_slot = 1
        
        self.use_eating = False
        self.eat_every = 1000
        
    def log(self, msg):
        print(msg)
        if self.gui_callback:
            self.gui_callback(msg)
        
    def find_minecraft(self):
        windows = []
        required_keywords = ['minecraft']
        
        excluded_keywords = [
            'chrome', 'firefox', 'edge', 'brave', 'opera', 'vivaldi', 'safari',
            'youtube', 'twitch', 'kick.com', 'netflix', 'vimeo',
            'kopacz', 'miner', 'placer', 'python', 'visual studio', 'code', 
            'cmd', 'powershell', 'notepad', 'discord', 'spotify',
            'explorer', 'folder',
        ]
        
        minecraft_classes = ['LWJGL', 'GLFW', 'SunAwtCanvas', 'SunAwtFrame']
        
        GetClassName = user32.GetClassNameW
        
        def callback(hwnd, _):
            if IsWindowVisible(hwnd):
                length = GetWindowTextLength(hwnd)
                if length > 0:
                    buff = ctypes.create_unicode_buffer(length + 1)
                    GetWindowText(hwnd, buff, length + 1)
                    title = buff.value
                    title_lower = title.lower()
                    
                    class_buff = ctypes.create_unicode_buffer(256)
                    GetClassName(hwnd, class_buff, 256)
                    window_class = class_buff.value
                    
                    has_required = any(kw in title_lower for kw in required_keywords)
                    has_excluded = any(kw in title_lower for kw in excluded_keywords)
                    is_launcher = any(x in title_lower for x in ['lunar', 'badlion', 'feather', 'labymod'])
                    is_minecraft_class = any(mc in window_class for mc in minecraft_classes)
                    
                    if (has_required or is_launcher) and not has_excluded:
                        priority = 1 if is_minecraft_class else 0
                        windows.append((priority, hwnd, title, window_class))
            return True
        
        EnumWindows(EnumWindowsProc(callback), 0)
        
        if windows:
            windows.sort(key=lambda x: x[0], reverse=True)
            best = windows[0]
            self.hwnd = best[1]
            self.window_title = best[2]
            self.log(f"✅ Found: {best[2][:40]}...")
            return True, best[2]
        self.log("❌ Minecraft not found!")
        return False, None
    
    def key_down(self, vk):
        if self.hwnd:
            lparam = make_lparam(vk, down=True)
            PostMessage(self.hwnd, WM_KEYDOWN, vk, lparam)
            SendMessage(self.hwnd, WM_KEYDOWN, vk, lparam)
    
    def key_up(self, vk):
        if self.hwnd:
            lparam = make_lparam(vk, down=False)
            PostMessage(self.hwnd, WM_KEYUP, vk, lparam)
            SendMessage(self.hwnd, WM_KEYUP, vk, lparam)

    def resize_window(self):
        """Resizes the Minecraft window to 1280x720."""
        if self.hwnd:
            try:
                # Restore if minimized
                user32.ShowWindow(self.hwnd, 9) # SW_RESTORE
                time.sleep(0.2)
                
                # Move and Resize
                # X=0, Y=0, Width=1280, Height=720, Repaint=True
                user32.MoveWindow(self.hwnd, 0, 0, 1280, 720, True)
                self.log("   📏 Resized Minecraft to 1280x720")
            except Exception as e:
                self.log(f"   ❌ Resize failed: {e}")
    
    def key_press(self, vk, duration=0.05):
        self.key_down(vk)
        time.sleep(duration)
        self.key_up(vk)
    
    def right_click(self):
        """Right mouse button click."""
        if self.hwnd:
            PostMessage(self.hwnd, WM_RBUTTONDOWN, MK_RBUTTON, 0)
            time.sleep(0.02)
            PostMessage(self.hwnd, WM_RBUTTONUP, 0, 0)
    
    def left_click(self):
        """Left mouse button click."""
        if self.hwnd:
            PostMessage(self.hwnd, WM_LBUTTONDOWN, MK_LBUTTON, 0)
            time.sleep(0.02)
            PostMessage(self.hwnd, WM_LBUTTONUP, 0, 0)
    
    def send_text(self, text):
        if self.hwnd:
            for char in text:
                PostMessage(self.hwnd, WM_CHAR, ord(char), 0)
                time.sleep(0.01)
    
    def send_command(self, cmd):
        """Sends a command to chat."""
        self.log(f"   📝 Command: {cmd}")
        self.key_press(VK_T)
        time.sleep(0.2)
        self.send_text(cmd)
        time.sleep(0.1)
        self.key_press(VK_RETURN)
        time.sleep(0.5)
    
    def send_chat_message(self, msg):
        """Sends a chat message (not a command)."""
        self.log(f"   💬 Chat: {msg}")
        self.key_press(VK_T)
        time.sleep(0.2)
        self.send_text(msg)
        time.sleep(0.1)
        self.key_press(VK_RETURN)
        time.sleep(0.5)
    
    def get_current_slot_key(self):
        """Returns the VK code for current placing slot (2-9 or 3-9 if eating enabled)."""
        if self.use_slot_rotation:
            return self.SLOT_KEYS[self.current_slot]
        else:
            if self.use_eating:
                return 0x33  # VK_3 - slot 3 when eating is enabled
            return VK_2
    
    def get_starting_slot(self):
        """Returns starting slot index (1 for slot 2, 2 for slot 3 if eating enabled)."""
        return 2 if self.use_eating else 1
    
    def rotate_slot(self):
        """Rotates to next slot (3->4->...->9->3 if eating, else 2->3->...->9->2)."""
        start_slot = self.get_starting_slot()
        max_slot = 8  # slot 9 index
        old_slot = self.current_slot + 1
        
        if self.current_slot >= max_slot:
            self.current_slot = start_slot
        else:
            self.current_slot += 1
        
        new_slot = self.current_slot + 1
        self.log(f"🔄 Slot rotation: {old_slot} → {new_slot}")
    
    def eat_food(self):
        """Eats food from slot 2."""
        if not self.running or self.paused or not self.use_eating:
            return
        if self.cycle_count % self.eat_every != 0:
            return
        
        self.log(f"   🍎 Eating from slot 2 (every {self.eat_every} cycles)")
        self.key_press(VK_2, duration=0.02)
        time.sleep(0.1)
        
        # Hold right click for eating (longer hold)
        if self.hwnd:
            PostMessage(self.hwnd, WM_RBUTTONDOWN, MK_RBUTTON, 0)
            time.sleep(2.5)  # Hold for 2.5 seconds to eat
            PostMessage(self.hwnd, WM_RBUTTONUP, 0, 0)
        
        time.sleep(0.2)
    
    def do_place_cycle(self):
        """One placing cycle: slot X -> right click -> slot 1 -> left click"""
        if not self.running or self.paused:
            return
        
        slot_key = self.get_current_slot_key()
        self.key_press(slot_key, duration=0.02)
        time.sleep(self.place_delay)
        
        if not self.running or self.paused:
            return
        
        self.right_click()
        time.sleep(self.place_delay)
        
        if not self.running or self.paused:
            return
        
        self.key_press(VK_1, duration=0.02)
        time.sleep(self.place_delay)
        
        if not self.running or self.paused:
            return
        
        self.left_click()
        time.sleep(self.place_delay)
    
    def get_client_origin(self):
        """Returns the (x, y) screen coordinates of the client area top-left corner."""
        if self.hwnd:
            pt = POINT(0, 0)
            ClientToScreen(self.hwnd, ctypes.byref(pt))
            return pt.x, pt.y
        return 0, 0

    def get_client_origin(self):
        """Returns the (x, y) screen coordinates of the client area top-left corner."""
        if self.hwnd:
            pt = POINT(0, 0)
            ClientToScreen(self.hwnd, ctypes.byref(pt))
            return pt.x, pt.y
        return 0, 0

    def perform_free_repair(self):
        """
        Executes 'Free Repair' (Crafting Shears) sequence.
        1. Open Inventory (E)
        2. Click Iron Stack
        3. Right Click Craft Slot 1 (Place 1 Iron)
        4. Right Click Craft Slot 2 (Place 1 Iron)
        5. Put back Iron (Click original slot)
        6. Shift+Click Result (Craft Shears)
        7. Close (E)
        """
        self.log("   ✂️ Free Repair (Crafting)...")
        
        # COORDINATES (From User Logs Step 171)
        # Assume 1280x720 window at 0,0
        IRON_SLOT = (648, 391)      # Where your Iron stack is
        CRAFT_1 = (827, 301)        # Top-Right crafting slot (2x2 grid)
        CRAFT_2 = (868, 255)        # Bottom-Left crafting slot (2x2 grid)
        RESULT_SLOT = (947, 277)    # Output slot
        
        # 1. Open Inventory
        self.key_press(0x45) # VK_E
        time.sleep(0.5)
        
        try:
            # 2. Pick up Iron
            pyautogui.moveTo(*IRON_SLOT)
            pyautogui.click()
            time.sleep(0.1)
            
            # 3. Place Iron 1
            pyautogui.moveTo(*CRAFT_1)
            pyautogui.click(button='right')
            time.sleep(0.1)
            
            # 4. Place Iron 2
            pyautogui.moveTo(*CRAFT_2)
            pyautogui.click(button='right')
            time.sleep(0.1)
            
            # 5. Put back rest of Iron
            pyautogui.moveTo(*IRON_SLOT)
            pyautogui.click()
            time.sleep(0.1)
            
            # 6. Craft (Shift+Click)
            pyautogui.keyDown('shift')
            pyautogui.moveTo(*RESULT_SLOT)
            pyautogui.click()
            pyautogui.keyUp('shift')
            time.sleep(0.1)
            
        except Exception as e:
            self.log(f"   ❌ Craft Error: {e}")
            pyautogui.keyUp('shift') # Safety
            
        # 7. Close Inventory
        self.key_press(0x1B) # VK_ESCAPE
        time.sleep(0.3)
        
        # Double check close
        self.key_press(0x1B) # VK_ESCAPE
        time.sleep(0.2)
        
    def repair(self):
        """Executes repair sequence (Command or Free Crafting)."""
        # Run if either repair mode is enabled
        if not self.running or self.paused:
            return
        
        if not (self.use_repair or self.use_free_repair):
            return

        if self.cycle_count % self.repair_every != 0:
            return
            
        if self.use_free_repair:
            self.perform_free_repair()
        elif self.use_repair:
            self.log(f"   🔧 /repair (every {self.repair_every} cycles)")
            time.sleep(0.1)
            self.send_command("/repair")
            time.sleep(0.3)
        
    def repair(self):
        """Executes repair sequence (Command or Manual)."""
        # Run if either repair mode is enabled
        if not self.running or self.paused:
            return
        
        if not (self.use_repair or self.use_free_repair):
            return

        if self.cycle_count % self.repair_every != 0:
            return
            
        if self.use_free_repair:
            self.perform_free_repair()
        elif self.use_repair:
            self.log(f"   🔧 /repair (every {self.repair_every} cycles)")
            time.sleep(0.1)
            self.send_command("/repair")
            time.sleep(0.3)
    
    def main_loop(self):
        while self.running:
            if self.paused:
                time.sleep(0.1)
                continue
            
            self.cycle_count += 1
            
            if self.cycle_count % 10 == 0:
                slot_info = f" [Slot {self.current_slot + 1}]" if self.use_slot_rotation else ""
                self.log(f"🏗️ Cycle #{self.cycle_count}{slot_info}")
            
            self.do_place_cycle()
            
            if not self.running or self.paused:
                continue
            
            if self.cycle_count % self.repair_every == 0:
                self.repair()
            
            if self.use_eating and self.eat_every > 0:
                if self.cycle_count % self.eat_every == 0:
                    self.eat_food()
            
            if self.use_slot_rotation and self.slot_rotate_every > 0:
                if self.cycle_count % self.slot_rotate_every == 0:
                    self.rotate_slot()
            
            if self.cycle_count % 500 == 0:
                import random
                exclamations = "!" * random.randint(1, 3)
                self.send_chat_message(f"{_CHAT_MSG}{exclamations}")
    
    def toggle(self):
        if self.paused:
            if not self.hwnd:
                found, _ = self.find_minecraft()
                if not found:
                    return False, None
            
            # Auto-resize if free repair is enabled
            if self.use_free_repair:
                self.resize_window()
                
            self.paused = False
            self.log("▶️ PLACING STARTED!")
            return True, self.window_title
        else:
            self.paused = True
            self.log("⏸️ PLACING STOPPED!")
            return False, self.window_title
    
    def start(self):
        self.running = True
        self.thread = threading.Thread(target=self.main_loop, daemon=True)
        self.thread.start()
    
    def stop(self):
        self.running = False
        self.paused = True
        if self.thread:
            self.thread.join(timeout=2)


class PlacerGUI(ctk.CTk):
    """
    Elegant, minimalist black and gray interface.
    """
    
    COLORS = {
        'bg_primary': '#0a0a0a',
        'bg_secondary': '#111111',
        'bg_card': '#161616',
        'bg_card_hover': '#1c1c1c',
        'bg_input': '#0d0d0d',
        
        'border': '#2a2a2a',
        'border_hover': '#3a3a3a',
        
        'text_primary': '#ffffff',
        'text_secondary': '#888888',
        'text_muted': '#555555',
        
        'accent': '#ffffff',
        'accent_dim': '#666666',
        
        'success': '#888888',
        'error': '#666666',
        'active': '#ffffff',
        'inactive': '#444444',
    }
    
    def __init__(self):
        super().__init__()
        
        # Force taskbar icon
        try:
            myappid = 'mcapple.bot.gui.1.0' # arbitrary string
            ctypes.windll.shell32.SetCurrentProcessExplicitAppUserModelID(myappid)
        except:
            pass
        
        self.title("Mc-Apple")
        self.geometry("420x820")
        self.resizable(False, False)
        self.configure(fg_color=self.COLORS['bg_primary'])
        
        try:
            self.iconbitmap("icon.ico")
        except:
            pass
        
        self.config = ConfigManager()
        
        self.placer = BlockPlacer(gui_callback=self.add_log)
        self.is_running = False
        
        self.create_widgets()
        
        # Check Admin Rights
        try:
            is_admin = ctypes.windll.shell32.IsUserAnAdmin()
            if not is_admin:
                self.after(500, lambda: self.add_log("⚠️ WARNING: Not running as Admin!"))
                self.after(600, lambda: self.add_log("   Input might be blocked by MC."))
        except:
            pass
            
        self.load_config_to_gui()
        self.setup_hotkeys()
        self.placer.start()
        self.after(1000, self.find_mc)
    
    def create_widgets(self):
        header = ctk.CTkFrame(self, fg_color=self.COLORS['bg_secondary'], corner_radius=0, height=70)
        header.pack(fill="x")
        header.pack_propagate(False)
        
        header_content = ctk.CTkFrame(header, fg_color="transparent")
        header_content.place(relx=0.5, rely=0.5, anchor="center")
        
        ctk.CTkLabel(header_content, text="◆", 
                    font=ctk.CTkFont(size=20),
                    text_color=self.COLORS['text_primary']).pack(side="left", padx=(0, 8))
        
        ctk.CTkLabel(header_content, text="Mc-Apple",
                    font=ctk.CTkFont(size=16, weight="bold"),
                    text_color=self.COLORS['text_primary']).pack(side="left")
        
        ctk.CTkFrame(self, fg_color=self.COLORS['border'], height=1).pack(fill="x")
        
        main = ctk.CTkFrame(self, fg_color=self.COLORS['bg_primary'])
        main.pack(fill="both", expand=True, padx=16, pady=12)
        
        status_card = ctk.CTkFrame(main, fg_color=self.COLORS['bg_card'], 
                                   corner_radius=12, height=70,
                                   border_width=1, border_color=self.COLORS['border'])
        status_card.pack(fill="x", pady=(0, 10))
        status_card.pack_propagate(False)
        
        status_left = ctk.CTkFrame(status_card, fg_color="transparent")
        status_left.pack(side="left", padx=16, pady=12)
        
        status_row = ctk.CTkFrame(status_left, fg_color="transparent")
        status_row.pack(anchor="w")
        
        self.status_dot = ctk.CTkLabel(status_row, text="●", 
                                       font=ctk.CTkFont(size=12), 
                                       text_color=self.COLORS['inactive'])
        self.status_dot.pack(side="left", padx=(0, 8))
        
        self.status_label = ctk.CTkLabel(status_row, text="WAITING", 
                                         font=ctk.CTkFont(size=13, weight="bold"), 
                                         text_color=self.COLORS['text_secondary'])
        self.status_label.pack(side="left")
        
        self.mc_label = ctk.CTkLabel(status_left, text="Searching for Minecraft...", 
                                     font=ctk.CTkFont(size=10), 
                                     text_color=self.COLORS['text_muted'])
        self.mc_label.pack(anchor="w", pady=(4, 0))
        
        cycle_container = ctk.CTkFrame(status_card, fg_color=self.COLORS['bg_secondary'], 
                                       corner_radius=8, width=60, height=45,
                                       border_width=1, border_color=self.COLORS['border'])
        cycle_container.pack(side="right", padx=16)
        cycle_container.pack_propagate(False)
        
        self.cycle_num = ctk.CTkLabel(cycle_container, text="0", 
                                      font=ctk.CTkFont(size=18, weight="bold"), 
                                      text_color=self.COLORS['text_primary'])
        self.cycle_num.pack(expand=True)
        
        creator_card = ctk.CTkFrame(main, fg_color=self.COLORS['bg_card'], 
                                    corner_radius=12,
                                    border_width=1, border_color=self.COLORS['border'])
        creator_card.pack(fill="x", pady=(0, 10))
        
        creator_content = ctk.CTkFrame(creator_card, fg_color="transparent")
        creator_content.pack(fill="x", padx=16, pady=12)
        
        creator_row = ctk.CTkFrame(creator_content, fg_color="transparent")
        creator_row.pack(fill="x")
        
        ctk.CTkLabel(creator_row, text=_AUTHOR_LABEL, 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_muted']).pack(side="left")
        
        ctk.CTkLabel(creator_row, text=_AUTHOR_NAME, 
                    font=ctk.CTkFont(size=12, weight="bold"), 
                    text_color=self.COLORS['text_primary']).pack(side="left", padx=(6, 0))
        
        socials_frame = ctk.CTkFrame(creator_row, fg_color="transparent")
        socials_frame.pack(side="right")
        
        discord_btn = ctk.CTkButton(socials_frame, text="Discord", 
                                    width=70, height=26,
                                    font=ctk.CTkFont(size=10),
                                    fg_color=self.COLORS['bg_secondary'],
                                    hover_color=self.COLORS['border'],
                                    text_color=self.COLORS['text_secondary'],
                                    corner_radius=6,
                                    command=self.open_discord)
        discord_btn.pack(side="left", padx=(0, 6))
        
        github_btn = ctk.CTkButton(socials_frame, text="GitHub", 
                                   width=70, height=26,
                                   font=ctk.CTkFont(size=10),
                                   fg_color=self.COLORS['bg_secondary'],
                                   hover_color=self.COLORS['border'],
                                   text_color=self.COLORS['text_secondary'],
                                   corner_radius=6,
                                   command=self.open_github)
        github_btn.pack(side="left")
        
        opts_card = ctk.CTkFrame(main, fg_color=self.COLORS['bg_card'], 
                                 corner_radius=12,
                                 border_width=1, border_color=self.COLORS['border'])
        opts_card.pack(fill="x", pady=(0, 10))
        
        opts_header = ctk.CTkFrame(opts_card, fg_color="transparent")
        opts_header.pack(fill="x", padx=16, pady=(12, 8))
        
        ctk.CTkLabel(opts_header, text="OPTIONS", 
                    font=ctk.CTkFont(size=10, weight="bold"), 
                    text_color=self.COLORS['text_muted']).pack(anchor="w")
        
        opts_content = ctk.CTkFrame(opts_card, fg_color="transparent")
        opts_content.pack(fill="x", padx=16, pady=(0, 12))
        
        row1 = ctk.CTkFrame(opts_content, fg_color="transparent")
        row1.pack(fill="x", pady=4)
        
        ctk.CTkLabel(row1, text="Enable /repair", 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_secondary']).pack(side="left")
        
        self.repair_sw = ctk.CTkSwitch(row1, text="", width=42, height=22,
                                       progress_color=self.COLORS['text_primary'],
                                       button_color=self.COLORS['text_primary'],
                                       button_hover_color=self.COLORS['text_secondary'],
                                       fg_color=self.COLORS['bg_input'],
                                       command=self.update_opts)
        self.repair_sw.pack(side="right")
        self.repair_sw.select()
        
        row1b = ctk.CTkFrame(opts_content, fg_color="transparent")
        row1b.pack(fill="x", pady=4)
        
        ctk.CTkLabel(row1b, text="Free Repair (Crafting)", 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_secondary']).pack(side="left")
        
        self.free_repair_sw = ctk.CTkSwitch(row1b, text="", width=42, height=22,
                                       progress_color=self.COLORS['text_primary'],
                                       button_color=self.COLORS['text_primary'],
                                       button_hover_color=self.COLORS['text_secondary'],
                                       fg_color=self.COLORS['bg_input'],
                                       command=self.update_opts)
        self.free_repair_sw.pack(side="right")
        
        row2 = ctk.CTkFrame(opts_content, fg_color="transparent")
        row2.pack(fill="x", pady=4)
        
        ctk.CTkLabel(row2, text="Repair every (cycles)", 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_secondary']).pack(side="left")
        
        self.repair_every_entry = ctk.CTkEntry(row2, width=60, height=28, 
                                               font=ctk.CTkFont(size=11),
                                               fg_color=self.COLORS['bg_input'],
                                               border_color=self.COLORS['border'],
                                               text_color=self.COLORS['text_primary'],
                                               justify="center",
                                               corner_radius=6)
        self.repair_every_entry.pack(side="right")
        self.repair_every_entry.insert(0, "100")
        
        # row3 removed (Action delay)
        
        ctk.CTkFrame(opts_content, fg_color=self.COLORS['border'], height=1).pack(fill="x", pady=8)
        
        row4 = ctk.CTkFrame(opts_content, fg_color="transparent")
        row4.pack(fill="x", pady=4)
        
        ctk.CTkLabel(row4, text="Slot Rotation (2→3→...→9)", 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_secondary']).pack(side="left")
        
        self.slot_rotation_sw = ctk.CTkSwitch(row4, text="", width=42, height=22,
                                               progress_color=self.COLORS['text_primary'],
                                               button_color=self.COLORS['text_primary'],
                                               button_hover_color=self.COLORS['text_secondary'],
                                               fg_color=self.COLORS['bg_input'],
                                               command=self.update_opts)
        self.slot_rotation_sw.pack(side="right")
        
        row5 = ctk.CTkFrame(opts_content, fg_color="transparent")
        row5.pack(fill="x", pady=4)
        
        ctk.CTkLabel(row5, text="Rotate every (cycles)", 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_secondary']).pack(side="left")
        
        self.slot_rotate_entry = ctk.CTkEntry(row5, width=60, height=28, 
                                              font=ctk.CTkFont(size=11),
                                              fg_color=self.COLORS['bg_input'],
                                              border_color=self.COLORS['border'],
                                              text_color=self.COLORS['text_primary'],
                                              justify="center",
                                              corner_radius=6)
        self.slot_rotate_entry.pack(side="right")
        self.slot_rotate_entry.insert(0, "500")
        
        ctk.CTkFrame(opts_content, fg_color=self.COLORS['border'], height=1).pack(fill="x", pady=8)
        
        row6 = ctk.CTkFrame(opts_content, fg_color="transparent")
        row6.pack(fill="x", pady=4)
        
        ctk.CTkLabel(row6, text="Eating (slot 2 reserved)", 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_secondary']).pack(side="left")
        
        self.eating_sw = ctk.CTkSwitch(row6, text="", width=42, height=22,
                                       progress_color=self.COLORS['text_primary'],
                                       button_color=self.COLORS['text_primary'],
                                       button_hover_color=self.COLORS['text_secondary'],
                                       fg_color=self.COLORS['bg_input'],
                                       command=self.update_opts)
        self.eating_sw.pack(side="right")
        
        row7 = ctk.CTkFrame(opts_content, fg_color="transparent")
        row7.pack(fill="x", pady=4)
        
        ctk.CTkLabel(row7, text="Eat every (cycles)", 
                    font=ctk.CTkFont(size=11), 
                    text_color=self.COLORS['text_secondary']).pack(side="left")
        
        self.eat_every_entry = ctk.CTkEntry(row7, width=60, height=28, 
                                            font=ctk.CTkFont(size=11),
                                            fg_color=self.COLORS['bg_input'],
                                            border_color=self.COLORS['border'],
                                            text_color=self.COLORS['text_primary'],
                                            justify="center",
                                            corner_radius=6)
        self.eat_every_entry.pack(side="right")
        self.eat_every_entry.insert(0, "1000")
        
        self.save_btn = ctk.CTkButton(main, 
                                      text="SAVE CONFIG",
                                      font=ctk.CTkFont(size=12, weight="bold"),
                                      fg_color=self.COLORS['bg_card'],
                                      hover_color=self.COLORS['border'],
                                      text_color=self.COLORS['text_secondary'],
                                      border_width=1,
                                      border_color=self.COLORS['border'],
                                      height=38,
                                      corner_radius=8,
                                      command=self.save_config)
        self.save_btn.pack(fill="x", pady=(0, 6))
        
        self.main_btn = ctk.CTkButton(main, 
                                      text="START",
                                      font=ctk.CTkFont(size=14, weight="bold"),
                                      fg_color=self.COLORS['text_primary'],
                                      hover_color=self.COLORS['text_secondary'],
                                      text_color=self.COLORS['bg_primary'],
                                      height=50,
                                      corner_radius=10,
                                      command=self.toggle_placer)
        self.main_btn.pack(fill="x", pady=(0, 10))
        
        log_card = ctk.CTkFrame(main, fg_color=self.COLORS['bg_card'], 
                                corner_radius=12,
                                border_width=1, border_color=self.COLORS['border'])
        log_card.pack(fill="both", expand=True)
        
        log_header = ctk.CTkFrame(log_card, fg_color="transparent")
        log_header.pack(fill="x", padx=16, pady=(12, 8))
        
        ctk.CTkLabel(log_header, text="ACTIVITY LOG", 
                    font=ctk.CTkFont(size=10, weight="bold"), 
                    text_color=self.COLORS['text_muted']).pack(side="left")
        
        ctk.CTkButton(log_header, text="Clear", 
                     width=50, height=22,
                     font=ctk.CTkFont(size=9),
                     fg_color=self.COLORS['bg_secondary'],
                     hover_color=self.COLORS['border'],
                     text_color=self.COLORS['text_secondary'],
                     corner_radius=6,
                     command=self.clear_log).pack(side="right")
        
        self.log_box = ctk.CTkTextbox(log_card, 
                                      fg_color=self.COLORS['bg_input'],
                                      text_color=self.COLORS['text_primary'],
                                      font=ctk.CTkFont(family="Consolas", size=11),
                                      corner_radius=8,
                                      border_width=1,
                                      border_color=self.COLORS['border'])
        self.log_box.pack(fill="both", expand=True, padx=12, pady=(0, 12))
        
        footer = ctk.CTkFrame(self, fg_color=self.COLORS['bg_secondary'], height=40)
        footer.pack(fill="x")
        footer.pack_propagate(False)
        
        ctk.CTkLabel(footer, text="F8 Start/Stop  •  F9 Exit", 
                    font=ctk.CTkFont(size=10),
                    text_color=self.COLORS['text_muted']).place(relx=0.5, rely=0.5, anchor="center")
        
        self.update_gui()
    
    def update_opts(self):
        self.placer.use_repair = self.repair_sw.get()
        self.placer.use_free_repair = self.free_repair_sw.get()
        try:
            self.placer.repair_every = max(1, int(self.repair_every_entry.get()))
        except:
            self.placer.repair_every = 100
        
        # Fixed delay
        self.placer.place_delay = 0.05
        
        self.placer.use_slot_rotation = self.slot_rotation_sw.get()
        try:
            self.placer.slot_rotate_every = max(1, int(self.slot_rotate_entry.get()))
        except:
            self.placer.slot_rotate_every = 500
        
        self.placer.use_eating = self.eating_sw.get()
        try:
            self.placer.eat_every = max(1, int(self.eat_every_entry.get()))
        except:
            self.placer.eat_every = 1000
        
        # Update starting slot when eating option changes
        if self.placer.use_eating and self.placer.current_slot < 2:
            self.placer.current_slot = 2  # Start from slot 3
    
    def setup_hotkeys(self):
        keyboard.add_hotkey(START_KEY, self.toggle_placer)
        keyboard.add_hotkey(EXIT_KEY, self.on_close)
    
    def toggle_placer(self):
        self.update_opts()
        
        result, title = self.placer.toggle()
        if result:
            self.status_label.configure(text="RUNNING", text_color=self.COLORS['text_primary'])
            self.status_dot.configure(text_color=self.COLORS['text_primary'])
            self.main_btn.configure(text="STOP", 
                                   fg_color=self.COLORS['border'],
                                   hover_color=self.COLORS['border_hover'],
                                   text_color=self.COLORS['text_primary'])
        else:
            self.status_label.configure(text="STOPPED", text_color=self.COLORS['text_secondary'])
            self.status_dot.configure(text_color=self.COLORS['inactive'])
            self.main_btn.configure(text="START", 
                                   fg_color=self.COLORS['text_primary'],
                                   hover_color=self.COLORS['text_secondary'],
                                   text_color=self.COLORS['bg_primary'])
    
    def find_mc(self):
        found, title = self.placer.find_minecraft()
        if found:
            short = title[:25] + "..." if len(title) > 25 else title
            self.mc_label.configure(text=f"● {short}", text_color=self.COLORS['text_secondary'])
        else:
            self.mc_label.configure(text="○ Not found", text_color=self.COLORS['text_muted'])
    
    def add_log(self, msg):
        self.after(0, lambda: (self.log_box.insert("end", msg + "\n"), self.log_box.see("end")))
    
    def clear_log(self):
        self.log_box.delete("1.0", "end")
    
    def update_gui(self):
        self.cycle_num.configure(text=str(self.placer.cycle_count))
        self.after(300, self.update_gui)
    
    def save_config(self):
        """Saves configuration to file."""
        config_dict = {
            'use_repair': self.repair_sw.get(),
            'use_free_repair': self.free_repair_sw.get(),
            'repair_every': int(self.repair_every_entry.get()) if self.repair_every_entry.get().isdigit() else 100,
            # 'place_delay': removed
            'use_slot_rotation': self.slot_rotation_sw.get(),
            'slot_rotate_every': int(self.slot_rotate_entry.get()) if self.slot_rotate_entry.get().isdigit() else 500,
            'use_eating': self.eating_sw.get(),
            'eat_every': int(self.eat_every_entry.get()) if self.eat_every_entry.get().isdigit() else 1000,
        }
        
        if self.config.save(config_dict):
            self.add_log("✅ Config saved!")
            self.save_btn.configure(text="SAVED!", text_color=self.COLORS['text_primary'])
            self.after(1500, lambda: self.save_btn.configure(text="SAVE CONFIG", text_color=self.COLORS['text_secondary']))
        else:
            self.add_log("❌ Failed to save config!")
    
    def load_config_to_gui(self):
        """Loads saved configuration to GUI."""
        if self.config.get('use_repair', True):
            self.repair_sw.select()
        else:
            self.repair_sw.deselect()
            
        if self.config.get('use_free_repair', False):
            self.free_repair_sw.select()
        else:
            self.free_repair_sw.deselect()
        
        self.repair_every_entry.delete(0, 'end')
        self.repair_every_entry.insert(0, str(self.config.get('repair_every', 238)))
        
        # self.delay_entry removed
        
        if self.config.get('use_slot_rotation', False):
            self.slot_rotation_sw.select()
        else:
            self.slot_rotation_sw.deselect()
        
        self.slot_rotate_entry.delete(0, 'end')
        self.slot_rotate_entry.insert(0, str(self.config.get('slot_rotate_every', 500)))
        
        if self.config.get('use_eating', False):
            self.eating_sw.select()
        else:
            self.eating_sw.deselect()
        
        self.eat_every_entry.delete(0, 'end')
        self.eat_every_entry.insert(0, str(self.config.get('eat_every', 1000)))
        
        self.update_opts()
    
    def open_discord(self):
        webbrowser.open(DISCORD_URL)
        self.add_log("🌐 Opening Discord...")
    
    def open_github(self):
        """Otwiera link GitHub w przeglądarce."""
        webbrowser.open(GITHUB_URL)
        self.add_log("🌐 Opening GitHub...")
    
    def on_close(self):
        self.placer.stop()
        self.destroy()
        import os
        os._exit(0)


if __name__ == "__main__":
    app = PlacerGUI()
    app.protocol("WM_DELETE_WINDOW", app.on_close)
    app.mainloop()
