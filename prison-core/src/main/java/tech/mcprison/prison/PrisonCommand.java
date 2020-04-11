/*
 *  Prison is a Minecraft plugin for the prison game mode.
 *  Copyright (C) 2017 The Prison Team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.mcprison.prison;

import java.util.ArrayList;
import java.util.List;

import tech.mcprison.prison.commands.Arg;
import tech.mcprison.prison.commands.Command;
import tech.mcprison.prison.integration.IntegrationManager;
import tech.mcprison.prison.integration.IntegrationType;
import tech.mcprison.prison.internal.CommandSender;
import tech.mcprison.prison.modules.Module;
import tech.mcprison.prison.modules.ModuleStatus;
import tech.mcprison.prison.output.BulletedListComponent;
import tech.mcprison.prison.output.ChatDisplay;
import tech.mcprison.prison.output.DisplayComponent;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.troubleshoot.TroubleshootResult;
import tech.mcprison.prison.troubleshoot.Troubleshooter;
import tech.mcprison.prison.util.Text;

/**
 * Root commands for managing the platform as a whole, in-game.
 *
 * @author Faizaan A. Datoo
 * @since API 1.0
 */
public class PrisonCommand {

	private List<String> registeredPlugins = new ArrayList<>();
	
    @Command(identifier = "prison version", description = "Displays version information.", onlyPlayers = false)
    public void versionCommand(CommandSender sender) {
    	ChatDisplay display = displayVersion();
    	
        display.send(sender);
    }
    
    public ChatDisplay displayVersion() {
    	
        ChatDisplay display = new ChatDisplay("/prison version");
        display.text("&7Prison Version: &3%s", Prison.get().getPlatform().getPluginVersion());

        display.text("&7Running on Platform: &3%s", Prison.get().getPlatform().getClass().getName());
        display.text("&7Minecraft Version: &3%s", Prison.get().getMinecraftVersion());

        display.text("");
        
        display.text("&7Commands: &2/prison");
        
        for ( Module module : Prison.get().getModuleManager().getModules() ) {
        	
        	display.text( "&7Module: &3%s&3 : %s  %s", module.getName(), 
        			(module.getStatus().getStatus() == ModuleStatus.Status.ENABLED ? 
        					String.format( "&2Enabled  &7Commands: &2%s", module.getBaseCommands()) : 
        				(module.getStatus().getStatus() == ModuleStatus.Status.FAILED ? "&cFailed" :
        						"&9&m-Disabled-" )),
        			(module.getStatus().getStatus() == ModuleStatus.Status.FAILED ? 
        						"&d[" + module.getStatus().getMessage() + "&d]" : "")
        			);
        }
         
        
        display.text("");
        display.text("&7Integrations:");

        IntegrationManager im = Prison.get().getIntegrationManager();
        String permissions =
        		(im.hasForType(IntegrationType.PERMISSION) ?
                "&a" + im.getForType(IntegrationType.PERMISSION).get().getDisplayName() :
                "&cNone");

        display.text(Text.tab("&7Permissions: " + permissions));

        String economy =
        		(im.hasForType(IntegrationType.ECONOMY) ?
                "&a" + im.getForType(IntegrationType.ECONOMY).get().getDisplayName() : 
                "&cNone");

        display.text(Text.tab("&7Economy: " + economy));
        
        
        List<DisplayComponent> integrationRows = Prison.get().getIntegrationManager().getIntegrationComponents();
        for ( DisplayComponent component : integrationRows )
		{
        	display.addComponent( component );
		}
        
        
        // Display all loaded plugins:
        if ( getRegisteredPlugins().size() > 0 ) {
        	display.text( "&7Registered Plugins: " );
        	StringBuilder sb = new StringBuilder();
        	for ( String plugin : getRegisteredPlugins() ) {
        		if ( sb.length() == 0) {
        			sb.append( "  " );
        			sb.append( plugin );
        		} else {
        			sb.append( ",  " );
        			sb.append( plugin );
        			display.text( sb.toString() );
        			sb.setLength( 0 );
        		}
        	}
        	if ( sb.length() > 0 ) {
        		display.text( sb.toString());
        	}
        }
        
        
        return display;
    }

    @Command(identifier = "prison modules", description = "Lists the modules that hook into Prison to give it functionality.", onlyPlayers = false, permissions = "prison.modules")
    public void modulesCommand(CommandSender sender) {
        ChatDisplay display = new ChatDisplay("/prison modules");
        display.emptyLine();

        BulletedListComponent.BulletedListBuilder builder =
            new BulletedListComponent.BulletedListBuilder();
        for (Module module : Prison.get().getModuleManager().getModules()) {
            builder.add("&3%s &8(%s) &3v%s &8- %s", module.getName(), module.getPackageName(),
                module.getVersion(), module.getStatus().getMessage());
        }

        display.addComponent(builder.build());

        display.send(sender);
    }

    @Command(identifier = "prison troubleshoot", description = "Runs a troubleshooter.", onlyPlayers = false, permissions = "prison.troubleshoot")
    public void troubleshootCommand(CommandSender sender,
        @Arg(name = "name", def = "list", description = "The name of the troubleshooter.") String name) {
        // They just want to list stuff
        if (name.equals("list")) {
            sender.dispatchCommand("prison troubleshoot list");
            return;
        }

        TroubleshootResult result =
            PrisonAPI.getTroubleshootManager().invokeTroubleshooter(name, sender);
        if (result == null) {
            Output.get().sendError(sender, "The troubleshooter %s doesn't exist.", name);
            return;
        }

        ChatDisplay display = new ChatDisplay("Result Summary");
        display.text("&7Troubleshooter name: &b%s", name.toLowerCase()) //
            .text("&7Result type: &b%s", result.getResult().name()) //
            .text("&7Result details: &b%s", result.getDescription()) //
            .send(sender);

    }

    @Command(identifier = "prison troubleshoot list", description = "Lists the troubleshooters.", onlyPlayers = false, permissions = "prison.troubleshoot")
    public void troubleshootListCommand(CommandSender sender) {
        ChatDisplay display = new ChatDisplay("Troubleshooters");
        display.text("&8Type /prison troubleshoot <name> to run a troubleshooter.");

        BulletedListComponent.BulletedListBuilder builder =
            new BulletedListComponent.BulletedListBuilder();
        for (Troubleshooter troubleshooter : PrisonAPI.getTroubleshootManager()
            .getTroubleshooters()) {
            builder.add("&b%s &8- &7%s", troubleshooter.getName(), troubleshooter.getDescription());
        }
        display.addComponent(builder.build());

        display.send(sender);
    }

    @Command(identifier = "prison convert", description = "Convert your Prison 2 data to Prison 3 data.", onlyPlayers = false, permissions = "prison.convert")
    public void convertCommand(CommandSender sender) {
        sender.sendMessage(Prison.get().getPlatform().runConverter());
    }

	public List<String> getRegisteredPlugins() {
		return registeredPlugins;
	}

}
