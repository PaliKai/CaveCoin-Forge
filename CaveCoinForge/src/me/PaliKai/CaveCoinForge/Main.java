package me.PaliKai.CaveCoinForge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

//Programmed by PaliKai

//Inventory Serialization by graywolf336 - https://gist.github.com/graywolf336/8153678

public class Main extends JavaPlugin implements Listener {
	
	public Long delay = 36000L;
	public int convert = 8;
	public int chance = 12;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(this, this);
		this.saveDefaultConfig();
		this.reloadConfig();
		
		if (this.getConfig().getList("chests") != null && this.getConfig().getList("inventories") != null && this.getConfig().getList("armorstands") != null && this.getConfig().getList("UUIDs") != null) {
			Bukkit.broadcastMessage("RL");
			LinkedList<Vector> chests = (LinkedList<Vector>) convertALtoLL(this.getConfig().getList("chests"));
			LinkedList<String> inventories = (LinkedList<String>) convertALtoLL(this.getConfig().getList("inventories"));
			LinkedList<String> armorstands = (LinkedList<String>) convertALtoLL(this.getConfig().getList("armorstands"));
			LinkedList<String> UUIDs = (LinkedList<String>) convertALtoLL(this.getConfig().getList("UUIDs"));
			
			for (int i = 0; i < chests.size(); i++) {
				if (chests.get(i).toLocation(Bukkit.getWorld("plot")).getBlock().getType().equals(Material.CHEST)) {
					try {
						forges.add(new Forge(this, Bukkit.getWorld("plot").getBlockAt(chests.get(i).toLocation(Bukkit.getWorld("plot"))), fromBase64(inventories.get(i)), UUID.fromString(armorstands.get(i)), UUID.fromString(UUIDs.get(i))));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		if (this.getConfig().getLong("delay") > 0) {
			delay = this.getConfig().getLong("delay");
		} else {
			delay = 36000L;
		}
		if (this.getConfig().getInt("convert") > 0) {
			convert = this.getConfig().getInt("convert");
		} else {
			convert = 8;
		}
		if (this.getConfig().getInt("chance") > 0) {
			chance = this.getConfig().getInt("chance");
		} else {
			chance = 12;
		}
	}
	
	@Override
	public void onDisable() {
		
		LinkedList<Vector> chests = new LinkedList<Vector>();
		LinkedList<String> inventories = new LinkedList<String>();
		LinkedList<String> armorstands = new LinkedList<String>();
		LinkedList<String> UUIDs = new LinkedList<String>();
		
		for (Forge forge : forges) {
			chests.add(forge.chest.getLocation().toVector());
			inventories.add(toBase64(forge.inventory));
			armorstands.add(forge.minion.toString());
			UUIDs.add(forge.owner.toString());
		}
		
		this.getConfig().set("chests", chests.toArray());
		
		this.getConfig().set("inventories", inventories.toArray());
		
		this.getConfig().set("armorstands", armorstands.toArray());
		
		this.getConfig().set("UUIDs", UUIDs.toArray());
		
		this.getConfig().set("delay", delay);
		
		this.getConfig().set("convert", convert);
		
		this.getConfig().set("chance", chance);
		
		this.saveConfig();
		this.saveDefaultConfig();
		this.reloadConfig();
	}
	
	public LinkedList<Forge> forges = new LinkedList<Forge>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("Forge") || label.equalsIgnoreCase("f")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (player.hasPermission("forge.use")) {
					if (args.length == 0) {
						player.getInventory().addItem(forgeItem());
					} else {
						if (args[0].equalsIgnoreCase("clear")) {
							for (Forge forge : forges) {
								forge.breakMinion(true);
							}
							forges.clear();
							player.sendMessage(ChatColor.GREEN + "All forges cleared.");
						}
						if (args[0].equalsIgnoreCase("list")) {
							String list = "";
							if (!forges.isEmpty()) {
								for (Forge forge : forges) {
									list = list + forge.chest.getX() + " " + forge.chest.getY() + " " + forge.chest.getZ() + (forge.equals(forges.getLast()) ? "" : ", ");
								}
							} else {
								list = ChatColor.RED + "There are no saved forges.";
							}
							player.sendMessage(list);
						}
						if (args[0].equalsIgnoreCase("chance") || args[0].equalsIgnoreCase("chances")) {
							if (args.length == 2) {
								if (args[1].equalsIgnoreCase("reset")) {
									chance = 12;
									player.sendMessage(ChatColor.GREEN + "Chances reset to " + ChatColor.YELLOW + 1 + ChatColor.GREEN + " in " + ChatColor.YELLOW + chance + ChatColor.GREEN + ".");
								} else {
									try {
										if (Integer.parseInt((args[1])) != 0) {
											chance = Integer.parseInt((args[1]));
											restartForges();
											player.sendMessage(ChatColor.GREEN + "Chances are now " + ChatColor.YELLOW + 1 + ChatColor.GREEN + " in " + ChatColor.YELLOW + chance + ChatColor.GREEN + ".");
										} else {
											player.sendMessage(ChatColor.RED + "Please enter a number greater than 0.");
										}
									} catch (NumberFormatException e) {
										player.sendMessage(ChatColor.RED + "Invalid number format.");
									}
								}
							} else {
								player.sendMessage(ChatColor.GREEN + "Chances are " + ChatColor.YELLOW + 1 + ChatColor.GREEN + " in " + ChatColor.YELLOW + chance + ChatColor.GREEN + ".");
							}
						}
						if (args[0].equalsIgnoreCase("delay")) {
							if (args.length == 2) {
								if (args[1].equalsIgnoreCase("reset")) {
									delay = 36000L;
									restartForges();
									player.sendMessage(ChatColor.GREEN + "Delay reset to " + ChatColor.YELLOW + delay + ChatColor.GREEN + " ticks.");
								} else {
									try {
										if (Long.parseLong((args[1])) != 0) {
											delay = Long.parseLong((args[1]));
											restartForges();
											player.sendMessage(ChatColor.GREEN + "Delay set to " + ChatColor.YELLOW + delay + ChatColor.GREEN + " ticks.");
										} else {
											player.sendMessage(ChatColor.RED + "Please enter a number greater than 0.");
										}
									} catch (NumberFormatException e) {
										player.sendMessage(ChatColor.RED + "Invalid number format.");
									}
								}
							} else {
								player.sendMessage(ChatColor.GREEN + "Delay is " + ChatColor.YELLOW + delay + ChatColor.GREEN + " ticks.");
							}
						}
						if (args[0].equalsIgnoreCase("convert") || args[0].equalsIgnoreCase("amount")) {
							if (args.length == 2) {
								if (args[1].equalsIgnoreCase("reset")) {
									convert = 8;
									player.sendMessage(ChatColor.GREEN + "Conversion rate reset to " + ChatColor.YELLOW + convert + ChatColor.GREEN + " Unprocessed Cave Coins per Cave Coin.");
								} else {
									try {
										if (Integer.parseInt((args[1])) != 0) {
											convert = Integer.parseInt((args[1]));
											player.sendMessage(ChatColor.GREEN + "Conversion rate set to " + ChatColor.YELLOW + convert + ChatColor.GREEN + " Unprocessed Cave Coins per Cave Coin.");
										} else {
											player.sendMessage(ChatColor.RED + "Please enter a number greater than 0.");
										}
									} catch (NumberFormatException e) {
										player.sendMessage(ChatColor.RED + "Invalid number format.");
									}
								}
							} else {
								player.sendMessage(ChatColor.GREEN + "Conversion rate is " + ChatColor.YELLOW + convert + ChatColor.GREEN + " Unprocessed Cave Coins per Cave Coin.");
							}
						}
						if (args[0].equalsIgnoreCase("settings")) {
							player.sendMessage(ChatColor.GREEN + "Chances are " + ChatColor.YELLOW + 1 + ChatColor.GREEN + " in " + ChatColor.YELLOW + chance + ChatColor.GREEN + ".");
							player.sendMessage(ChatColor.GREEN + "Delay is set to " + ChatColor.YELLOW + delay + ChatColor.GREEN + " ticks.");
							player.sendMessage(ChatColor.GREEN + "Conversion rate is " + ChatColor.YELLOW + convert + ChatColor.GREEN + " Unprocessed Cave Coins per Cave Coin.");
						}
						if (args[0].equalsIgnoreCase("reset")) {
							delay = 36000L;
							convert = 8;
							chance = 12;
							restartForges();
							player.sendMessage(ChatColor.GREEN + "Forge settings reset to default.");
						}
						if (args[0].equalsIgnoreCase("restart")) {
							restartForges();
							player.sendMessage(ChatColor.GREEN + "Forge cycles restarted.");
						}
					}
				} else {
					player.sendMessage(ChatColor.RED + "You do not have permission!");
				}
			} else {
				sender.sendMessage("Ello Console!");
			}
		} else if (label.equalsIgnoreCase("Forges")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				LinkedList<Forge> plrForges = new LinkedList<Forge>();
				for (Forge forge : forges) {
					if (forge.owner.equals(player.getUniqueId())) {
						plrForges.add(forge);
					}
				}
				if (args.length == 1) {
					if (args[0].equalsIgnoreCase("list")) {
						String list = "";
						if (!plrForges.isEmpty()) {
							for (Forge forge : plrForges) {
								list = list + forge.chest.getX() + " " + forge.chest.getY() + " " + forge.chest.getZ() + (forge.equals(plrForges.getLast()) ? "" : ", ");
							}
						} else {
							list = ChatColor.RED + "You have no saved forges.";
						}
						player.sendMessage(list);
					} else if (args[0].equalsIgnoreCase("clear")) {
						for (Forge forge : plrForges) {
							forge.breakMinion(true);
						}
						forges.removeAll(plrForges);
						player.sendMessage(ChatColor.GREEN + "Forges cleared.");
					}
				} else {
					String list = "";
					if (!plrForges.isEmpty()) {
						for (Forge forge : plrForges) {
							list = list + forge.chest.getX() + " " + forge.chest.getY() + " " + forge.chest.getZ() + (forge.equals(plrForges.getLast()) ? "" : ", ");
						}
					} else {
						list = ChatColor.RED + "You have no saved forges.";
					}
					player.sendMessage(list);
				}
			}
		}
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("Forge") || label.equalsIgnoreCase("f")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (args.length == 1) {
					List<String> returns = new ArrayList<>();
					
					returns.add("list");
					returns.add("clear");
					returns.add("chance");
					returns.add("delay");
					returns.add("convert");
					returns.add("settings");
					returns.add("reset");
					returns.add("restart");
					
					return returns;
				} else {
					if (args.length > 1) {
						if (args[0].equalsIgnoreCase("chance") || args[0].equalsIgnoreCase("chances") || args[0].equalsIgnoreCase("delay") || args[0].equalsIgnoreCase("convert")) {
							List<String> returns = new ArrayList<>();
							
							returns.add("reset");
							
							return returns;
						}
					}
				}
			}
			return Collections.emptyList();
		} else if (label.equalsIgnoreCase("Forges")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (args.length == 1) {
					List<String> returns = new ArrayList<>();
					
					returns.add("list");
					returns.add("clear");
					
					return returns;
				} else {
					return Collections.emptyList();
				}
			}
			return Collections.emptyList();
		}
		return Collections.emptyList();
	}
	
	public void spawnForge(Player player, Block toPlace) {
		toPlace.setType(Material.CHEST);
		BlockData blockData = toPlace.getBlockData(); // Access the block Data
		((Directional) blockData).setFacing(getBlockDir(player, toPlace)); // Set latch direction
		toPlace.setBlockData(blockData);
		
		Location chest = toPlace.getLocation();
		Location loc = toPlace.getLocation().add(0.5, 0.875, 0.5);
		loc.setYaw(getDir(player, toPlace));
		ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		armorStand.setVisible(true);
		armorStand.setGravity(false);
		armorStand.setArms(true);
		armorStand.setInvulnerable(true);
		armorStand.setPersistent(true);
		armorStand.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
		armorStand.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
		armorStand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.ADDING_OR_CHANGING);
		armorStand.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING);
		armorStand.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING);
		armorStand.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING);
		armorStand.setSmall(true);
		armorStand.getEquipment().setBoots(new ItemStack(Material.GOLDEN_BOOTS));
		armorStand.getEquipment().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
		armorStand.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
		armorStand.getEquipment().setHelmet(caveCoin());
		
		Inventory inventory = ForgeGUI();
		
		forges.add(new Forge(this, toPlace, inventory, armorStand.getUniqueId(), player.getUniqueId()));
	}
	
	ItemStack forgeItem() {
		ItemStack forge = new ItemStack(Material.BLAZE_POWDER, 1);
		forge.addUnsafeEnchantment(Enchantment.LURE, 1);
		ItemMeta meta = forge.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GOLD + "Cave Coin Forge");
		forge.setItemMeta(meta);
		return forge;
	}
	
	public ItemStack caveCoin() {
		String Texture = "http://textures.minecraft.net/texture/f509de981a786a980b0bc871ad855b20ce0b701b7a2df14cfff6b3a6e4529723";
		ItemStack item;
		item = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		
		meta.setDisplayName(ChatColor.GREEN + "Cave Coin");
		
		meta.setLocalizedName("CaveCoin");
		
		GameProfile profile = new GameProfile(UUID.fromString("9199e15d-d2c3-455f-b8ee-d7ee58bc1d58"), null);
		byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", Texture).getBytes());
		profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
		Field profileField = null;
		try {
			profileField = meta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(meta, profile);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		item.setItemMeta(meta);
		return item;
	}
	
	public ItemStack unprocessedCaveCoin() {
		String Texture = "http://textures.minecraft.net/texture/78e99ddf9511d35b579067f6fb79a0a2a5b77fc4c2898bbef5712f9853cfbd04";
		ItemStack item;
		item = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		
		meta.setDisplayName(ChatColor.GREEN + "Unprocessed Cave Coin");
		
		meta.setLocalizedName("UnprocessedCaveCoin");
		
		GameProfile profile = new GameProfile(UUID.fromString("9199e15d-d2c3-455f-b8ee-d7ee58bc1d58"), null);
		byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", Texture).getBytes());
		profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
		Field profileField = null;
		try {
			profileField = meta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(meta, profile);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		item.setItemMeta(meta);
		return item;
	}
	
	public ItemStack rightArrow() {
		String Texture = "http://textures.minecraft.net/texture/956a3618459e43b287b22b7e235ec699594546c6fcd6dc84bfca4cf30ab9311";
		ItemStack item;
		item = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + "" + ChatColor.BOLD + "->");
		
		GameProfile profile = new GameProfile(UUID.fromString("9199e15d-d2c3-455f-b8ee-d7ee58bc1d58"), null);
		byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", Texture).getBytes());
		profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
		Field profileField = null;
		try {
			profileField = meta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(meta, profile);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		item.setItemMeta(meta);
		return item;
	}
	
	int[] black = { 0, 1, 3, 4, 5, 7, 8, 9, 17, 18, 19, 21, 22, 23, 25, 26 };
	int[] red = { 2, 10, 12, 20 };
	int[] green = { 6, 14, 16, 24 };
	
	public Inventory ForgeGUI() {
		ItemStack bGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta bMeta = bGlass.getItemMeta();
		bMeta.setDisplayName(" ");
		bGlass.setItemMeta(bMeta);
		ItemStack rGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
		ItemMeta rMeta = rGlass.getItemMeta();
		rMeta.setDisplayName(" ");
		rGlass.setItemMeta(rMeta);
		ItemStack gGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
		ItemMeta gMeta = gGlass.getItemMeta();
		gMeta.setDisplayName(" ");
		gGlass.setItemMeta(gMeta);
		
		Inventory inventory = Bukkit.createInventory(null, 27, "Cave Coin Forge");
		for (int b : black) {
			inventory.setItem(b, bGlass);
		}
		for (int r : red) {
			inventory.setItem(r, rGlass);
		}
		for (int g : green) {
			inventory.setItem(g, gGlass);
		}
		inventory.setItem(13, rightArrow());
		
		return inventory;
	}
	
	private ArrayList<Player> cooldown = new ArrayList<Player>();
	
	@EventHandler
	public void Click(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getItem() != null && event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = event.getItem();
			Block block = event.getClickedBlock();
			Block toPlace = block.getRelative(event.getBlockFace());
			if (item.isSimilar(forgeItem())) {
				if (event.getPlayer().getWorld().getName().equalsIgnoreCase("plot") || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
					if (!block.getType().toString().contains("SIGN")) {
						if (!cooldown.contains(player)) {
							spawnForge(player, toPlace);
							item.setAmount(item.getAmount() - 1);
							cooldown.add(player);
							this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
								@Override
								public void run() {
									cooldown.remove(player);
								}
							}, 5L);
						}
					}
				} else {
					event.getPlayer().sendMessage(ChatColor.RED + "You cannot do this in this world.");
				}
			}
		}
		
	}
	
	@EventHandler
	public void clickEntity(PlayerInteractAtEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		if (entity.getType().equals(EntityType.ARMOR_STAND)) {
			if (getForgeFromMinion(entity.getUniqueId()) != null) {
				if (getForgeFromMinion(entity.getUniqueId()).owner.equals(player.getUniqueId())) {
					player.openInventory(getForgeFromMinion(entity.getUniqueId()).inventory);
				} else {
					player.sendMessage(ChatColor.RED + "This is not your forge!");
				}
			}
		}
	}
	
	@EventHandler
	public void invClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		Inventory inv = event.getClickedInventory();
		ItemStack item = event.getCurrentItem();
		int slot = event.getSlot();
		InventoryAction action = event.getAction();
		if (item != null) {
			if (getForgeFromInventory(inv) != null) {
				if (slot != 11 && slot != 15) {
					event.setCancelled(true);
					
				} else {
					if (slot == 11) {
						if (!item.getType().equals(Material.PLAYER_HEAD)) {
							event.setCancelled(true);
						}
					} else {
						if (!item.getType().equals(Material.PLAYER_HEAD)) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void Break(BlockBreakEvent event) {
		if (event.getBlock().getWorld().getName().equalsIgnoreCase("plot") || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
			Player player = event.getPlayer();
			if (event.getBlock().getType().equals(Material.GOLD_ORE)) {
				Random rand = new Random();
				if (rand.nextInt(chance) == 0) {
					event.setDropItems(false);
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), unprocessedCaveCoin());
				}
			} else if (event.getBlock().getType().equals(Material.CHEST)) {
				Block chest = event.getBlock();
				if (getForgeFromChest(chest) != null) {
					Forge forge = getForgeFromChest(chest);
					event.setDropItems(false);
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), forgeItem());
					forge.breakMinion(false);
					forges.remove(forge);
					
				}
			}
		}
	}
	
	@EventHandler
	public void Place(BlockPlaceEvent event) {
		if ((event.getBlock().getType().equals(Material.PLAYER_HEAD) || event.getBlock().getType().equals(Material.PLAYER_WALL_HEAD) || event.getBlock().getType().equals(Material.GOLD_ORE)) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
		}
	}
	
	public void restartForges() {
		for (Forge forge : forges) {
			forge.task.cancel();
			forge.startRunnable();
		}
	}
	
	public int getDir(Player player, Block block) {
		Vector plr = player.getLocation().toVector();
		Vector loc = block.getLocation().toVector();
		
		// Bukkit.broadcastMessage((plr.getX() - block.getX()) + ", " + (plr.getZ() -
		// block.getZ()));
		
		if (Math.abs(plr.getX() - block.getX()) > Math.abs(plr.getZ() - block.getZ())) {
			if (plr.subtract(loc).getX() > 0) {
				return 270;
			} else {
				return 90;
			}
		} else {
			if (plr.subtract(loc).getZ() > 0) {
				return 0;
			} else {
				return 180;
			}
		}
		
	}
	
	public BlockFace getBlockDir(Player player, Block block) {
		Vector plr = player.getLocation().toVector();
		Vector loc = block.getLocation().toVector();
		
		// Bukkit.broadcastMessage((plr.getX() - block.getX()) + ", " + (plr.getZ() -
		// block.getZ()));
		
		if (Math.abs(plr.getX() - block.getX()) > Math.abs(plr.getZ() - block.getZ())) {
			if (plr.subtract(loc).getX() > 0) {
				return BlockFace.EAST;
			} else {
				return BlockFace.WEST;
			}
		} else {
			if (plr.subtract(loc).getZ() > 0) {
				return BlockFace.SOUTH;
			} else {
				return BlockFace.NORTH;
			}
		}
		
	}
	
	public Forge getForgeFromMinion(UUID armorStand) {
		for (Forge forge : forges) {
			if (forge.minion.equals(armorStand)) {
				return forge;
			}
		}
		return null;
	}
	
	public Forge getForgeFromInventory(Inventory inv) {
		for (Forge forge : forges) {
			if (forge.inventory.equals(inv)) {
				return forge;
			}
		}
		return null;
	}
	
	public Forge getForgeFromChest(Block chest) {
		for (Forge forge : forges) {
			if (forge.chest.equals(chest)) {
				return forge;
			}
		}
		return null;
	}
	
	public static <T> LinkedList<T> convertALtoLL(List<T> aL) {
		LinkedList<T> lL = new LinkedList<T>();
		for (T t : aL) {
			lL.add(t);
		}
		return lL;
	}
	
	public LinkedList<Player> addAll(List<?> obj) {
		LinkedList<Player> list = new LinkedList<Player>();
		
		for (Object object : obj) {
			list.add((Player) object);
		}
		
		return list;
	}
	
	public static String toBase64(Inventory inventory) throws IllegalStateException {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
			
			// Write the size of the inventory
			dataOutput.writeInt(inventory.getSize());
			
			// Save every element in the list
			for (int i = 0; i < inventory.getSize(); i++) {
				dataOutput.writeObject(inventory.getItem(i));
			}
			
			// Serialize that array
			dataOutput.close();
			return Base64Coder.encodeLines(outputStream.toByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to save item stacks.", e);
		}
	}
	
	public static Inventory fromBase64(String data) throws IOException {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
			BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
			Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());
			
			// Read the serialized inventory
			for (int i = 0; i < inventory.getSize(); i++) {
				inventory.setItem(i, (ItemStack) dataInput.readObject());
			}
			
			dataInput.close();
			return inventory;
		} catch (ClassNotFoundException e) {
			throw new IOException("Unable to decode class type.", e);
		}
	}
	
}

/*


















*/
