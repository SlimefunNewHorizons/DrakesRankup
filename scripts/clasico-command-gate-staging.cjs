// Ensayo de protocolo real en mundo desechable `clasico`, exclusivamente en loopback.
// NODE_PATH=/home/jack/ai-hub/src/saori-bot/node_modules node scripts/clasico-command-gate-staging.cjs /home/jack/workspace/season2-staging
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const os = require('node:os')
const { createHash } = require('node:crypto')
const { spawn } = require('node:child_process')
const mineflayer = require('mineflayer')

const source = path.resolve(process.argv[2] || '')
assert(fs.existsSync(path.join(source, 'drakespurpur-26.2-paperclip.jar')), 'Falta el servidor local de staging')
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'clasico-gate-staging-'))
const port = 25578
const jar = path.resolve(__dirname, '../target/DrakesRankup-v1.0.0.jar')
assert(fs.existsSync(jar), 'Ejecutar primero mvn clean package mediante build_seguro.sh')

fs.mkdirSync(path.join(root, 'plugins/DrakesRankup'), { recursive: true })
for (const name of ['libraries', 'cache']) fs.symlinkSync(path.join(source, name), path.join(root, name))
fs.cpSync(path.join(source, 'versions'), path.join(root, 'versions'), { recursive: true })
fs.symlinkSync(path.join(source, 'drakespurpur-26.2-paperclip.jar'), path.join(root, 'server.jar'))
for (const name of ['Vault.jar', 'EssentialsX-2.22.0.jar', 'LuckPerms-Bukkit-5.5.17.jar', 'ViaVersion-5.12.1.jar', 'ViaBackwards-5.12.1.jar']) {
  fs.copyFileSync(path.join(source, 'plugins', name), path.join(root, 'plugins', name))
}
fs.copyFileSync(jar, path.join(root, 'plugins/DrakesRankup.jar'))
fs.writeFileSync(path.join(root, 'eula.txt'), 'eula=true\n')
fs.writeFileSync(path.join(root, 'server.properties'), [
  'server-ip=127.0.0.1', `server-port=${port}`, 'online-mode=false', 'enable-rcon=false',
  'level-name=clasico', 'level-type=minecraft:flat',
  'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}',
  'generate-structures=false', 'spawn-protection=0',
  'view-distance=3', 'simulation-distance=3', 'max-players=3', 'difficulty=peaceful',
  'allow-nether=false', 'sync-chunk-writes=false', 'pause-when-empty-seconds=-1', ''
].join('\n'))
fs.writeFileSync(path.join(root, 'plugins/DrakesRankup/config.yml'), [
  'settings:',
  '  world-filter:',
  '    enabled: true',
  '    mode: "BLACKLIST"',
  '    disabled-worlds:',
  '      - "clasico"',
  '      - "clasico_nether"',
  '      - "clasico_the_end"',
  '  maintenance:',
  '    enabled: false',
  ''
].join('\n'))

const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : '/usr/lib/jvm/java-25-openjdk-amd64/bin/java'
const server = spawn(java, ['-Xms256m', '-Xmx1536m', '-XX:ActiveProcessorCount=2', '-jar', 'server.jar', '--nogui'], { cwd: root })
const log = fs.createWriteStream(path.join(root, 'console.log'))
let output = ''
let exited = false
server.on('exit', () => { exited = true })
for (const stream of [server.stdout, server.stderr]) stream.on('data', chunk => { output += chunk.toString(); log.write(chunk) })

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

const BLOCKED_MSG = '[DrakesCraft] Ese comando no esta disponible en Clasico: aqui se juega vanilla.'
const COMMANDS_TO_VERIFY = [
  '/hat', '/ehat', '/head', '/ehead', '/essentials:hat', '/essentials:head',
  '/workbench', '/eworkbench', '/wb', '/ewb', '/wbench', '/ewbench', '/craft', '/ecraft',
  '/near', '/enear', '/nearby', '/enearby',
  '/back', '/eback', '/return', '/ereturn',
  '/anvil', '/eanvil',
  '/grindstone', '/egrindstone',
  '/cartographytable', '/ecartographytable', '/carttable', '/ecarttable',
  '/loom', '/eloom',
  '/smithingtable', '/esmithingtable', '/smithtable', '/esmithtable',
  '/stonecutter', '/estonecutter'
]

