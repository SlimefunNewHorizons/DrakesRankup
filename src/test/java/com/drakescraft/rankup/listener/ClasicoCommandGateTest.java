package com.drakescraft.rankup.listener;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClasicoCommandGateTest {

    private ServerMock server;
    private DrakesRankupPlugin plugin;
    private WorldMock clasico;
    private WorldMock clasicoNether;
    private WorldMock clasicoEnd;
    private WorldMock survivalCustom;

    private static final List<String> COMANDOS_BLOQUEADOS = List.of(
            "hat", "ehat", "head", "ehead",
            "workbench", "eworkbench", "wb", "ewb", "wbench", "ewbench", "craft", "ecraft",
            "near", "enear", "nearby", "enearby",
            "back", "eback", "return", "ereturn",
            "anvil", "eanvil",
            "grindstone", "egrindstone",
            "cartographytable", "ecartographytable", "carttable", "ecarttable",
            "loom", "eloom",
            "smithingtable", "esmithingtable", "smithtable", "esmithtable",
            "stonecutter", "estonecutter"
    );

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        clasico = server.addSimpleWorld("clasico");
        clasicoNether = server.addSimpleWorld("clasico_nether");
        clasicoEnd = server.addSimpleWorld("clasico_the_end");
        survivalCustom = server.addSimpleWorld("world");
        plugin = MockBukkit.load(DrakesRankupPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerCommandPreprocessEvent fireCommand(PlayerMock player, World world, String rawCommand) {
        player.teleport(new Location(world, 0, 64, 0));
        while (player.nextMessage() != null) {
            // Limpiar cola previa de mensajes
        }
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, rawCommand);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void testBlocksAllComfortCommandsAndAliasesInClasicoWorlds() {
        PlayerMock player = server.addPlayer("JugadorClasico");
        for (World world : List.of(clasico, clasicoNether, clasicoEnd)) {
            assertFalse(plugin.isWorldAllowed(world), "El mundo " + world.getName() + " debe estar deshabilitado para rankup");
            for (String label : COMANDOS_BLOQUEADOS) {
                PlayerCommandPreprocessEvent event = fireCommand(player, world, "/" + label);
                assertTrue(event.isCancelled(), "Debe bloquear /" + label + " en " + world.getName());
                assertEquals(ClasicoCommandGate.MENSAJE_BLOQUEADO, player.nextMessage(),
                        "Mensaje de bloqueo esperado para /" + label + " en " + world.getName());
            }
        }
    }

    @Test
    void testBlocksNamespacedUppercaseArgumentsAndWhitespaceVariantsInClasico() {
        PlayerMock player = server.addPlayer("JugadorBypass");
        List<String> variants = List.of(
                "/HAT",
                "/Head",
                "/essentials:hat",
                "/essentials:head",
                "/essentials:workbench",
                "/essentials:wbench",
                "/essentials:near 200",
                "/essentials:enearby 50",
                "/essentials:back",
                "/essentials:anvil",
                "/essentials:carttable",
                "/essentials:eloom",
                "/essentials:smithtable",
                "/essentials:estonecutter",
                "/cmi:essentials:hat",
                "/ hat",
                "/hat\targumento",
                "/workbench   extra"
        );
        for (String raw : variants) {
            PlayerCommandPreprocessEvent event = fireCommand(player, clasico, raw);
            assertTrue(event.isCancelled(), "Debe bloquear variante: " + raw);
            assertEquals(ClasicoCommandGate.MENSAJE_BLOQUEADO, player.nextMessage());
        }
    }

    @Test
    void testAllowsComfortCommandsInCustomSurvivalAndForAdminsInClasico() {
        PlayerMock player = server.addPlayer("JugadorCustom");
        assertTrue(plugin.isWorldAllowed(survivalCustom));
        for (String cmd : List.of("/hat", "/head", "/workbench", "/wbench", "/near", "/back", "/anvil")) {
            PlayerCommandPreprocessEvent event = fireCommand(player, survivalCustom, cmd);
            assertFalse(event.isCancelled(), "No debe bloquear " + cmd + " en mundo permitido");
            assertNull(player.nextMessage());
        }

        PlayerMock admin = server.addPlayer("AdminClasico");
        admin.addAttachment(plugin, "drakesrankup.admin", true);
        for (String cmd : List.of("/hat", "/head", "/workbench", "/near", "/back", "/anvil")) {
            PlayerCommandPreprocessEvent event = fireCommand(admin, clasico, cmd);
            assertFalse(event.isCancelled(), "Admin con drakesrankup.admin no debe ser bloqueado en Clasico para " + cmd);
            assertNull(admin.nextMessage());
        }
    }

    @Test
    void testAllowsUnrelatedCommandsInClasico() {
        PlayerMock player = server.addPlayer("JugadorVanilla");
        for (String cmd : List.of("/spawn", "/help", "/msg otro hola", "/hatter", "/", "/   ")) {
            PlayerCommandPreprocessEvent event = fireCommand(player, clasico, cmd);
            assertFalse(event.isCancelled(), "No debe bloquear comando ajeno: " + cmd);
            assertNull(player.nextMessage());
        }
    }
}
