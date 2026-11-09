<div align="center">

  <img src="https://raw.githubusercontent.com/SlimefunNewHorizons/DrakesRankup/main/banner.svg" alt="DrakesRankup Banner" width="920" />

# ⚡ DrakesRankup — Drake Edition

**Modular Rank Progression, Dynamic Requirements Engine & Multi-Tier Rewards**

<p>
  <a href="https://github.com/SlimefunNewHorizons/DrakesRankup"><img src="https://img.shields.io/badge/GitHub-DrakesRankup-181717?style=for-the-badge&logo=github" alt="GitHub"/></a>
  <img src="https://img.shields.io/badge/Slimefun4-Drake_Edition-22C55E?style=for-the-badge&logo=curseforge&logoColor=white" alt="Slimefun4"/>
  <img src="https://img.shields.io/badge/Paper-1.21.11-38BDF8?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
  <img src="https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
</p>

[🇬🇧 **English**](README.md) · [🇪🇸 **Español**](README_ES.md)

</div>

> ### 🏰 Join the Official DrakesCraft Community!
> 
> * 🎮 **Server IP**: `mc.drakescraft.cl` *(Java 1.21.11 & Bedrock Port 25565 / 19132)*
> * 💬 **Official Discord**: [discord.gg/drakescraft](https://discord.gg/rv3vtXZTk7) — *Check out `#general-english`!*
> * 🌐 **Website & Guides**: [web.drakescraft.cl](https://web.drakescraft.cl) — 🛒 **Store**: [web.drakescraft.cl/store](https://web.drakescraft.cl/store.html)
> 
> *Play with this addon alongside 80+ optimized expansions live on our technical survival network!*

---

## 📖 What is DrakesRankup?

DrakesRankup drives player progression with customizable rank requirements (Dracmas, playtime, block breaks, Slimefun researches) and automated perk unlocking.

---

## ⚙️ Key Features & Systems

* 🏆 **Flexible Requirements**: Combine economy costs, playtime thresholds, and Slimefun researches into rank criteria.
* 📋 **Intuitive GUI Ladder**: Visual `/rankup` and `/ranks` interfaces showing current progress percentages.
* 🎁 **Automated Tier Rewards**: Grant commands, permissions, vault pages, and crate keys upon ranking up.

---

## 📋 Technical Compatibility

| Parameter | Requirement |
|---|---|
| **Server Software** | Paper / Purpur / Folia **1.21.11** |
| **Java Runtime** | **Java 21** LTS |
| **Required Core** | [Slimefun4-Drake](https://github.com/SlimefunNewHorizons/Slimefun4-Drake) |
| **Architecture** | 100% Server-Side (Vanilla Minecraft clients can join without installing client mods) |

---

## 📥 Installation

1. Download the latest release `.jar` from the [Releases](https://github.com/SlimefunNewHorizons/DrakesRankup/releases) tab or [Modrinth](https://modrinth.com/organization/drakescraft-labs).
2. Place the `.jar` into your server's `plugins/` directory alongside `Slimefun4-Drake.jar`.
3. Start or restart your server. Recipes and machines will appear automatically in the Slimefun guide (`/sf guide`).

---

## 🛠️ Building from Source

```bash
git clone https://github.com/SlimefunNewHorizons/DrakesRankup.git
cd DrakesRankup
mvn clean package -DskipTests
```

The compiled artifact will be located under `target/DrakesRankup.jar`.

---

<div align="center">

**Developed and Maintained by [DrakesCraft Labs](https://github.com/SlimefunNewHorizons)**  
Licensed under **MIT**.

</div>

---

## 📄 License & Upstream Attribution

This project is a sovereign fork maintained by [**JackStar6677-1**](https://github.com/JackStar6677-1) under [**DrakesCraft Labs**](https://github.com/SlimefunNewHorizons).

- **Original Project:** Created by the upstream authors and the open-source community ([Upstream Repository](https://github.com/Slimefun)).
- **DrakesCraft Optimizations:** Modernized for Paper/Purpur 1.21.11+, Java 21 LTS, high concurrency, asynchronous safety, and exploit/duplication prevention.
- **License:** Distributed under the original **MIT** license terms. See the [LICENSE](LICENSE) file for complete details.
