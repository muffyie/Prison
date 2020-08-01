package tech.mcprison.prison.spigot.sellall;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.mcprison.prison.Prison;
import tech.mcprison.prison.PrisonAPI;
import tech.mcprison.prison.integration.EconomyIntegration;
import tech.mcprison.prison.integration.IntegrationType;
import tech.mcprison.prison.modules.Module;
import tech.mcprison.prison.modules.ModuleManager;
import tech.mcprison.prison.ranks.PrisonRanks;
import tech.mcprison.prison.spigot.SpigotPrison;
import tech.mcprison.prison.spigot.game.SpigotPlayer;
import tech.mcprison.prison.spigot.gui.sellall.SellAllAdminGUI;
import tech.mcprison.prison.spigot.gui.sellall.SellAllPlayerGUI;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * @author GABRYCA
 */
public class SellAllCommands implements CommandExecutor {

    // Check if the SellAll's enabled
	public static boolean isEnabled() {
		return Objects.requireNonNull(SpigotPrison.getInstance().getConfig().getString("sellall")).equalsIgnoreCase("true");
	}
	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    	if (!isEnabled()){
            sender.sendMessage(SpigotPrison.format("&3[PRISON ERROR]&c Sorry but the SellAll Feature's disabled in the config.yml"));
            return true;
        }

    	File file = new File(SpigotPrison.getInstance().getDataFolder() + "/SellAllConfig.yml");
    	FileConfiguration conf = YamlConfiguration.loadConfiguration(file);
    	
        if (args.length == 0){
            if (sender.hasPermission("prison.admin") || sender.isOp()) {
                sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Please use a command like /sellall sell-gui-add-delete-multiplier"));
            } else {
                return sellallCommandSell(sender, conf);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("gui")){

            return sellallCommandGUI(sender, conf);

        } else if (args[0].equalsIgnoreCase("sell")){

            return sellallCommandSell(sender, conf);

        } else if (args[0].equalsIgnoreCase("add")){

            return sellallCommandAdd(sender, args, file, conf);

        } else if (args[0].equalsIgnoreCase("delete")){

            return sellallCommandDelete(sender, args, file, conf);

        } else if (args[0].equalsIgnoreCase("edit")){

            return sellallCommandEdit(sender, args, file, conf, "] edited with success!");

        } else if (args[0].equalsIgnoreCase("multiplier")){

            if (!(Objects.requireNonNull(conf.getString("Options.Multiplier_Enabled")).equalsIgnoreCase("true"))){
                sender.sendMessage(SpigotPrison.format("&3[PRISON WARN] &cMultipliers are disabled in the SellAll config"));
                return true;
            }

            if (args.length != 3){
                sender.sendMessage(SpigotPrison.format("&c[PRISON WARN] &cWrong format, please use /sellall multiplier <Prestige> <Multiplier>"));
                return true;
            }

            if (Objects.requireNonNull(conf.getString("Options.Multiplier_Command_Permission_Enabled")).equalsIgnoreCase("true")){
                if (!(sender.hasPermission(Objects.requireNonNull(conf.getString("Options.Multiplier_Command_Permission"))))){
                    sender.sendMessage(SpigotPrison.format("&3[PRISON WARN] &cSorry, but you don't have the permission [" + conf.getString("Options.Multiplier_Command_Permission") + "]"));
                    return true;
                }
            }

            // Add check if the Prestige is present
            // Add to the SellAllCommandSell the check for Player prestige, if prestiges are enabled and if player have a prestige or use default, also if there're multipliers for that prestige or use default

            ModuleManager modMan = Prison.get().getModuleManager();
            Module module = modMan == null ? null : modMan.getModule( PrisonRanks.MODULE_NAME ).orElse( null );

            PrisonRanks rankPlugin = (PrisonRanks) module;
            if (rankPlugin == null){
                sender.sendMessage(SpigotPrison.format("&3[PRISON ERROR] &cThe Ranks module's disabled or not found!"));
                return true;
            }

            boolean isPrestigeLadder = rankPlugin.getLadderManager().getLadder("prestiges").isPresent();

            if (!isPrestigeLadder){
                sender.sendMessage(SpigotPrison.format("&3[PRISON WARN] &cCan't find a -prestiges- ladder, they might be disabled in the config.yml."));
                return true;
            }

            boolean isARank = rankPlugin.getRankManager().getRank(args[1]).isPresent();

            if (!isARank){
                sender.sendMessage(SpigotPrison.format("&3[PRISON WARN] &cCan't find the Prestige/Rank: " + args[1]));
                return true;
            }

            boolean isInPrestigeLadder = rankPlugin.getLadderManager().getLadder("prestiges").get().containsRank(rankPlugin.getRankManager().getRank(args[1]).get().id);

            if (!isInPrestigeLadder){
                sender.sendMessage(SpigotPrison.format("&3[PRISON WARN] &cThe -prestiges- ladder doesn't contains the Rank: " + args[1]));
                return true;
            }

            double multiplier;
            try {
                multiplier = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Sorry but the multiplier isn't a number [/sellall multiplier " + args[1] + " Here-> " + args[2] + " <-"));
                return true;
            }

            conf.set("Multiplier." + args[1] + ".PRESTIGE_NAME", args[1]);
            conf.set("Multiplier." + args[1] + ".MULTIPLIER", multiplier);
            try {
                conf.save(file);
            } catch (IOException e) {
                sender.sendMessage(SpigotPrison.format("&3[PRISON ERROR] &cSorry, the config didn't save with success!"));
            }

            sender.sendMessage(SpigotPrison.format("&3[PRISON] &aMultiplier got added or edited with success!"));

            return true;

        }

        return true;
    }

