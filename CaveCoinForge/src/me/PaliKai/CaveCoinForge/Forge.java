package me.PaliKai.CaveCoinForge;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class Forge {
	public Main main;
	public Block chest;
	public Inventory inventory;
	public UUID minion;
	public UUID owner;
	
	public BukkitTask task;
	
	public Forge(Main main, Block chest, Inventory inventory, UUID minion, UUID owner) {
		this.main = main;
		this.chest = chest;
		this.inventory = inventory;
		this.minion = minion;
		this.owner = owner;
		startRunnable();
	}
	
	public void refine() {
		ItemStack Unrefined = inventory.getItem(11);
		
		ItemStack Refined = inventory.getItem(15);
		if (Unrefined != null) {
			if (Unrefined.getType().equals(Material.PLAYER_HEAD)) {
				if (Unrefined.getItemMeta().getLocalizedName().equalsIgnoreCase("UnprocessedCaveCoin")) {
					if (Unrefined.getAmount() >= main.convert) {
						ItemStack itemToAdd = main.caveCoin();
						int addable = -1;
						for (int i = 0; i < ((Chest) chest.getState()).getBlockInventory().getSize(); i++) {
							if (((Chest) chest.getState()).getBlockInventory().getItem(i) == null) {
								addable = i;
								break;
							} else {
								if (((Chest) chest.getState()).getBlockInventory().getItem(i).getType().equals(Material.PLAYER_HEAD)) {
									if (((Chest) chest.getState()).getBlockInventory().getItem(i).getItemMeta().getLocalizedName().equalsIgnoreCase("CaveCoin")) {
										if (((Chest) chest.getState()).getBlockInventory().getItem(i).getAmount() <= 63) {
											addable = i;
											break;
										}
									}
								}
							}
						}
						if (addable != -1) {
							if (((Chest) chest.getState()).getBlockInventory().getItem(addable) == null) {
								((Chest) chest.getState()).getBlockInventory().setItem(addable, itemToAdd);
							} else {
								((Chest) chest.getState()).getBlockInventory().getItem(addable).setAmount(((Chest) chest.getState()).getBlockInventory().getItem(addable).getAmount() + 1);
							}
							Unrefined.setAmount(Unrefined.getAmount() - main.convert);
							effects(chest.getLocation().add(.5, 1, .5));
						} else {
							if (Refined == null) {
								inventory.setItem(15, itemToAdd);
							} else {
								if (Refined.getItemMeta().getLocalizedName().equalsIgnoreCase("CaveCoin")) {
									if (Refined.getAmount() <= 63) {
										Refined.setAmount(Refined.getAmount() + 1);
										Unrefined.setAmount(Unrefined.getAmount() - main.convert);
										effects(chest.getLocation().add(.5, 1, .5));
									} else {
										// full
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public void effects(Location loc) {
		loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1F, 1F);
		loc.getWorld().spawnParticle(Particle.LAVA, loc, 10, .2, .2, .2);
	}
	
	public void breakMinion(boolean drop) {
		ArmorStand as = null;
		for (Entity entity : Bukkit.getWorld("plot").getNearbyEntities(chest.getLocation().add(new Vector(0, 1, 0)), .5, .5, .5)) {
			if (entity.getUniqueId().equals(minion)) {
				as = (ArmorStand) entity;
				break;
			}
		}
		if (as != null && inventory != null) {
			if (inventory.getItem(11) != null) {
				as.getWorld().dropItemNaturally(as.getLocation(), inventory.getItem(11));
			}
			if (inventory.getItem(15) != null) {
				as.getWorld().dropItemNaturally(as.getLocation(), inventory.getItem(15));
			}
			as.remove();
		}
		minion = null;
		inventory = null;
		if (drop) {
			chest.getWorld().dropItemNaturally(chest.getLocation().add(0, 1, 0), main.forgeItem());
		}
		chest.setType(Material.AIR);
	}
	
	int ticks = 0;
	int seconds = 0;
	
	public void startRunnable() {
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (minion == null || inventory == null || !(chest.getState() instanceof Chest)) {
					this.cancel();
					return;
				}
				refine();
			}
		};
		
		task = runnable.runTaskTimer(main, 0, main.delay);
	}
}
