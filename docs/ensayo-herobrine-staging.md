# Ensayo de Herobrine con clientes de protocolo

`scripts/herobrine-staging.cjs` levanta Purpur 26.2 y tres clientes Mineflayer
1.21.11 mediante ViaBackwards en `127.0.0.1:25576`. Crea un mundo y configuraciones
desechables bajo `/tmp/herobrine-staging-*`; no utiliza mundos, cuentas,
credenciales ni configuraciones del staging base. No conecta a Dallas.

Requiere Java 25, Node, Mineflayer instalado y los binarios del staging local:
Purpur 26.2, Vault, LuckPerms 5.5.17, ViaVersion/ViaBackwards 5.12.1 y sus
bibliotecas/cache. El puerto 25576 debe estar libre.

```bash
~/ai-hub/scripts/build_seguro.sh 25 "$PWD" mvn -o -B clean package
NODE_PATH=~/ai-hub/src/saori-bot/node_modules \
  node scripts/herobrine-staging.cjs ~/workspace/season2-staging
```

Comprueba la barra roja inicial, la transición al 65% con barra púrpura y cuatro
sombras sin duplicación, y al 30% con barra blanca. Mantiene al jefe con `NoAI`
para distinguir su blink de la navegación: debe aparecer detrás de un jugador
Survival y rechazar un destino ocupado.

Los dos participantes atacan con espadas de netherita mediante paquetes de
cliente. La consola fija los umbrales de salud y atribuye el golpe final al
participante A para hacer determinista el mayor contribuyente. Se comprueban
los inventarios recibidos: cinco diamantes por participante, una esencia solo
para A y ningún premio para el espectador. También verifica la desaparición
del jefe, sombras y barra.

Guarda `console.log` y `evidence.json` con el SHA-256 del jar, las aserciones y
el resultado. Solicita `save-all flush` y `stop` al finalizar, también ante
errores. Los archivos de evidencia permanecen para inspección.

Es una prueba de integración con servidor y clientes de protocolo reales.
No comprueba render gráfico, texturas, una sesión humana ni la paridad con
los demás plugins de Dallas. Solo prueba la revisión de PR #2; las PR
incrementales #3–#8 necesitan su propio ensayo antes de desplegarse.

## Resultado del 12 de noviembre de 2026

Sobre `803cf1e`, el build seguro aprobó 40 pruebas. El script publicado en
`ec4f474` completó sus siete comprobaciones con Mineflayer 4.37.1 y Purpur
26.2-138. Una lectura adicional del NBT guardado confirmó los objetos y los
2000 XP: A recibió 763 XP, B 1237 y el espectador 0. El log del ensayo final
contiene cero errores y confirma el guardado y el apagado.

[Evidencia estructurada](evidence/herobrine-staging-20261112.json).
La primera pasada no alcanzó el mínimo de participación con golpes sin arma;
el ensayo final equipa espadas de netherita sin cambiar ese mínimo ni el jefe.