    private boolean sellallCommandEdit(CommandSender sender, String[] args, File file, FileConfiguration conf, String s) {

        if (Objects.requireNonNull(conf.getString("Options.Add_Permission_Enabled")).equalsIgnoreCase("true")) {
            if (!sender.hasPermission("Options.Add_Permission")) {
                sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Sorry, but you're missing the permission [" + conf.getString("Options.Add_Permission") + "]"));
                return true;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Please add an ITEM_ID [example: /sellall add COAL_ORE <price>]"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Please add a price or value for the item [example: /sellall add COAL_ORE 100]"));
            return true;
        }
        if (Material.getMaterial(args[1]) == null) {
            sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Sorry but the ITEM_ID's wrong, please check it [/sellall " + args[0] + " Here-> " + args[1] + " <- " + args[2] + "]"));
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Sorry but the value isn't a number [/sellall " + args[0] + " " + args[1] + " Here-> " + args[2] + " <-]"));
            return true;
        }

        conf.set("Items." + args[1] + ".ITEM_ID", args[1]);
        conf.set("Items." + args[1] + ".ITEM_VALUE", value);
        try {
            conf.save(file);
        } catch (IOException e) {
            sender.sendMessage(SpigotPrison.format("&3[PRISON ERROR]&c Sorry, an error occurred while saving the config"));
            e.printStackTrace();
        }

        sender.sendMessage(SpigotPrison.format("&3[PRISON]&a ITEM [" + args[1] + s));

