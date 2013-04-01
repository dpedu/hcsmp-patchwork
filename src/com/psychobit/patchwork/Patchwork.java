package com.psychobit.patchwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Various patches to improve functionality and patch security holes
 * @author Psychobit
 *
 */
public class Patchwork extends JavaPlugin implements Listener
{
	
	/**
	 * List of death chests
	 */
	private ArrayList<Chest> _deathchests;
	
	/**
	 * Plugin initialization 
	 */
	public void onEnable()
	{
		// Register event listeners
		this.getServer().getPluginManager().registerEvents( this, this );
		
		// Default config
		FileConfiguration config = this.getConfig();
		if ( !config.contains("anticreative" ) ) config.set( "anticreative", false );
		if ( !config.contains("antidupe" ) ) config.set( "antidupe", false );
		if ( !config.contains("antinetherroof" ) ) config.set( "antinetherroof", false );
		if ( !config.contains("deathchests" ) ) config.set( "deathchests", false );
		if ( !config.contains("disenchant" ) ) config.set( "disenchant", false );
		if ( !config.contains("uberenchant.enable" ) ) config.set( "uberenchant.enable", false );
		if ( !config.contains("uberenchant.level" ) ) config.set( "uberenchant.level", 5 );
		if ( !config.contains("uberenchant.replace" ) ) config.set( "uberenchant.replace", 20 );
		this.saveConfig();
		
		// Deathchests
		this._deathchests = new ArrayList<Chest>();
	}
	
	/**
	 * Plugin disabled
	 */
	public void onDisable()
	{
		// Pop deathchests
		Iterator<Chest> chests = this._deathchests.iterator();
		while( chests.hasNext() ) chests.next().getBlock().setType( Material.AIR );
	}
	
	/**
	 * Anti-Creative
	 * Set non-ops to survival when they join
	 * @param e
	 */
	@EventHandler( priority = EventPriority.LOW )
	public void onPlayerJoin( PlayerJoinEvent e )
	{
		if ( !this.getConfig().getBoolean( "anticreative", false ) ) return;
		if ( e.getPlayer().isOp() ) return;
		e.getPlayer().setGameMode( GameMode.SURVIVAL );
	}
	
