package tech.mcprison.prison.ranks.commands;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.PrisonAPI;
import tech.mcprison.prison.PrisonCommand.RegisteredPluginsData;
import tech.mcprison.prison.chat.FancyMessage;
import tech.mcprison.prison.commands.Arg;
import tech.mcprison.prison.commands.BaseCommands;
import tech.mcprison.prison.commands.Command;
import tech.mcprison.prison.commands.Wildcard;
import tech.mcprison.prison.integration.EconomyCurrencyIntegration;
import tech.mcprison.prison.internal.CommandSender;
import tech.mcprison.prison.internal.Player;
import tech.mcprison.prison.modules.ModuleElement;
import tech.mcprison.prison.modules.ModuleElementType;
import tech.mcprison.prison.output.BulletedListComponent;
import tech.mcprison.prison.output.ChatDisplay;
import tech.mcprison.prison.output.FancyMessageComponent;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.ranks.PrisonRanks;
import tech.mcprison.prison.ranks.data.Rank;
import tech.mcprison.prison.ranks.data.RankLadder;
import tech.mcprison.prison.ranks.data.RankLadder.PositionRank;
import tech.mcprison.prison.ranks.data.RankPlayer;
import tech.mcprison.prison.ranks.data.RankPlayerName;
import tech.mcprison.prison.ranks.managers.PlayerManager;
import tech.mcprison.prison.util.Text;

/**
 * @author Faizaan A. Datoo
 */