        return true;
    }

    private boolean sellallCommandDelete(CommandSender sender, String[] args, File file, FileConfiguration conf) {
        if (Objects.requireNonNull(conf.getString("Options.Delete_Permission_Enabled")).equalsIgnoreCase("true")){
            if (!sender.hasPermission("Options.Delete_Permission")){
                return true;
            }
        }

        if (args.length < 2){
            sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Please add an ITEM_ID [example: /sellall delete COAL_ORE]"));
            return true;
        }

        if (conf.getConfigurationSection("Items." + args[1]) == null){
            sender.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c " + args[1] + " not found in the config or got already deleted"));
            return true;
        }

        conf.set("Items." + args[1] + ".ITEM_ID", null);
        conf.set("Items." + args[1] + ".ITEM_VALUE", null);
        conf.set("Items." + args[1], null);
        try {
            conf.save(file);
        } catch (IOException e) {
            sender.sendMessage(SpigotPrison.format("&3[PRISON ERROR]&c Sorry, an error occurred while saving the config"));
            e.printStackTrace();
        }

        sender.sendMessage(SpigotPrison.format("&3[PRISON]&a " + args[1] + " Deleted with success!"));
        return true;
    }

    private boolean sellallCommandAdd(CommandSender sender, String[] args, File file, FileConfiguration conf) {
        return sellallCommandEdit(sender, args, file, conf, "] added with success!");
    }

    private boolean sellallCommandSell(CommandSender sender, FileConfiguration conf) {

        if (!(sender instanceof Player)){
            sender.sendMessage(SpigotPrison.format("&3[PRISON ERROR]&c You aren't a player"));
            return true;
        }

        Player p = (Player) sender;

        if (Objects.requireNonNull(conf.getString("Options.Sell_Permission_Enabled")).equalsIgnoreCase("true")){
            if (!p.hasPermission("Options.Sell_Permission")){
                p.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Sorry, but you're missing the permission [" + conf.getString("Options.Sell_Permission") + "]"));
                return true;
            }
        }

        if (!(conf.getConfigurationSection("Items.") == null)){

            // Get the Items config section
            Set<String> items = Objects.requireNonNull(conf.getConfigurationSection("Items")).getKeys(false);

            double moneyToGive = 0;
            for (String key : items) {
                double amount = 0;
                while (p.getInventory().contains(Material.valueOf(conf.getString("Items." + key + ".ITEM_ID")))){
                    p.getInventory().removeItem(new ItemStack(Material.valueOf(conf.getString("Items." + key + ".ITEM_ID")),1));
                    amount++;
                }
                moneyToGive = moneyToGive + (Double.parseDouble(Objects.requireNonNull(conf.getString("Items." + key + ".ITEM_VALUE"))) * amount);
            }

            // Get Spigot Player
            SpigotPlayer sPlayer = new SpigotPlayer(p);

            ModuleManager modMan = Prison.get().getModuleManager();
            Module module = modMan == null ? null : modMan.getModule( PrisonRanks.MODULE_NAME ).orElse( null );

            PrisonRanks rankPlugin = (PrisonRanks) module;

            if (Objects.requireNonNull(conf.getString("Options.Multiplier_Enabled")).equalsIgnoreCase("true")) {

                boolean hasPlayerPrestige = false;
                double multiplier = Double.parseDouble(Objects.requireNonNull(conf.getString("Options.Multiplier_Default")));

                if (rankPlugin != null) {
                    if (rankPlugin.getPlayerManager().getPlayer(sPlayer.getUUID()).isPresent()) {

                        String playerRankName = rankPlugin.getPlayerManager().getPlayer(sPlayer.getUUID()).get().getRank("prestiges").name;

                        if (playerRankName != null) {
                            hasPlayerPrestige = true;
                            sender.sendMessage("Playername: " + playerRankName);
                            multiplier = Double.parseDouble(Objects.requireNonNull(conf.getString("Multiplier." + playerRankName + ".MULTIPLIER")));
                            moneyToGive = moneyToGive * multiplier;
                        }
                    }
                }

                if (!hasPlayerPrestige) {
                    moneyToGive = moneyToGive * multiplier;
                }
            }

            // Get economy
            EconomyIntegration economy = (EconomyIntegration) PrisonAPI.getIntegrationManager().getForType(IntegrationType.ECONOMY).orElseThrow(IllegalStateException::new);
            // Add balance
            economy.addBalance(sPlayer, moneyToGive);
            if (moneyToGive<0.001){
                sender.sendMessage(SpigotPrison.format("&3[PRISON]&c You have nothing to sell!"));
            } else {
                sender.sendMessage(SpigotPrison.format("&3[PRISON]&a You got $" + moneyToGive));
            }
        }

        return true;
    }

    private boolean sellallCommandGUI(CommandSender sender, FileConfiguration conf) {

        if (!(sender instanceof Player)){
            sender.sendMessage(SpigotPrison.format("&c[PRISON ERROR] You aren't a player"));
            return true;
        }

        Player p = (Player) sender;

        if (!Objects.requireNonNull(conf.getString("Options.GUI_Enabled")).equalsIgnoreCase("true")){
            if (p.isOp() || p.hasPermission("prison.admin")) {
                sender.sendMessage(SpigotPrison.format("&c[PRISON ERROR] Sorry but the GUI's disabled in the SellAllConfig.yml"));
                return true;
            }
        }

        if (Objects.requireNonNull(conf.getString("Options.GUI_Permission_Enabled")).equalsIgnoreCase("true")){
            if (!p.hasPermission(Objects.requireNonNull(conf.getString("Options.GUI_Permission")))){
                p.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Sorry, but you're missing the permission [" + conf.getString("Options.GUI_Permission") + "]"));
                return true;
            } else if (Objects.requireNonNull(conf.getString("Options.Player_GUI_Enabled")).equalsIgnoreCase("true")){
                if (Objects.requireNonNull(conf.getString("Options.Player_GUI_Permission_Enabled")).equalsIgnoreCase("true")) {
                    if (!p.hasPermission(Objects.requireNonNull(conf.getString("Options.Player_GUI_Permission")))){
                        p.sendMessage(SpigotPrison.format("&3[PRISON WARN]&c Sorry, but you're missing the permission [" + conf.getString("Options.Player_GUI_Permission") + "]"));
                        return true;
                    }
                }
                SellAllPlayerGUI gui = new SellAllPlayerGUI(p);
                gui.open();
                return true;
            }
        }

        SellAllAdminGUI gui = new SellAllAdminGUI(p);
        gui.open();

        return true;
    }
}