	/**
	 * Anti-Creative
	 * Prevent non-ops from having their gamemode changed
	 * @param e
	 */
	@EventHandler( priority = EventPriority.LOW )
	public void onGamemodeChange( PlayerGameModeChangeEvent e )
	{
		if ( !this.getConfig().getBoolean( "anticreative", false ) ) return;
		if ( e.getPlayer().isOp() ) return;
		e.setCancelled( true );
	}
	
	
	/**
	 * Anti-Duplication
	 * Prevents item duplication using chest breaking.
	 * Clears container inventory and drop the items.
	 * @param e
	 */
	@EventHandler( priority = EventPriority.NORMAL, ignoreCancelled=true )
	public void onBlockBreak( BlockBreakEvent e )
	{
		if ( !this.getConfig().getBoolean( "antidupe", false ) ) return;
		Block b = e.getBlock();
		if ( ! ( b.getState() instanceof InventoryHolder ) ) return;
		Inventory inv = ( ( InventoryHolder ) b.getState() ).getInventory();
		ItemStack[] items = inv.getContents(); 
		if ( items.length == 0 ) return;
		inv.clear();
		for ( ItemStack t : items )
		{
			if ( t != null ) b.getWorld().dropItemNaturally( b.getLocation(), t );
		}
	}
	
	
	/**
	 * Anti-Netherroof
	 * Move player to the nearest open location if they try to spawn on top of the nether 
	 * @param e
	 */
	@EventHandler( priority=EventPriority.NORMAL, ignoreCancelled=true )
	public void onPlayerSpawn( PlayerJoinEvent e )
	{
		if ( !this.getConfig().getBoolean( "antinetherroof", false ) ) return;
		Player player = e.getPlayer();
		if ( player.getWorld().getEnvironment().compareTo( Environment.NETHER ) != 0 ) return;
		if ( player.getLocation().getBlockY() <= 127 ) return;
		
		player.sendMessage( "You tried to spawn on the nether-roof, which is not allowed!" );
		
		World world = player.getWorld();
		int x = player.getLocation().getBlockX();
		int z = player.getLocation().getBlockZ();
		int lowerBody = -1;
		int upperBody = -1;
		for( int i = 0; i < 100; i++ )
		{
			for( int j = 0; j < 100; j++ )
			{
				for( int y = 120; y > 1; y-- )
				{
					int floor = world.getBlockTypeIdAt( ( x - i ), y, ( z - j ) );
					
					if ( lowerBody == 0 && upperBody == 0 && floor != 0 && floor != 10 && floor != 11 )
					{
						player.teleport( new Location( world, ( x - i ), ( y + 2 ), ( z - j ) ) );
						return;
					}
					upperBody = lowerBody;
					lowerBody = floor;
				}
				lowerBody = -1;
				upperBody = -1;
			}
			lowerBody = -1;
			upperBody = -1;
		}
		
		// Fail case
		player.teleport( new Location( world, ( 0 ), ( 127 ), ( 0 ) ) );
	}
	
	
	/**
	 * Process commands
	 */
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if ( command.getName().equalsIgnoreCase( "patchwork-reload" ) )
		{
			// Check permissions
			if ( !sender.hasPermission( "patchwork.reload" ) )
			{
				sender.sendMessage( ChatColor.RED + "You don't have permission to do that!" );
				return true;
			}
			this.reloadConfig();
			sender.sendMessage( "Reloaded!" );
			return true;
		}
		return true;
	}
	
	
	/**
	 * Death-Chest
	 * Creates a chest containing the player's items when they die
	 * Chests break after a time and spit out the items
	 * @param e
	 */
	@EventHandler( priority=EventPriority.NORMAL )
	public void onDeath( PlayerDeathEvent e )
	{
		if ( !this.getConfig().getBoolean( "deathchests", false ) ) return;
		
		// Alias
		Player player = e.getEntity();
		List<ItemStack> drops = e.getDrops();
		Location loc = player.getLocation();
		World world = player.getWorld();
		
		// Ignore players without drops
		if ( drops.isEmpty() ) return;
		
		// Create the chest 
		Block block = world.getBlockAt( loc );
		block.setType( Material.CHEST );
		Chest chest = ( Chest ) block.getState();
		this._deathchests.add( chest );
		final Chest chest1 = chest;
		
		// Add the items to the chest and remove them from the drops
		Iterator<ItemStack> items = drops.iterator();
		int itemCount = 0;
		while( items.hasNext() )
		{
			if ( itemCount++ >= 27 )
			{
				// Player's inventory contained more items than can fit in one chest. Make another
				block = world.getBlockAt( loc.add( 0, 1, 0 ) );
				block.setType( Material.CHEST );
				chest = ( Chest ) block.getState();
				this._deathchests.add( chest );
				itemCount = 0;
				
			}
			chest.getInventory().addItem( items.next() );
			items.remove();
		}
		
		// Add the hook to break the chest
		final Chest chest2 = ( chest.equals( chest1 ) ? null : chest );
		this.getServer().getScheduler().scheduleSyncDelayedTask( this, new Runnable()
		{
			@Override
			public void run()
			{
				chest1.getBlock().setType( Material.AIR );
				if ( chest2 != null ) chest2.getBlock().setType( Material.AIR );
			}
			
		}, 20L * 30 );
		
	}
	
	/**
	 * Uberenchant
	 * Adds a chance to replace certain enchants with better ones, and a chance to
	 * add a level to the enchantment.
	 * @param e
	 */
	@EventHandler
	public void onEnchant( EnchantItemEvent e )
	{
		// Config settings
		if ( !this.getConfig().getBoolean( "uberenchant.enable", false ) ) return;
		int levelProbability = (int) ( 1 / ( this.getConfig().getDouble( "uberenchant.level", 5 ) / 100 ) );
		int replaceProbability = (int) ( 1 / ( this.getConfig().getDouble( "uberenchant.replace", 20 ) / 100 ) );
		
		// This will hold what enchants we want to apply
		final HashMap<Enchantment, Integer> newEnchants = new HashMap<Enchantment, Integer>();
		
		// Replacement tables
		HashMap<Enchantment,Enchantment> replace = new HashMap<Enchantment,Enchantment>();
		replace.put( Enchantment.DAMAGE_ARTHROPODS, Enchantment.DAMAGE_ALL );				 	// Bane -> Sharpness
		replace.put( Enchantment.DAMAGE_UNDEAD, Enchantment.DAMAGE_ALL ); 						// Bane -> Sharpness
		replace.put( Enchantment.PROTECTION_EXPLOSIONS, Enchantment.PROTECTION_ENVIRONMENTAL); 	// Pro-Explosion -> Pro All
		replace.put( Enchantment.PROTECTION_PROJECTILE, Enchantment.PROTECTION_ENVIRONMENTAL); 	// Pro-Explosion -> Pro All
		
		// What item do we have
		final ItemStack item = e.getItem();
		
		
		// Check what enchantment we want to apply
		Map<Enchantment, Integer> enchants = e.getEnchantsToAdd();
		Iterator<Enchantment> enchantIterator = enchants.keySet().iterator();
		while( enchantIterator.hasNext() )
		{
			Enchantment enchant = enchantIterator.next();
			int level = enchants.get( enchant );			
			
			// 5% chance to bump a level
			Random generatorLevel = new Random( System.currentTimeMillis() );
			if ( generatorLevel.nextInt( levelProbability ) + 1 == 1 )
			{
				int newlevel = Math.min( enchant.getMaxLevel(), level + 1);
				level = newlevel;
			}
			
			// 20% chance to replace with a better enchant
			if ( replace.containsKey( enchant ) )
			{
				Random generatorReplace = new Random( System.currentTimeMillis() );
				if ( generatorReplace.nextInt( replaceProbability ) + 1 == 1 )
				{
					Enchantment better = replace.get( enchant );
					
					// Prevent sharp V from going onto non-gold, and cap stone swords at sharp 3
					if ( better.equals( Enchantment.DAMAGE_ALL ) )
					{
						if ( item.getType().equals( Material.STONE_SWORD ) ) level = Math.min( level, 3 );
						else if ( !item.getType().equals( Material.GOLD_SWORD) ) level = Math.min( level, 4 );
					} else
					
					// Prevent efficiency V from going onto non-gold, and cap stone tools at efficiency 4
					if ( better.equals( Enchantment.DIG_SPEED) )
					{
						if ( item.getType().equals( Material.STONE_PICKAXE ) ) level = Math.min( level, 3 );
						else if ( item.getType().equals( Material.STONE_SPADE ) ) level = Math.min( level, 3 );
						else if ( item.getType().equals( Material.STONE_AXE ) ) level = Math.min( level, 3 );
						else if ( !item.getType().equals( Material.GOLD_PICKAXE ) ) level = Math.min( level, 4 );
						else if ( !item.getType().equals( Material.GOLD_SPADE ) ) level = Math.min( level, 4 );
						else if ( !item.getType().equals( Material.GOLD_AXE ) ) level = Math.min( level, 4 );
					}
					
					newEnchants.put( better, level );
				}
				else newEnchants.put( enchant, level );
			} else newEnchants.put( enchant, level );
		}
		
		// Remove the old enchantments and add the new ones
		this.getServer().getScheduler().scheduleSyncDelayedTask( this, new Runnable()
		{
			@Override
			public void run()
			{
				// Remove the old enchants
				Map<Enchantment, Integer> enchants = item.getEnchantments();
				Iterator<Enchantment> enchantIterator = enchants.keySet().iterator();
				while( enchantIterator.hasNext() ) item.removeEnchantment( enchantIterator.next() );
				
				// Add the new ones
				enchantIterator = newEnchants.keySet().iterator();
				while( enchantIterator.hasNext() )
				{
					Enchantment enchant = enchantIterator.next();
					item.addEnchantment( enchant, newEnchants.get( enchant ) );
				}
			}
		}, 1L );
	}
	
	/**
	 * Disenchant
	 * Removes enchantments from items when they are rightclicked in the enchanting menu
	 * @param e
	 */
	@EventHandler
	public void onEnchantRightclick( InventoryClickEvent e )
	{
		if ( !this.getConfig().getBoolean( "disenchant", false ) ) return;
		
		// Check that we want to remove enchants
		if ( !e.getInventory().getName().equalsIgnoreCase( "Enchant" ) ) return;
		if ( !e.getSlotType().name().equalsIgnoreCase( "crafting" ) ) return;
		if ( !e.isRightClick() ) return;
		if ( e.getCurrentItem().getType().getId() == 0 ) return;
		
		// Remove the old enchants
		ItemStack item = e.getCurrentItem();
		Map<Enchantment, Integer> enchants = item.getEnchantments();
		Iterator<Enchantment> enchantIterator = enchants.keySet().iterator();
		while( enchantIterator.hasNext() ) item.removeEnchantment( enchantIterator.next() );
	}
}
