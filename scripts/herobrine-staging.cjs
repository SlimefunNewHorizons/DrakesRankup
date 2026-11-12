// Ensayo de protocolo real en un mundo desechable, exclusivamente en loopback.
// NODE_PATH=<node_modules con mineflayer> node scripts/herobrine-staging.cjs <staging-base>
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const os = require('node:os')
const { createHash } = require('node:crypto')
const { spawn } = require('node:child_process')
const mineflayer = require('mineflayer')

const source = path.resolve(process.argv[2] || '')
assert(fs.existsSync(path.join(source, 'drakespurpur-26.2-paperclip.jar')), 'Falta el servidor local de staging')
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'herobrine-staging-'))
const port = 25576
const jar = path.resolve(__dirname, '../target/DrakesRankup-v1.0.0.jar')
assert(fs.existsSync(jar), 'Ejecutar primero mvn clean package mediante build_seguro.sh')
fs.mkdirSync(path.join(root, 'plugins/DrakesRankup'), { recursive: true })
// Solo binarios reutilizables; nunca configuraciones, mundos, cuentas ni credenciales.
for (const name of ['libraries', 'cache']) fs.symlinkSync(path.join(source, name), path.join(root, name))
fs.cpSync(path.join(source, 'versions'), path.join(root, 'versions'), { recursive: true })
fs.symlinkSync(path.join(source, 'drakespurpur-26.2-paperclip.jar'), path.join(root, 'server.jar'))
for (const name of ['Vault.jar', 'LuckPerms-Bukkit-5.5.17.jar', 'ViaVersion-5.12.1.jar', 'ViaBackwards-5.12.1.jar']) {
  fs.copyFileSync(path.join(source, 'plugins', name), path.join(root, 'plugins', name))
}
fs.copyFileSync(jar, path.join(root, 'plugins/DrakesRankup.jar'))
fs.writeFileSync(path.join(root, 'eula.txt'), 'eula=true\n')
fs.writeFileSync(path.join(root, 'server.properties'), [
  'server-ip=127.0.0.1', `server-port=${port}`, 'online-mode=false', 'enable-rcon=false',
  'level-type=minecraft:flat', 'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}',
  'generate-structures=false', 'spawn-protection=0',
  'view-distance=3', 'simulation-distance=3', 'max-players=3', 'difficulty=normal',
  'allow-nether=false', 'sync-chunk-writes=false', 'pause-when-empty-seconds=-1', ''
].join('\n'))
fs.writeFileSync(path.join(root, 'plugins/DrakesRankup/config.yml'), [
  'settings:', '  world-filter:', '    enabled: false', '  maintenance:', '    enabled: false',
  'herobrine:', '  health: 400', '  damage: 1', '  cooldown-seconds: 0',
  '  lifetime-seconds: 180', '  arena-radius: 48', '  minimum-reward-damage: 4', '  reward-diamonds: 5', ''
].join('\n'))
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : '/usr/lib/jvm/java-25-openjdk-amd64/bin/java'
const server = spawn(java, ['-Xms256m', '-Xmx1536m', '-XX:ActiveProcessorCount=2', '-jar', 'server.jar', '--nogui'], { cwd: root })
const log = fs.createWriteStream(path.join(root, 'console.log'))
let output = ''
let exited = false
server.on('exit', () => { exited = true })
for (const stream of [server.stdout, server.stderr]) stream.on('data', chunk => { output += chunk.toString(); log.write(chunk) })
const bots = []
const evidence = { base: 'PR #2 / 803cf1e', fixture: root, client: 'Mineflayer 1.21.11 / ViaBackwards',
  jarSha256: createHash('sha256').update(fs.readFileSync(jar)).digest('hex'), assertions: [] }
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))
async function until(test, message, timeout = 15000) {
  const end = Date.now() + timeout
  while (Date.now() < end) {
    if (test()) return
    if (exited) throw new Error(`Servidor terminado: ${message}`)
    await delay(100)
  }
  throw new Error(`Timeout: ${message}; consultar ${root}/console.log`)
}
function command(text) { server.stdin.write(text + '\n') }
function passed(message) { evidence.assertions.push(message); console.log('PASS ' + message) }
async function client(username) {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port, username, auth: 'offline', version: '1.21.11' })
  bots.push(bot)
  bot.on('error', error => { evidence.error = String(error) })
  bot.on('kicked', reason => { evidence.error = JSON.stringify(reason) })
  bot.messages = []
  bot.on('messagestr', message => bot.messages.push(message))
  await until(() => bot.entity, `spawn ${username}`, 45000)
  return bot
}
function count(bot, name) { return bot.inventory.items().filter(item => item.name === name).reduce((sum, item) => sum + item.count, 0) }
const bossSelector = '@e[tag=herobrine_test,limit=1]'
async function main() {
  await until(() => output.includes('Done ('), 'arranque del staging', 180000)
  assert(output.includes('DrakesRankup v1.0.0 habilitado'), 'DrakesRankup no se habilitó')
  command('gamerule minecraft:spawn_mobs false')
  command('time set midnight')
  const a = await client('EnsayoHeroeA')
  const b = await client('EnsayoHeroeB')
  const spectator = await client('EnsayoObserva')
  for (const bot of bots) {
    command(`tp ${bot.username} 0 -60 0 0 0`)
    command(`effect give ${bot.username} minecraft:resistance infinite 4 true`)
    command(`item replace entity ${bot.username} weapon.mainhand with minecraft:netherite_sword`)
  }
  command('gamemode spectator EnsayoObserva')
  await delay(1000)
  a.chat('/herobrine')
  await until(() => a.bossBars.length === 1, 'barra de fase 1')
  command('tag @e[type=minecraft:zombie,sort=nearest,limit=1] add herobrine_test')
  command(`data merge entity ${bossSelector} {NoAI:1b}`)
  await delay(300)
  const boss = Object.values(a.entities).find(entity => entity.name === 'zombie')
  assert(boss, 'Cliente no ve al jefe')
  const bossId = boss.id
  assert.equal(a.bossBars[0].color, 'red')
  passed('Fase 1: jefe y barra recibidos por cliente real')
  command(`data merge entity ${bossSelector} {Health:260.0f}`)
  await until(() => a.messages.some(message => message.includes('Mis sombras')), 'fase 2')
  await until(() => Object.values(a.entities).filter(entity => ['zombie', 'wither_skeleton'].includes(entity.name)).length === 5, 'cuatro sombras')
  await until(() => a.bossBars[0].color === 'purple' && Math.abs(a.bossBars[0].health - 0.65) < 0.001, 'barra de fase 2')
  passed('Fase 2 al 65%: mensaje y cuatro sombras visibles')
  await delay(2200)
  assert.equal(Object.values(a.entities).filter(entity => ['zombie', 'wither_skeleton'].includes(entity.name)).length, 5)
  passed('Oleada única: no duplica sombras en ticks siguientes')
  command(`data merge entity ${bossSelector} {Health:120.0f}`)
  await until(() => a.messages.some(message => message.includes('Ya no hay escapatoria')), 'fase 3')
  await until(() => a.bossBars[0].color === 'white' && Math.abs(a.bossBars[0].health - 0.30) < 0.001, 'barra de fase 3')
  command(`tp ${bossSelector} 15 -60 0`)
  // NoAI impide confundir un desplazamiento por navegación con el blink.
  await until(() => a.entities[bossId] && a.entities[bossId].position.distanceTo(a.entity.position.offset(0, 0, -2)) < 0.6, 'blink detrás del jugador', 10000)
  passed('Fase 3 al 30%: blink real detrás de Survival sobre suelo libre')
  command(`tp ${bossSelector} 15 -60 0`)
  command('setblock 0 -60 -2 stone')
  await delay(6500)
  assert(a.entities[bossId].position.distanceTo(a.entity.position.offset(0, 0, -2)) > 1)
  command('setblock 0 -60 -2 air')
  passed('Blink bloqueado cuando el destino está ocupado')
  // Dos golpes físicos enviados por cada cliente; mayor contribución controlada al final.
  for (const player of [b, a]) {
    command(`tp ${bossSelector} 0 -60 2`)
    command(`tp ${player.username} 0 -60 0`)
    await delay(400)
    for (let hit = 0; hit < 8; hit++) { player.attack(player.entities[bossId]); await delay(650) }
  }
  command(`damage ${bossSelector} 1000 minecraft:generic_kill by EnsayoHeroeA`)
  await until(() => a.messages.some(message => message.includes('HEROBRINE DEFEATED')), 'muerte y reparto')
  await until(() => count(a, 'diamond') === 5 && count(b, 'diamond') === 5, 'diamantes de ambos participantes')
  assert.equal(count(a, 'nether_star'), 1)
  assert.equal(count(b, 'nether_star'), 0)
  assert.equal(count(spectator, 'diamond'), 0)
  assert.equal(count(spectator, 'nether_star'), 0)
  evidence.rewards = Object.fromEntries(bots.map(bot => [bot.username, { diamonds: count(bot, 'diamond'), essence: count(bot, 'nether_star') }]))
  passed('Recompensas por inventario recibido: 5 diamantes por participante, esencia solo al mayor; espectador sin premios')
  await until(() => a.bossBars.length === 0 && Object.values(a.entities).filter(entity => ['zombie', 'wither_skeleton'].includes(entity.name)).length === 0, 'limpieza del encuentro')
  passed('Muerte: desaparecen barra, jefe y cuatro sombras del cliente')
}
main().then(() => { evidence.ok = true }).catch(error => {
  evidence.ok = false
  evidence.error = String(error)
  console.error(error.message)
  process.exitCode = 1
}).finally(async () => {
  for (const bot of bots) bot.quit()
  if (!exited) { command('save-all flush'); command('stop') }
  await until(() => exited, 'apagado del staging', 30000).catch(() => server.kill('SIGTERM'))
  fs.writeFileSync(path.join(root, 'evidence.json'), JSON.stringify(evidence, null, 2) + '\n')
  console.log('EVIDENCE ' + path.join(root, 'evidence.json'))
  log.end()
})
