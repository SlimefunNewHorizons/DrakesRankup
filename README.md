<div align="center">

![DrakesRankup](banner.svg)

# ⛩️ DrakesRankup

**Sistema de progresión por rangos anime/shōnen (100 tiers / 10 Divisiones), auras, poderes y sumidero económico de late-game para DrakesCraft.**

<p>
  <img src="https://img.shields.io/badge/Purpur-26.2-8B5CF6?style=for-the-badge&logo=minecraft&logoColor=white" alt="Purpur 26.2"/>
  <img src="https://img.shields.io/badge/Java-25_LTS-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 25"/>
  <img src="https://img.shields.io/badge/Version-1.0.0-22C55E?style=for-the-badge" alt="Version 1.0.0"/>
</p>

Purpur 26.2 · Java 25 · Vault · LuckPerms · PlaceholderAPI

</div>

> ### 🏰 Comunidad Oficial de DrakesCraft
>
> * 🎮 **IP del Servidor**: `mc.drakescraft.cl` *(Java & Bedrock)*
> * 💬 **Discord Oficial**: [discord.gg/rv3vtXZTk7](https://discord.gg/rv3vtXZTk7)
> * 🌐 **Portal Web & Guía**: [web.drakescraft.cl](https://web.drakescraft.cl)
> * 🛒 **Tienda Oficial**: [drakescraft-v2.tebex.io](https://drakescraft-v2.tebex.io)

---

## Qué hace

`DrakesRankup` es la escalera de progresión de largo plazo de DrakesCraft: **100 tiers (10 Divisiones)** de rango que el jugador sube pagando desde su economía, cada uno con **aura visual, habilidades y transformaciones** de temática anime. Está diseñado como **sumidero económico** para dar a los jugadores avanzados objetivos de late-game sin romper el balance del servidor.

Opera exclusivamente en modalidades personalizadas (**Slimefun, OneBlock, SkyBlock**); la modalidad **Clásico (Vanilla)** queda excluida por diseño.

---

## Características

### Progresión (100 Tiers / 10 Divisiones)
- **100 tiers** con coste creciente y sincronización en tiempo real con **LuckPerms**.
- **10 Divisiones (I a X)** navegables mediante GUI interactiva (`/ranks` o `/rangos`) con navegación rápida entre Divisiones I–V y VI–X.
- **Auto-reparación al conectar:** si el grupo de LuckPerms y el tier registrado se desalinean (incluso offline), se reconcilian automáticamente.
- **Bolsa de ascenso sin tope:** permite acumular pagos para rangos que superan el límite de dinero en mano.

### Habilidades y Combate
- **Golpe Serio de Saitama (True Damage):** onda sónica (`DamageType.SONIC_BOOM`) que atraviesa armaduras de late-game, inutiliza escudos rivales por 8 segundos y aplica knockback con partículas Warden Sonic Boom y efectos de aturdimiento.
- **Empuje cinético (Shunpo):** impulso direccional con doble salto y cooldown.
- **Vuelo de Ki (tier 31+):** dash sónico con inmunidad a daño por caída.
- **Convivencia limpia con vuelo legítimo:** si el jugador cuenta con fly legítimo (`essentials.fly`, FlyingBubble, AngelGem o staff), el doble salto activa el vuelo y no se secuestra como dash.

### Auras y Efectos Visuales
- **Cobertura continua tier 1–100**, escalando densidad de partículas por división.
- **Auras de transformación** para poderes anime activos (MUI, Ultra Ego, Gohan Beast, Broly, SSJ Blue/God, etc.).
- **Tres niveles de intensidad:** `completo → reducido → apagado` para optimizar rendimiento en clientes modestos.

### Administración
- **Modo Ángel (`/angel`):** invulnerabilidad y aura de halo doble para administración.

---

## Compatibilidad

| Componente | Requisito |
| :--- | :--- |
| **Servidor** | Paper / Purpur **26.2** (rama `port-26x`, Temporada 2) |
| **Java** | **Java 25** (LTS) |
| **Dependencias opcionales** | Vault, LuckPerms, PlaceholderAPI |
| **Versión** | `1.0.0` |

---

## Comandos

| Comando | Alias | Descripción |
| :--- | :--- | :--- |
| `/rankup` | `/ranks`, `/rangos`, `/subirrango` | Abre la GUI de rangos o gestiona el ascenso. |
| `/rankup max [jugador]` | — | Sube al máximo tier posible según el saldo actual. |
| `/rankup particles` | — | Cicla la intensidad del aura: completo → reducido → apagado. |
| `/rankup admin set <jugador> <tier\|max>` | — | Fija el tier exacto (1–100) o máximo de un jugador *(admin)*. |
| `/rankup admin max <jugador>` | — | Lleva a un jugador directamente al Tier 100 *(admin)*. |
| `/transform` | `/transformaciones`, `/ki`, `/poderes` | Abre la GUI de transformaciones y poderes anime. |
| `/angel` | `/zenosama`, `/zeno`, `/daishinkan` | Modo staff de deidad *(permiso `drakesrankup.staff`)*. |

## Permisos

| Permiso | Descripción |
| :--- | :--- |
| `drakesrankup.staff` | Acceso a comandos de staff (`/angel` y gestión). |
| `drakesrankup.admin` | Bypass total y gestión administrativa de tiers. |
| `essentials.fly` | Reconocido como vuelo legítimo para no interferir con habilidades de dash. |

---

## Compilación

Requisitos: **JDK 25** y **Maven 3.9+**.

```bash
mvn clean package
```

El artefacto compilado se genera en `target/DrakesRankup-v1.0.0.jar`.

---

## 📄 License & Intellectual Property

Copyright © 2026 [**JackStar6677-1**](https://github.com/JackStar6677-1) · [**DrakesCraft Labs**](https://github.com/SlimefunNewHorizons). All Rights Reserved.

This software is **Source-Available** for public inspection and technical audit. Redistribution, commercial repackaging, or unauthorized derivative distribution without explicit written permission from the author is strictly prohibited.
