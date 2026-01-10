# 🛒 BK-Rynek

<p align="center">
  <img src="https://i.imgur.com/cjrtesr.png" alt="BK-Rynek Logo" width="128" height="128">
</p>

<p align="center">
  <b>Advanced Auction House Automation for Minecraft</b><br>
  <i>A professional-grade Fabric mod for automated market scanning, sniping, and inventory management. 🚀</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1--1.21.10-brightgreen?style=flat-square" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Mod%20Loader-Fabric-blue?style=flat-square" alt="Fabric">
  <img src="https://img.shields.io/badge/Java-21+-red?style=flat-square" alt="Java">
</p>

---

## 📌 About

**BK-Rynek** is an intelligent automated market bot with a clean, user-friendly GUI, featuring high-speed scanning, sniping, and automated Discord notifications—all running seamlessly in the background without requiring Minecraft to be in focus.

It streamlines the process of finding and purchasing items from the Auction House (AH) with extreme speed and precision. Whether you're a casual trader or a market tycoon, BK-Rynek gives you the edge you need to dominate the economy.

---

## ✨ Features

### 🤖 Automation
- **SCAN Mode** - Automatically cycles through Auction House pages to find items matching your price list.
- **SNIPER Mode** - Ultra-fast refreshing of the first page to catch newly listed items instantly.
- **Auto-Buy System** - Automatically handles purchase confirmation screens with configurable delays.

### 🌐 Discord
- **Webhooks** - Get real-time notifications on your Discord server whenever a successful purchase is made or an error occurs (e.g., insufficient funds).
- **Remote Monitoring** - Track your bot's performance while AFK.

### ⚙️ Manage
- **Profile System** - Create server-specific profiles with unique price lists and regex lore detection.
- **Dynamic Lore Parsing** - Advanced regex-based price extraction that works across various server formats.
- **AFK Controller** - Built-in anti-AFK jumping to keep you connected during long scanning sessions.
- **Professional GUI** - An intuitive in-game interface to manage your price lists and settings on the fly.

---

## 🎮 Usage

### ⚡ Main

| Command | Action |
|---------|--------|
| `/bkr` | Show main info and active profile |
| `/bkr gui` | Open the professional management panel |
| `/bkr scan` | Toggle the market scanner ON/OFF |
| `/bkr setup` | Start the interactive server setup wizard |
| `/bkr profiles` | List and switch between server profiles |
| `/bkr webhook <url>` | Set your Discord Webhook for notifications |
| `/bkr afk` | Toggle the built-in anti-AFK system |

### 🧩 Other
- `/bkr list` - View current price list for the active profile.
- `/bkr sounds <on/off>` - Toggle notification sounds.
- `/bkr config reload` - Reload all configuration files from disk.

---

## 📥 Installation

1. Download the latest `.jar` for your Minecraft version from the [Releases](https://github.com/xqbkubiak/minecraft-apple-script/releases) page.
2. Ensure you have [Fabric Loader](https://fabricmc.net/) installed.
3. Place the mod into your `%appdata%/.minecraft/mods` folder.
4. Launch the game and use `/bkr setup` to configure the mod for your favorite server.

---

## 🛠️ Config

The mod stores its configuration in the `.minecraft/config/bkrynek/` directory.
- `bkrynek-config.json` - Global settings and server profile mappings.
- Individual `.json` files for each profile (e.g., `minestar_boxpvp.json`) containing specific price lists.

---

## 🤝 Links

- **Main Website**: [bkubiak.dev/mods](https://bkubiak.dev/mods)
- **Discord**: [dc.bkubiak.dev](https://dc.bkubiak.dev)
- **Developer**: [@xqbkubiak](https://github.com/xqbkubiak)

---

## 📜 License

MIT License © 2026 bkubiakdev

<p align="center">
  <i>Built with precision for the modern Minecraft trader.</i>
</p>