const evidence = {
  startedAtUtc: new Date().toISOString(),
  fixture: root,
  world: 'clasico',
  client: 'Mineflayer 1.21.11 / ViaBackwards -> Purpur 26.2',
  jarSha256: createHash('sha256').update(fs.readFileSync(jar)).digest('hex'),
  blockedCommandsTested: [],
  adminBypassVerified: false,
  cleanShutdown: false
}

async function main() {
  let bot = null
  try {
    await until(() => output.includes('Done ('), 'arranque del staging', 180000)
    assert(output.includes('DrakesRankup v1.0.0 habilitado'), 'DrakesRankup no se habilitó')

    bot = mineflayer.createBot({ host: '127.0.0.1', port, username: 'ClasicoTester', auth: 'offline', version: '1.21.11' })
    bot.messages = []
    bot.windowsOpened = 0
    bot.on('messagestr', message => bot.messages.push(message))
    bot.on('windowOpen', () => { bot.windowsOpened += 1 })
    await until(() => bot.entity, 'spawn ClasicoTester', 45000)

    const perms = [
      'essentials.hat', 'essentials.workbench', 'essentials.near', 'essentials.back',
      'essentials.anvil', 'essentials.grindstone', 'essentials.cartographytable',
      'essentials.loom', 'essentials.smithingtable', 'essentials.stonecutter'
    ]
    for (const perm of perms) {
      command(`lp user ClasicoTester permission set ${perm} true`)
      await delay(150)
    }
    command('minecraft:item replace entity ClasicoTester weapon.mainhand with minecraft:glass 1')
    await delay(1500)

    for (const rawCmd of COMMANDS_TO_VERIFY) {
      const prevCount = bot.messages.length
      const prevWindows = bot.windowsOpened
      bot.chat(rawCmd)
      await until(
        () => bot.messages.slice(prevCount).some(m => m.includes(BLOCKED_MSG)),
        `bloqueo de ${rawCmd} en clasico`,
        10000
      )
      assert.equal(bot.windowsOpened, prevWindows, `No debe abrirse ventana al ejecutar ${rawCmd} en clasico`)
      const helmet = bot.inventory.slots[5]
      assert.equal(helmet, null, `No debe equiparse casco tras ${rawCmd} en clasico`)
      evidence.blockedCommandsTested.push(rawCmd)
      console.log(`PASS bloqueado en clasico: ${rawCmd}`)
      await delay(1100)
    }

    // Verificar bypass con permiso drakesrankup.admin
    command('lp user ClasicoTester permission set drakesrankup.admin true')
    await delay(1200)
    const beforeAdmin = bot.messages.length
    bot.chat('/workbench')
    await until(() => bot.windowsOpened > 0 || bot.messages.length > beforeAdmin, 'respuesta admin /workbench', 10000)
    const adminMsgs = bot.messages.slice(beforeAdmin)
    assert(!adminMsgs.some(m => m.includes(BLOCKED_MSG)), 'Admin con drakesrankup.admin no debe recibir bloqueo de ClasicoCommandGate')
    evidence.adminBypassVerified = true
    console.log('PASS bypass drakesrankup.admin verificado en clasico')
  } finally {
    if (bot) {
      try { bot.quit() } catch (_) {}
    }
    if (!exited) {
      command('save-all flush')
      await delay(1000)
      command('stop')
      await until(() => exited, 'apagado limpio del staging', 45000)
    }
    log.end()
  }
  evidence.cleanShutdown = exited
  evidence.finishedAtUtc = new Date().toISOString()
  const evidencePath = path.join(root, 'evidence.json')
  fs.writeFileSync(evidencePath, JSON.stringify(evidence, null, 2) + '\n')
  console.log(`EVIDENCE ${evidencePath}`)
}

main().catch(err => {
  console.error(err)
  process.exit(1)
})
