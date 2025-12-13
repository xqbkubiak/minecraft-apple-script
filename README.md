# MC-Apple

A simple Minecraft automation script for apple farming. Built with Python and CustomTkinter for a sleek, modern interface.

![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Windows-lightgrey.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

## ✨ Features

- **Auto Block Placing** - Automatically switches slots and places blocks
- **Slot Rotation** - Rotates through slots 2-9 (or 3-9 with eating) every N cycles
- **Auto Repair** - Runs `/repair` command at configurable intervals
- **Free Repair (Crafting)** - Automatically crafts new shears for players without `/repair`
- **Auto Eating** - Automatically eats from slot 2 to keep hunger full
- **Minecraft Detection** - Smart window detection (ignores browsers, focuses on game)
- **Config Saving** - Saves your settings to JSON file
- **Modern UI** - Minimalist black & gray design

## 🚀 Installation (Easy Mode)

1. **Download** the repository (Code -> Download ZIP) and extract it.
2. Double click **`install_requirements.bat`**.
   - It will automatically install Python (if missing) and all dependencies.
3. Double click **`start_bot.bat`** (created by the installer) to run the bot.

## 🛠️ Manual Installation (Advanced)

1. **Clone the repository**
   ```bash
   git clone https://github.com/xqbkubiak/mc-apple.git
   cd mc-apple
   ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Run the script**
   ```bash
   python mc-apple.py
   ```

## ⚙️ Configuration

| Option | Description | Default |
|--------|-------------|---------|
| Enable /repair | Automatically run repair command | On |
| Free Repair (Crafting) | Craft new shears (for non-ranked players) | Off |
| Repair every | Cycles between repairs/crafting | 100 / 238 |
| Slot Rotation | Rotate through slots 2-9 | Off |
| Rotate every | Cycles between slot changes | 500 |
| Eating (slot 2) | Reserve slot 2 for food, auto eat | Off |
| Eat every | Cycles between eating | 1000 |

## ⌨️ Hotkeys

| Key | Action |
|-----|--------|
| `F8` | Start/Stop |
| `F9` | Exit |

## 📁 Files

- `mc-apple.py` - Main script
- `requirements.txt` - Python dependencies
- `icon.ico` - Application icon

## 🔧 How It Works

1. Switch to slot 2 (or slot 3 if eating enabled, or current rotation slot)
2. Right click (place block)
3. Switch back to slot 1
4. Left click
5. Repeat cycle
6. Run `/repair` OR Craft Shears every N cycles (if enabled)
7. Rotate slot every N cycles (if enabled)
8. Eat from slot 2 every N cycles (if eating enabled) - holds right click for 2.5s

### ✂️ Free Repair (Crafting Mode)
If you don't have access to `/repair`, enable **Free Repair**.
1. Have a stack of Iron Ingots in your inventory.
2. The bot will automatically open inventory, craft new shears, and resume farming.

## ⚠️ Disclaimer

This script is for educational purposes only. Use at your own risk. (sometimes they can ban u)

## 📜 License

MIT License - feel free to use and modify.

---

**Created by ru.su** • [Discord](https://discord.gg/getnotify)
