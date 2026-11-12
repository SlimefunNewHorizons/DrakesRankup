# Encuentro de Herobrine

`/herobrine` invoca un jefe por mundo permitido. El centro de la arena es su
punto de aparición; el radio predeterminado es de 48 bloques horizontales.
El encuentro no modifica terreno ni teletransporta jugadores. El jefe y sus
sombras regresan al centro si salen del radio.
El jefe y las sombras tienen desactivada la recogida de objetos para evitar
que la limpieza del encuentro elimine pertenencias caídas de los jugadores.

`/herobrine estado` consulta el combate del mundo actual sin invocar otro jefe
ni consumir la espera. Muestra fase, vida, tiempo restante, centro y radio de
arena, si estás dentro y tu daño efectivo frente al mínimo de participación.
La participación suficiente se revisa de nuevo al morir el jefe: debes seguir
vivo, dentro de la arena y en Survival/Adventure. Sin combate activo lo indica;
los argumentos desconocidos muestran el uso sin invocar nada.

- Fase 1: combate inicial; la barra muestra la fase actual.
- Fase 2, al 66% de vida: una única oleada de cuatro sombras y velocidad II.
- Fase 3, al 33%: fuerza II y un intento de blink cada seis segundos hacia un
  jugador vivo en Survival o Adventure. El destino exige suelo sólido, espacio libre y
  estar dentro de la arena y del borde del mundo.

Los poderes de fase duran hasta terminar el combate. Survival y Adventure
permiten combatir y obtener botín; creativo y espectador quedan excluidos.
El daño entre jugadores y jefe/sombras se cancela si cualquiera de los dos
está fuera del radio. Las flechas se validan según la posición y el modo del
tirador al impactar. El combate entre entidades ajenas al encuentro no cambia.

El crédito usa daño efectivo no cancelado, limitado a la vida restante del
jefe. Al morir, cada jugador vivo, conectado y dentro de la arena que alcance
`herobrine.minimum-reward-damage` (4 por defecto) recibe
`herobrine.reward-diamonds` (5 por defecto, entre 0 y 64). El mayor contribuyente
elegible recibe además una esencia. Cada premiado ve su daño y botín en el chat.
Los objetos que no caben en el inventario caen a sus pies. Las sombras no
aportan crédito; el reparto del jefe se realiza una sola vez por encuentro.

La experiencia también se entrega directamente a los participantes elegibles,
sin orbes que otros jugadores puedan recoger. `herobrine.reward-experience`
establece el total del encuentro (2000 XP por defecto): se divide por igual y
el resto entero lo recibe el mayor contribuyente elegible, junto a la esencia.
El chat indica los XP recibidos. Un valor de 0 o negativo desactiva esta parte
del botín. Sin participantes elegibles no se entrega experiencia; el presupuesto
no se multiplica por el número de jugadores.

El tiempo máximo predeterminado es de 900 segundos y la espera por invocador
es de 600 segundos. Al expirar o apagar el plugin se limpian las entidades y
la barra sin repartir botín. Los parámetros están en `config.yml`, sección
`herobrine`. Esta rama usa Paper 26.2 y Java 25; requiere ensayo con cliente
en staging antes de fusionar o desplegar.