public class RanksCommands
			extends BaseCommands {
	
	private CommandCommands rankCommandCommands = null;
	
	public RanksCommands( CommandCommands rankCommandCommands ) {
		super( "RanksCommands" );
		
		this.rankCommandCommands = rankCommandCommands;
	}
	
    public CommandCommands getRankCommandCommands() {
		return rankCommandCommands;
	}
	public void setRankCommandCommands( CommandCommands rankCommandCommands ) {
		this.rankCommandCommands = rankCommandCommands;
	}


	@Command(identifier = "ranks create", description = "Creates a new rank", 
    							onlyPlayers = false, permissions = "ranks.create")
    public boolean createRank(CommandSender sender,
        @Arg(name = "rankName", description = "The name of this rank.") String name,
        @Arg(name = "cost", description = "The cost of this rank.") double cost,
        @Arg(name = "ladder", description = "The ladder to put this rank on.", def = "default")
            String ladder,
        @Wildcard(join=true)
        	@Arg(name = "tag", description = "The tag to use for this rank.", def = "none")
            		String tag) {
		
		boolean success = false;

        // Ensure a rank with the name doesn't already exist
        if (PrisonRanks.getInstance().getRankManager().getRank(name) != null) {
            Output.get()
                .sendWarn(sender, 
                		String.format( "&3The rank named &7%s &3already exists. Try a different name.", name) );
            return success;
        }
        
        // Ensure a rank with the name doesn't already exist
        if (name == null || name.trim().length() == 0 || name.contains( "&" )) {
        	Output.get().sendWarn(sender, "&3A rank name is required and cannot contain formatting codes.");
        	return success;
        }

        // Fetch the ladder first, so we can see if it exists

        Optional<RankLadder> rankLadderOptional =
            PrisonRanks.getInstance().getLadderManager().getLadder(ladder);
        if (!rankLadderOptional.isPresent()) {
            Output.get().sendWarn(sender, "&3A ladder by the name of '&7%s&3' does not exist.", ladder);
            return success;
        }

        // Set a default tag if necessary
        if (tag.equals("none")) {
            tag = "[" + name + "]";
        }

        // Create the rank
        Optional<Rank> newRankOptional =
            PrisonRanks.getInstance().getRankManager().createRank(name, tag, cost);

        // Ensure it was created
        if (!newRankOptional.isPresent()) {
            Output.get().sendError(sender, "&3The rank could not be created.");
            return success;
        }

        Rank newRank = newRankOptional.get();

        // Save the rank
//        try {
            PrisonRanks.getInstance().getRankManager().saveRank(newRank);
//        } catch (IOException e) {
//            Output.get().sendError(sender,
//                "The new rank could not be saved to disk. Check the console for details.");
//            Output.get().logError("Rank could not be written to disk.", e);
//        }

        // Add the ladder

        rankLadderOptional.get().addRank(newRank);
        try {
            PrisonRanks.getInstance().getLadderManager().saveLadder(rankLadderOptional.get());
            
            success = true;
            
            // Tell the player the good news!
            Output.get()
            	.sendInfo(sender, "&3Your new rank, '&7%s&3', was created in the ladder '&7%s&3', " +
            			"using the tag value of '&7%s&3'", name, ladder, tag);
        } catch (IOException e) {
            Output.get().sendError(sender,
                "&3The '&7%s&3' ladder could not be saved to disk. Check the console for details.",
                rankLadderOptional.get().name);
            Output.get().logError("&3Ladder could not be written to disk.", e);
        }

        return success;
    }

	
	@Command(identifier = "ranks autoConfigure", description = "Auto configures Ranks and Mines using " +
			"single letters A through Z for both the rank and mine names. If both ranks and mines are " +
			"generated, they will also be linked together automatically. To set the starting price use " +
			"price=x. To set multiplier mult=x. " +
			"Default values [full price=50000 mult=1.5]", 
			onlyPlayers = false, permissions = "ranks.set")
	public void autoConfigureRanks(CommandSender sender, 
			@Wildcard(join=true)
			@Arg(name = "options", 
				description = "Options: [full ranks mines price=x mult=x]", 
				def = "full") String options
			) {
		
		int rankCount = PrisonRanks.getInstance().getRankManager().getRanks().size();
		int mineCount = Prison.get().getPlatform().getModuleElementCount( ModuleElementType.MINE );
		
		if ( rankCount > 0 || mineCount > 0 ) {
			String message = String.format( "&3Cannot run &7/ranks autoConfigure &3 with any " +
					"ranks or mines already setup. Rank count = &7%d&3. Mine count = &7%d", 
					rankCount, mineCount );
			Output.get().logWarn( message );
			return;
		}
		
		
		String optionHelp = "&b[&7full ranks mines price=&dx &7mult=&dx&b]";
		boolean ranks = false;
		boolean mines = false;
		double startingPrice = 50000;
		double percentMultipler = 1.5;
		
		options = (options == null ? "" : options.replaceAll( "/s*", " " ));
		if ( options.trim().length() == 0 ) {
			Output.get().sendError(sender,
					"&3Invalid options.  Use %s&3.  Was: &3%s",
					optionHelp, options );
			return;
		}

		if ( options.contains( "full" ) ) {
			ranks = true;
			mines = true;
			options = options.replace( "full", "" );
		}
		if ( options.contains( "ranks" ) ) {
			ranks = true;
			options = options.replace( "ranks", "" );
		}
		if ( options.contains( "mines" ) ) {
			mines = true;
			options = options.replace( "mines", "" );
		}
		
		String priceStr = extractParameter("price=", options);
		if ( priceStr != null ) {
			options = options.replace( priceStr, "" );
			priceStr = priceStr.replace( "price=", "" ).trim();
			
			try {
				startingPrice = Double.parseDouble( priceStr );
			}
			catch ( NumberFormatException e ) {
				// Not a valid double number, or price:
			}
		}
		
		
		String multStr = extractParameter("mult=", options);
		if ( multStr != null ) {
			options = options.replace( multStr, "" );
			multStr = multStr.replace( "mult=", "" ).trim();
			
			try {
				percentMultipler = Double.parseDouble( multStr );
			}
			catch ( NumberFormatException e ) {
				// Not a valid double number, or price:
			}
		}
		
		
		// What's left over, if not just a blank string, must be an error:
		options = (options == null ? "" : options.replaceAll( "/s*", " " ));
		if ( options.trim().length() != 0 ) {
			Output.get().sendError(sender,
	                "Invalid options..  Use either %s&3.  Was: [%s]",
	                optionHelp, 
	                options == null ? "null" : options );
			return;
		}
		
		TreeMap<String, RegisteredPluginsData> plugins = 
								Prison.get().getPrisonCommands().getRegisteredPluginData();
		
//
        String permCmdAdd = null;
        String permCmdDel = null;
        String perm1 = "mines.";
        String perm2 = "mines.tp.";
        
        if ( plugins.containsKey("LuckPerms") ){
        	permCmdAdd = "lp user {player} permission set ";
        	permCmdDel = "lp user {player} permission unset ";
        } 
        else if ( plugins.containsKey("PermissionsEx") ){
        	permCmdAdd = "pex user {player} add ";
        	permCmdDel = "pex user {player} add -";
        } 
        else if ( plugins.containsKey("UltraPermissions") ){
        	permCmdAdd = "upc addplayerpermission {player} ";
        	permCmdDel = "upc removeplayerpermission {player} ";
        } 
        else if ( plugins.containsKey("GroupManager") ){
        	permCmdAdd = "manuaddp {player} ";
        	permCmdDel = "manudelp {player} ";
        } 
        else if ( plugins.containsKey("zPermissions") ){
        	permCmdAdd = "permissions player {player} set ";
        	permCmdDel = "permissions player {player} unset ";
        } 
        else if ( plugins.containsKey("PowerfulPerms") ){
        	permCmdAdd = "pp user {player} add ";
        	permCmdAdd = "pp user {player} remove ";
        }


		
		int countRanks = 0;
		int countRankCmds = 0;
		int countMines = 0;
		int countLinked = 0;
		
		if ( ranks ) {
			
	        int colorID = 1;
	        double price = 0;

	        
	        for ( char cRank = 'A'; cRank <= 'Z'; cRank++) {
	        	String rankName = Character.toString( cRank );
	        	String tag = "&7[&" + Integer.toHexString((colorID++ % 15) + 1) + rankName + "&7]";
	        	
	        	char cRankNext = (char) (cRank + 1);
	        	String rankNameNext = Character.toString( cRankNext );
	        	
	        	if ( createRank(sender, rankName, price, "default", tag) ) {
	        		countRanks++;
	        		
	        		if ( permCmdAdd != null ) {
	        			getRankCommandCommands().commandAdd( sender, rankName, permCmdAdd + perm1 + rankName.toLowerCase());
	        			countRankCmds++;
	        			getRankCommandCommands().commandAdd( sender, rankName, permCmdAdd + perm2 + rankName.toLowerCase());
	        			countRankCmds++;
	        			
	        			if ( cRankNext <= 'Z' ) {
	        				getRankCommandCommands().commandAdd( sender, rankName, permCmdDel + perm1 + rankNameNext.toLowerCase());
	        				countRankCmds++;
	        				getRankCommandCommands().commandAdd( sender, rankName, permCmdDel + perm2 + rankNameNext.toLowerCase());
	        				countRankCmds++;
	        			}
	        			
	        		}
	        		
	        		if ( mines ) {

	        			// Creates a virtual mine:
	        			ModuleElement mine = Prison.get().getPlatform().createModuleElement( 
	        					sender, ModuleElementType.MINE, rankName, tag );
	        			
	        			if ( mine != null ) {
	        				countMines++;
	        				
	        				// Links the virtual mine to generated rank:
	        				if ( Prison.get().getPlatform().linkModuleElements( mine, ModuleElementType.RANK, rankName ) ) {
	        					countLinked++;
	        				}
	        			}
	        		}
	        	}

	            if (price == 0){
	                price += startingPrice;
	            } else {
	                price *= percentMultipler;
	            }

	        }
		}
		
		// If mines were created, go ahead and auto assign blocks to the mines:
		if ( countMines > 0 ) {
			Prison.get().getPlatform().autoCreateMineBlockAssignment();
		}
		
		if ( countRanks == 0 ) {
			Output.get().logInfo( "Ranks autoConfigure: No ranks were created.");
		}
		else {
			Output.get().logInfo( "Ranks autoConfigure: %d ranks were created.", countRanks);
			
			if ( countRankCmds == 0 ) {
				Output.get().logInfo( "Ranks autoConfigure: No rank commandss were created.");
			}
			else {
				Output.get().logInfo( "Ranks autoConfigure: %d rank commands were created.", countRanks);
				Output.get().logInfo( "Ranks autoConfigure: The permission %s<rankName> and " +
						"%s<rankName> was " +
						"created for each rank. Make sure you add every permission to your " +
						"permission plugin or they may not work. ",
						 perm1, perm2 );
			}
		}
		
		if ( countMines == 0 ) {
			Output.get().logInfo( "Ranks autoConfigure: No mines were created.");
		}
		else {
			Output.get().logInfo( "Ranks autoConfigure: %d mines were created.", countMines);
			
			if ( countLinked == 0 ) {
				Output.get().logInfo( "Ranks autoConfigure: No mines and no ranks were linked.");
			}
			else {
				Output.get().logInfo( "Ranks autoConfigure: %d ranks and mines were linked.", countLinked);
			}
		}
		
		Output.get().logInfo( "");
		
		
	}
	
	private String extractParameter( String key, String options ) {
		String results = null;
		int idx = options.indexOf( key );
		if ( idx != -1 ) {
			int idxEnd = options.indexOf( " ", idx );
			if ( idxEnd == -1 ) {
				idxEnd = options.length();
			}
			results = options.substring( idx, idxEnd );
		}
		return results;
	}
	
	
    @Command(identifier = "ranks delete", description = "Removes a rank, and deletes its files.", 
    								onlyPlayers = false, permissions = "ranks.delete")
    public void removeRank(CommandSender sender, @Arg(name = "rankName") String rankName) {
        // Check to ensure the rank exists
        Rank rank = PrisonRanks.getInstance().getRankManager().getRank(rankName);
        if ( rank == null ) {
            Output.get().sendError(sender, "The rank '%s' does not exist.", rankName);
            return;
        }

        if (PrisonRanks.getInstance().getDefaultLadder().containsRank(rank.id)
            && PrisonRanks.getInstance().getDefaultLadder().ranks.size() == 1) {
            Output.get().sendError(sender,
                "You can't remove this rank because it's the only rank in the default ladder.");
            return;
        }

        if ( PrisonRanks.getInstance().getRankManager().removeRank(rank) ) {
        	
            Output.get().sendInfo(sender, "The rank '%s' has been removed successfully.", rankName);
        } else {
            Output.get()
                .sendError(sender, "The rank '%s' could not be deleted due to an error.", rankName);
        }
    }

    @Command(identifier = "ranks list", description = "Lists all the ranks on the server.", 
    							onlyPlayers = false, altPermissions = "ranks.list"
    							)
    public void listRanks(CommandSender sender,
        @Arg(name = "ladderName", def = "default") String ladderName) {

    	boolean hasPerm = sender.hasPermission("ranks.list") ||
    					sender.isOp();
    	
        Optional<RankLadder> ladderOpt =
        			PrisonRanks.getInstance().getLadderManager().getLadder(ladderName);

        if (!ladderOpt.isPresent()) {
            Output.get().sendError(sender, "The ladder '%s' doesn't exist.", ladderName);
            return;
        }

        RankLadder ladder = ladderOpt.get();
        Rank rank = null;
        for (PositionRank pRank : ladder.ranks) {
            Optional<Rank> rankOptional = ladder.getByPosition(pRank.getPosition());
            if (rankOptional.isPresent()) {
            	rank = rankOptional.get();
            	break;
            }
        }
        

        String rankHeader = "Ranks" + 
        				( hasPerm || !hasPerm && !ladderName.equalsIgnoreCase( "default" ) ?
        						" in " + ladderName : "");
        ChatDisplay display = new ChatDisplay( rankHeader );
        
        if ( hasPerm ) {
        	display.text("&7Click on a rank's name to view more info.");
        }
        

        BulletedListComponent.BulletedListBuilder builder =
            new BulletedListComponent.BulletedListBuilder();
        
        boolean first = true;
        while ( rank != null ) {
        	
            boolean defaultRank = ("default".equalsIgnoreCase( ladderName ) && first);
            
            String textRankName = ( hasPerm ?
            							String.format( "&3%s " , rank.name )
            							: "");
            String textCmdCount = ( hasPerm ? 
            							String.format( " &7- Commands: &3%d", rank.rankUpCommands.size())
            							: "" );
            
            String text =
                String.format("%s &9[&3%s&9] &7- %s&7%s%s%s", 
                			textRankName, rank.tag, 
                			(defaultRank ? "&b(&9Default&b) &7- " : ""),
                			Text.numberToDollars(rank.cost),
                			(rank.currency == null ? "" : " &7Currency: &3" + rank.currency),
                			textCmdCount );
            
            String rankName = rank.name;
            if ( rankName.contains( "&" ) ) {
            	rankName = rankName.replace( "&", "-" );
            }
            FancyMessage msg = null;
            if ( hasPerm ) {
            	msg = new FancyMessage(text).command("/ranks info " + rankName)
            			.tooltip("&7Click to view info.");
            }
            else {
            	msg = new FancyMessage(text);
            }
            
            builder.add(msg);
        	
        	rank = rank.rankNext;
        	first = false;
        }
        
//        for (RankLadder.PositionRank pos : ranks) {
//            Optional<Rank> rankOptional = ladder.get().getByPosition(pos.getPosition());
//            if (!rankOptional.isPresent()) {
//                continue; // Skip it
//            }
//            Rank rank = rankOptional.get();
//
//            boolean defaultRank = ("default".equalsIgnoreCase( ladderName ) && first);
//            
//            String text =
//                String.format("&3%s &9[&3%s&9] &7- %s&7%s &7- Commands: &3%d", 
//                			rank.name, rank.tag, 
//                			(defaultRank ? "&b(&9Default&b) &7-" : ""),
//                			Text.numberToDollars(rank.cost),
//                			rank.rankUpCommands.size());
//            FancyMessage msg = new FancyMessage(text).command("/ranks info " + rank.name)
//                .tooltip("&7Click to view info.");
//            builder.add(msg);
//            first = false;
//        }

        display.addComponent(builder.build());
        
        if ( hasPerm ) {
        	display.addComponent(new FancyMessageComponent(
        			new FancyMessage("&7[&a+&7] Add").suggest("/ranks create ")
        			.tooltip("&7Create a new rank.")));
        	
        	List<String> others = new ArrayList<>();
        	for (RankLadder other : PrisonRanks.getInstance().getLadderManager().getLadders()) {
        		if (!other.name.equals(ladderName) && (other.name.equals("default") || sender
        				.hasPermission("ranks.rankup." + other.name.toLowerCase()))) {
        			if (sender.hasPermission("ranks.admin")) {
        				others.add("/ranks list " + other.name);
        			} else {
        				others.add("/ranks " + other.name);
        			}
        		}
        	}
        	
        	if (others.size() != 0) {
        		FancyMessage msg = new FancyMessage("&8You may also try ");
        		int i = 0;
        		for (String other : others) {
        			i++;
        			if (i == others.size() && others.size() > 1) {
        				msg.then(" &8and ");
        			}
        			msg.then("&7" + other).tooltip("&7Click to view.").command(other);
        			msg.then(i == others.size() ? "&8." : "&8,");
        		}
        		display.addComponent(new FancyMessageComponent(msg));
        	}
        	
        }

        display.send(sender);

    }

    @Command(identifier = "ranks info", description = "Information about a rank.", 
    							onlyPlayers = false, permissions = "ranks.info", 
    							altPermissions = "ranks.admin" )
    public void infoCmd(CommandSender sender, @Arg(name = "rankName") String rankName) {
    	
        Rank rank = PrisonRanks.getInstance().getRankManager().getRank(rankName);
        if ( rank == null ) {
//        	rankOpt = PrisonRanks.getInstance().getRankManager().getRankEscaped(rankName);
//        	if (!rankOpt.isPresent()) {
        		Output.get().sendError(sender, "The rank '%s' doesn't exist.", rankName);
        		return;
//        	}
        }

        List<RankLadder> ladders =
            PrisonRanks.getInstance().getLadderManager().getLaddersWithRank(rank.id);

        ChatDisplay display = new ChatDisplay("Rank " + rank.tag);

        display.text("&3Rank Name: &7%s", rank.name);
        display.text("&3Rank Tag:  &7%s", rank.tag);
        
        // (I know this is confusing) Ex. Ladder(s): default, test, and test2.
        display.text("&3%s: &7%s", Text.pluralize("Ladder", ladders.size()),
            Text.implodeCommaAndDot(
                ladders.stream().map(rankLadder -> rankLadder.name).collect(Collectors.toList())));
        
        if ( rank.getMines().size() == 0 ) {
        	display.text( "&3This rank is not linked to any mines" );
        }
        else {
        	StringBuilder sb = new StringBuilder();
        	
        	for ( ModuleElement mine : rank.getMines() ) {
				if ( sb.length() > 0 ) {
					sb.append( "&3, " );
				}
				sb.append( "&7" );
				sb.append( mine.getName() );
			}
        	
        	display.text( "&3Mines linked to this rank: %s", sb.toString() );
        }

        display.text("&3Cost: &7%s", Text.numberToDollars(rank.cost));
        
        display.text("&3Currency: &7<&a%s&7>", (rank.currency == null ? "&cdefault" : rank.currency) );
        
        List<RankPlayer> players =
        		PrisonRanks.getInstance().getPlayerManager().getPlayers().stream()
        		.filter(rankPlayer -> rankPlayer.getRanks().values().contains(rank))
        		.collect(Collectors.toList());
        display.text("&7There %s &3%s players &7with this rank.", 
        				(players.size() == 1 ? "is": "are"), 
        				players.size() + "");

        if (sender.hasPermission("ranks.admin")) {
            // This is admin-exclusive content

            display.text("&8[Admin Only]");
            display.text("&6Rank ID: &7%s", rank.id);

            FancyMessage del =
                new FancyMessage("&7[&c-&7] Delete").command("/ranks delete " + rank.name)
                    .tooltip("&7Click to delete this rank.\n&cYou may not reverse this action.");
            display.addComponent(new FancyMessageComponent(del));
        }

        display.send(sender);
    }

    // set commands
    @Command(identifier = "ranks set cost", description = "Modifies a ranks cost", 
    							onlyPlayers = false, permissions = "ranks.set")
    public void setCost(CommandSender sender, 
    		@Arg(name = "rankName") String rankName, 
    		@Arg(name = "cost", description = "The cost of this rank.") double cost){
        Rank rank = PrisonRanks.getInstance().getRankManager().getRank(rankName);
        if ( rank == null ) {
            Output.get().sendError(sender, "The rank '%s' doesn't exist.", rankName);
            return;
        }
        
        rank.cost = cost;
        
        // Save the rank
//        try {
            PrisonRanks.getInstance().getRankManager().saveRank(rank);

            Output.get().sendInfo(sender,"Successfully set the cost of rank '%s' to "+cost,rankName);
//        } catch (IOException e) {
//            Output.get().sendError(sender,
//                "The rank could not be saved to disk. The change in rank cost has not been saved. Check the console for details.");
//            Output.get().logError("Rank could not be written to disk (setCost).", e);
//        }
    }
    
    // set commands
    @Command(identifier = "ranks set currency", description = "Modifies a ranks currency", 
    													onlyPlayers = false, permissions = "ranks.set")
    public void setCurrency(CommandSender sender, 
    		@Arg(name = "rankName") String rankName, 
    		@Arg(name = "currency", description = "The currency to use with this rank.") String currency){
    	
    	Rank rank = PrisonRanks.getInstance().getRankManager().getRank(rankName);
    	if ( rank == null ) {
    		Output.get().sendError(sender, "The rank '%s' doesn't exist.", rankName);
    		return;
    	}
    	
    	
    	if ( currency == null || currency.trim().length() == 0 ) {
    		Output.get().sendError(sender, "A currency name must be specified. '%s' is invalid.", currency);
    		return;
    	}
    	
    	
    	EconomyCurrencyIntegration currencyEcon = PrisonAPI.getIntegrationManager()
				.getEconomyForCurrency( currency );
    	if ( currencyEcon == null ) {
    		Output.get().sendError(sender, "No active economy supports the currency named '%s'.", currency);
    		return;
    	}
    	
    	
    	rank.currency = currency;
    	
    	// Save the rank
//    	try {
    		PrisonRanks.getInstance().getRankManager().saveRank(rank);
    		
    		Output.get().sendInfo(sender,"Successfully set the currency for the rank '%s' to %s", rankName, currency);
//    	} catch (IOException e) {
//    		Output.get().sendError(sender,
//    				"The rank could not be saved to disk. The change in rank currency has not been saved. Check the console for details.");
//    		Output.get().logError("Rank could not be written to disk (setCurrency).", e);
//    	}
    }

    @Command(identifier = "ranks set tag", description = "Modifies a ranks tag", 
    							onlyPlayers = false, permissions = "ranks.set")
    public void setTag(CommandSender sender, 
    				@Arg(name = "rankName") String rankName, 
    				@Wildcard(join=true)
    					@Arg(name = "tag", description = "Tag value for the Rank. Use [null] to remove.") String tag){
    	
        Rank rank = PrisonRanks.getInstance().getRankManager().getRank(rankName);
        if ( rank == null ) {
            Output.get().sendError(sender, "The rank '%s' doesn't exist.", rankName);
            return;
        }
        

        if ( tag == null || tag.trim().length() == 0 ) {
        	sender.sendMessage( "&cTag name must be a valid value. To remove use a value of &anull&c." );
        	return;
        }
        
        if ( tag.equalsIgnoreCase( "null" ) ) {
        	tag = null;
        }


        if ( tag == null && rank.getTag() == null || 
        		rank.getTag() != null &&
        		rank.getTag().equalsIgnoreCase( tag )) {
        
        	sender.sendMessage( "&cThe new tag name is the same as what it was. No change was made." );
        	return;
        }

        rank.tag = tag;
        
        PrisonRanks.getInstance().getRankManager().saveRank(rank);

        if ( tag == null ) {
        	sender.sendMessage( 
        			String.format( "&cThe tag name was cleared for the rank %s.", 
        					rank.getName() ) );
        }
        else {
        	sender.sendMessage( 
        			String.format( "&cThe tag name was changed to %s for the rank %s.", 
        					tag, rank.getName() ) );
        }
    }
    
    @Command(identifier = "ranks player", description = "Shows a player their rank", onlyPlayers = false)
    public void rankPlayer(CommandSender sender,
    			@Arg(name = "player", def = "", description = "Player name") String playerName){
    	
    	Player player = getPlayer( sender, playerName );
    	
    	if (player == null) {
    		sender.sendMessage( "&3You must be a player in the game to run this command, and/or the player must be online." );
    		return;
    	}

    	PlayerManager pm = PrisonRanks.getInstance().getPlayerManager();
		Optional<RankPlayer> oPlayer = pm.getPlayer(player.getUUID(), player.getName());
		
		if ( oPlayer.isPresent() ) {
			DecimalFormat dFmt = new DecimalFormat("#,##0.00");
			
			RankPlayer rankPlayer = oPlayer.get();
			Map<RankLadder, Rank> rankLadders = rankPlayer.getRanks();
			
			for ( RankLadder rankLadder : rankLadders.keySet() )
			{
				Rank rank = rankLadders.get( rankLadder );
				Rank nextRank = rank.rankNext;
				
				String messageRank = String.format("&c%s&7: Ladder: &b%s  &7Current Rank: &b%s", 
						player.getDisplayName(), 
						rankLadder.name,
						rank.name );
				
				if ( nextRank == null ) {
					messageRank += "  It's the highest rank!";
				} else {
					messageRank += String.format("  &7Next rank: &b%s&7 &c$&b%s", 
							nextRank.name, 
							dFmt.format( nextRank.cost ));

					if ( nextRank.currency != null ) {
						messageRank += String.format("  &7Currency: &b%s", 
								nextRank.currency);
					}
				}
				
				sendToPlayerAndConsole( sender, messageRank );
			}
			
			// Print out the player's balance:
			double balance = getPlayerBalance( player );
			String message = String.format( "&7The current balance for &b%s &7is &b%s", 
					player.getName(), dFmt.format( balance ) );
			sendToPlayerAndConsole( sender, message );
			
			
			if (sender.hasPermission("ranks.admin") && rankPlayer.names.size() > 1) {
	            // This is admin-exclusive content

				sendToPlayerAndConsole( sender, "&8[Admin Only]" );
				sendToPlayerAndConsole( sender, "  &7Past Player Names and Date Changed:" );
				
				for ( RankPlayerName rpn : rankPlayer.names ) {
					
					sendToPlayerAndConsole( sender, "    &b" + rpn.toString() );
				}


	        }
			
//			String nextRank = pm.getPlayerNextRankName( rankPlayer );
//			String nextRankCost = pm.getPlayerNextRankCost( rankPlayer );
//			
//			String message = String.format("&c%s&7:  Current Rank: &b%s&7", 
//					player.getDisplayName(), pm.getPlayerRankName( rankPlayer ));
//			
//			if ( nextRank.trim().length() == 0 ) {
//				message += "  It's the highest rank!";
//			} else {
//				message += String.format("  Next rank: &b%s&7 &c$&b%s &7s%", 
//						nextRank, nextRankCost, currency );
//			}
//			sender.sendMessage( message );
			
		} else {
			sender.sendMessage( "&3No ranks found for &c" + player.getDisplayName() );
		}
    }

	private void sendToPlayerAndConsole( CommandSender sender, String messageRank )
	{
		// If not a console user then send the message to the sender, other wise if a console
		// user then they will see duplicate messages:
		if ( sender.getName() != null && !sender.getName().equalsIgnoreCase( "console" ) ) {
			sender.sendMessage( messageRank );
		}
		
		// log the rank. There was one issue with the ranks suddenly being changed so this
		// will help document what ranks were.
		Output.get().logInfo( messageRank );
	}
 
    
    @Command(identifier = "ranks players", description = "Shows all ranks with player counts", onlyPlayers = false)
    public void rankPlayers(CommandSender sender,
    			@Arg(name = "ladderName", def = "all", description = "Ladder Name [all, none, LadderName]") String ladderName,
    			@Arg(name = "action", def = "players", description = "List type [players, all]") String action){

    	
    	if ( !ladderName.equalsIgnoreCase( "all" ) && 
    			!PrisonRanks.getInstance().getLadderManager().getLadder( ladderName ).isPresent() ) {
    		Output.get().sendError(sender, "The ladder '%s' doesn't exist, or was not ALL.", ladderName);
    		return;
    	}
    	
    	
    	if ( !action.equalsIgnoreCase( "players" ) && !action.equalsIgnoreCase( "all" ) ) {
    		Output.get().sendError(sender, "The action '%s' is invalid. [players, all]", action);
    		
    		return;
    	}
    	
    	boolean includeAll = action.equalsIgnoreCase( "all" );
    	PrisonRanks.getInstance().getRankManager().ranksByLadders( sender, ladderName, includeAll );
    	
//    	Output.get().logInfo( "Ranks by ladders:" );
//    	
//    	for ( RankLadder ladder : PrisonRanks.getInstance().getLadderManager().getLadders() ) {
//    		if ( ladderName.equalsIgnoreCase( "all" ) || ladderName.equalsIgnoreCase( ladder.name ) ) {
//    			
//    			boolean includeAll = action.equalsIgnoreCase( "all" );
//    			String ladderRanks = ladder.listAllRanks( includeAll );
//    			
//    			sender.sendMessage( ladderRanks );
//    		}
//			
//		}
    	
    }
    

    
//    /**
//     * This function is just an arbitrary test to access the various components.
//     * 
//     * @param sender
//     * @param playerName
//     */
//    @Command( identifier = "ranks test", onlyPlayers = false, permissions = "prison.admin" )
//    public void prisonModuleTest(CommandSender sender,
//			@Arg(name = "player", def = "", description = "Player name") String playerName){
//    	
// 		ModuleManager modMan = Prison.get().getModuleManager();
// 	    Module module = modMan == null ? null : modMan.getModule( PrisonRanks.MODULE_NAME ).orElse( null );
//
// 	    int moduleCount = (modMan == null ? 0 : modMan.getModules().size());
// 	    sender.sendMessage(String.format( "prisonModuleTest: prison=%s moduleManager=%s " +
// 	    		"registeredModules=%s PrisonRanks=%s", 
// 	    		(Prison.get() == null ? "null" : "active"),
// 	    		(Prison.get().getModuleManager() == null ? "null" : "active"),
// 	    		Integer.toString( moduleCount ),
// 	    		(modMan.getModule( PrisonRanks.MODULE_NAME ) == null ? "null" : "active")
// 	    		) );
//
// 	    if ( module == null || !(module instanceof PrisonRanks) ) {
// 	    	
// 	    	sender.sendMessage( "prisonModuleTest: Cannot get PrisonRanks. Terminating" );
// 	    	return;
// 	    }
// 	    
//
// 	    PrisonRanks rankPlugin = (PrisonRanks) module;
//
// 	    if ( rankPlugin == null || rankPlugin.getPlayerManager() == null ) {
// 	    	sender.sendMessage( "prisonModuleTest: PrisonRanks could not be created. Terminating" );
// 	    	return;
// 	    }
//
// 	    
// 	    PlayerManager playerManager = rankPlugin.getPlayerManager();
//    	Player player = getPlayer( sender, playerName );
//
//    	sender.sendMessage( String.format( "prisonModuleTest: PlayerManager=%s player=%s sender=%s playerName=%s",
//    				(playerManager == null ? "null" : "active"), (player == null ? "null" : player.getName()),
//    				(sender == null ? "null" : sender.getName()), (playerName == null ? "null" : playerName)
//    			));
//
//    	
//    	if ( player == null ) {
//    		sender.sendMessage( "prisonModuleTest: Cannot get a valid player. " +
//    				"If console, must supply a valid name. Terminating" );
//    		return;
//    	}
// 
//    	RankPlayer rPlayer = playerManager.getPlayer( player.getUUID() ).orElse( null );
//        LadderManager lm = rankPlugin.getLadderManager();
//        
//        for ( RankLadder ladderData : lm.getLadders() ) {
//        	Rank playerRank = rPlayer == null ? null : rPlayer.getRank( ladderData ).orElse( null );
//        	Rank rank = ladderData.getLowestRank().orElse( null );
//        	
//        	while ( rank != null ) {
//        		boolean playerHasThisRank = playerRank != null && playerRank.equals( rank );
//        		
//        		sender.sendMessage(String.format( "prisonModuleTest: ladder=%s rank=%s playerRank=%s hasRank=%s", 
//        				ladderData.name, rank.name, (playerRank == null ? "null" : playerRank.name ),
//        				Boolean.valueOf( playerHasThisRank ).toString()
//        				));
//        		
//        		rank = rank.rankNext;
//        	}
//        }
//    }
    

}
