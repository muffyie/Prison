package tech.mcprison.prison.spigot.gui.autofeatures;

import java.util.List;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import tech.mcprison.prison.autofeatures.AutoFeaturesFileConfig;
import tech.mcprison.prison.autofeatures.AutoFeaturesFileConfig.AutoFeatures;
import tech.mcprison.prison.spigot.SpigotPrison;
import tech.mcprison.prison.spigot.gui.ListenersPrisonManager;
import tech.mcprison.prison.spigot.gui.SpigotGUIComponents;

/**
 * @author GABRYCA
 */
public class SpigotAutoBlockGUI extends SpigotGUIComponents {

    private final Player p;
    private final Configuration messages = messages();
    private final AutoFeaturesFileConfig afConfig = AutoFeaturesFileConfig();

    public SpigotAutoBlockGUI(Player p){
        this.p = p;
    }

    public void open() {

        // Create the inventory and set up the owner, dimensions or number of slots, and title
        int dimension = 36;
        Inventory inv = Bukkit.createInventory(null, dimension, SpigotPrison.format("&3AutoFeatures -> AutoBlock"));

        if (guiBuilder(inv)) return;

        openGUI(p, inv);
    }

    private boolean guiBuilder(Inventory inv) {
        try {
            buttonsSetup(inv);
        } catch (NullPointerException ex){
            p.sendMessage(SpigotPrison.format("&cThere's a null value in the GuiConfig.yml [broken]"));
            ex.printStackTrace();
            return true;
        }
        return false;
    }

    private void buttonsSetup(Inventory inv) {


        List<String> enabledLore = createLore(
                messages.getString("Lore.ShiftAndRightClickToDisable")
        );
        List<String> disabledLore = createLore(
                messages.getString("Lore.RightClickToEnable")
        );
        List<String> closeGUILore = createLore(
                messages.getString("Lore.ClickToClose")
        );

        ItemStack closeGUI = createButton(XMaterial.RED_STAINED_GLASS_PANE.parseItem(), closeGUILore, SpigotPrison.format("&c" + "Close"));
        inv.setItem(35, closeGUI);

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockAllBlocks ) ) {
            ItemStack Enabled = createButton(XMaterial.LIME_STAINED_GLASS_PANE.parseItem(), enabledLore, SpigotPrison.format("&a" + "All_Blocks Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(XMaterial.RED_STAINED_GLASS_PANE.parseItem(), disabledLore, SpigotPrison.format("&c" + "All_Blocks Disabled"));
            inv.addItem(Disabled);
        }

        if (  afConfig.isFeatureBoolean( AutoFeatures.autoBlockGoldBlock ) ) {
            ItemStack Enabled = createButton(Material.GOLD_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Gold_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.GOLD_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Gold_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockIronBlock ) ) {
            ItemStack Enabled = createButton(Material.IRON_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Iron_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.IRON_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Iron_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockCoalBlock ) ) {
            ItemStack Enabled = createButton(Material.COAL_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Coal_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.COAL_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Coal_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockDiamondBlock ) ) {
            ItemStack Enabled = createButton(Material.DIAMOND_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Diamond_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.DIAMOND_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Diamond_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockRedstoneBlock ) ) {
            ItemStack Enabled = createButton(Material.REDSTONE_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Redstone_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.REDSTONE_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Redstone_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockEmeraldBlock ) ) {
            ItemStack Enabled = createButton(Material.EMERALD_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Emerald_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.EMERALD_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Emerald_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockQuartzBlock ) ) {
            ItemStack Enabled = createButton(Material.QUARTZ_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Quartz_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.QUARTZ_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Quartz_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockPrismarineBlock ) ) {
            ItemStack Enabled = createButton(Material.PRISMARINE, 1, enabledLore, SpigotPrison.format("&a" + "Prismarine_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.PRISMARINE, 1, disabledLore, SpigotPrison.format("&c" + "Prismarine_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockLapisBlock ) ) {
            ItemStack Enabled = createButton(Material.LAPIS_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Lapis_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.LAPIS_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Lapis_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockSnowBlock ) ) {
            ItemStack Enabled = createButton(Material.SNOW_BLOCK, 1, enabledLore, SpigotPrison.format("&a" + "Snow_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.SNOW_BLOCK, 1, disabledLore, SpigotPrison.format("&c" + "Snow_Block Disabled"));
            inv.addItem(Disabled);
        }

        if ( afConfig.isFeatureBoolean( AutoFeatures.autoBlockGlowstone ) ) {
            ItemStack Enabled = createButton(Material.GLOWSTONE, 1, enabledLore, SpigotPrison.format("&a" + "Glowstone_Block Enabled"));
            inv.addItem(Enabled);
        } else {
            ItemStack Disabled = createButton(Material.GLOWSTONE, 1, disabledLore, SpigotPrison.format("&c" + "Glowstone_Block Disabled"));
            inv.addItem(Disabled);
        }
    }
}
