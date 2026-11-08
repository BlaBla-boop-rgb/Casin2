package net.ccasino;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.ccasino.screens.LobbyScreen;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class CasinoPlugin extends JavaPlugin implements Listener {
	private static CasinoPlugin instance;

	private Economy economy;
	private final Map<UUID, Session> sessions = new HashMap<>();
	private final Map<UUID, Integer> betIndex = new HashMap<>();
	private final Map<UUID, Double> customBet = new HashMap<>();
	private final Set<UUID> packLoaded = new HashSet<>();
	private double[] chips = {10, 50, 100, 500, 1000};
	private ResourcePackInfo packInfo;
	private UUID packId;

	public static CasinoPlugin get() { return instance; }

	@Override
	public void onEnable() {
		instance = this;
		saveDefaultConfig();
		if (!setupEconomy()) {
			getLogger().severe("Kein Vault-Economy-Plugin gefunden (z. B. EssentialsX). Craft Casino wird deaktiviert.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		loadSettings();
		getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().runTaskTimer(this, () -> {
			for (Session s : sessions.values().toArray(new Session[0])) {
				try {
					s.update();
				} catch (Exception ex) {
					getLogger().warning("Fehler in Casino-Session von " + s.player().getName() + ": " + ex);
					ex.printStackTrace();
				}
			}
		}, 1L, 1L);
	}

	@Override
	public void onDisable() {
		for (Session s : sessions.values().toArray(new Session[0])) {
			s.finishAll();
			s.player().closeInventory();
		}
		sessions.clear();
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) return false;
		economy = rsp.getProvider();
		return economy != null;
	}

	private void loadSettings() {
		reloadConfig();
		List<Double> list = getConfig().getDoubleList("chips");
		if (list.size() == 5) {
			chips = new double[5];
			for (int i = 0; i < 5; i++) chips[i] = list.get(i);
		} else {
			getLogger().warning("config.yml: 'chips' braucht genau 5 Werte – benutze Standard 10/50/100/500/1000.");
		}
		packInfo = null;
		String url = getConfig().getString("resource-pack.url", "");
		if (getConfig().getBoolean("resource-pack.enabled", true) && url != null && !url.isBlank()) {
			String u = url.trim();
			packId = UUID.nameUUIDFromBytes(("ccasino:" + u).getBytes(StandardCharsets.UTF_8));
			try {
				// sofort ohne Prüfsumme nutzbar – die Prüfsumme wird im Hintergrund nachgereicht
				packInfo = ResourcePackInfo.resourcePackInfo().id(packId).uri(URI.create(u)).build();
				getLogger().info("Casino-Resourcepack: " + u);
				ResourcePackInfo.resourcePackInfo().id(packId).uri(URI.create(u)).computeHashAndBuild()
						.whenComplete((info, err) -> {
							if (err != null) {
								getLogger().warning("Prüfsumme des Casino-Packs nicht berechenbar (" + err.getMessage() + ") – wird ohne Prüfsumme gesendet.");
							} else {
								packInfo = info;
								getLogger().info("Casino-Resourcepack bereit (SHA-1 " + info.hash() + ").");
							}
						});
			} catch (Exception ex) {
				packInfo = null;
				getLogger().warning("Ungültige Resourcepack-URL: " + u);
			}
		} else {
			getLogger().info("Kein Casino-Resourcepack eingestellt – das Casino läuft im Vanilla-Look.");
		}
	}

	// ------------------------------------------------------------------ Economy

	public double balance(Player p) { return economy.getBalance(p); }

	public boolean take(Player p, double amount) {
		if (amount <= 0 || !economy.has(p, amount)) return false;
		return economy.withdrawPlayer(p, amount).transactionSuccess();
	}

	public void give(Player p, double amount) {
		if (amount > 0) economy.depositPlayer(p, amount);
	}

	public String money(double v) { return economy.format(v); }

	public double[] chips() { return chips; }

	public int betIndex(Player p) { return betIndex.getOrDefault(p.getUniqueId(), 1); }

	public void setBetIndex(Player p, int i) { betIndex.put(p.getUniqueId(), i); }

	/** Index 0–4 = Chips, 5 = eigener Einsatz */
	public double bet(Player p) {
		int i = betIndex(p);
		if (i == 5) return customBet.getOrDefault(p.getUniqueId(), chips[0]);
		return chips[Math.max(0, Math.min(4, i))];
	}

	public Double customBet(Player p) { return customBet.get(p.getUniqueId()); }

	public void setCustomBet(Player p, double v) {
		customBet.put(p.getUniqueId(), v);
		betIndex.put(p.getUniqueId(), 5);
	}

	/** "2500", "100.000", "100k", "1,5m", "2 mio" -> Zahl, sonst -1 */
	static double parseAmount(String raw) {
		String s = raw.trim().toLowerCase(java.util.Locale.ROOT).replace(" ", "").replace("€", "").replace("$", "");
		double mul = 1;
		if (s.endsWith("mio")) { mul = 1_000_000; s = s.substring(0, s.length() - 3); }
		else if (s.endsWith("m")) { mul = 1_000_000; s = s.substring(0, s.length() - 1); }
		else if (s.endsWith("k")) { mul = 1_000; s = s.substring(0, s.length() - 1); }
		if (mul > 1) s = s.replace(',', '.');
		else s = s.replace(".", "").replace(",", "");
		try {
			double v = Double.parseDouble(s) * mul;
			return Double.isFinite(v) && v > 0 ? Math.floor(v) : -1;
		} catch (NumberFormatException ex) {
			return -1;
		}
	}

	private void handleBetInput(Player p, String text) {
		Session s = sessions.get(p.getUniqueId());
		if (s == null || !s.isAwaitingBet()) return;
		String t = text.trim().toLowerCase(java.util.Locale.ROOT);
		if (t.equals("abbrechen") || t.equals("cancel") || t.equals("x")) {
			s.reopen();
			return;
		}
		double v = parseAmount(text);
		double min = getConfig().getDouble("min-bet", 1);
		double max = getConfig().getDouble("max-bet", 0);
		if (v < 0) {
			p.sendMessage(Tiles.text("§6[Casino] §cDas ist keine gültige Zahl. §7Beispiel: §e2500 §7oder §e100k"));
			return;
		}
		if (v < min) {
			p.sendMessage(Tiles.text("§6[Casino] §cMindesteinsatz: §e" + money(min)));
			return;
		}
		if (max > 0 && v > max) {
			p.sendMessage(Tiles.text("§6[Casino] §cHöchsteinsatz: §e" + money(max)));
			return;
		}
		if (v > balance(p)) {
			p.sendMessage(Tiles.text("§6[Casino] §cSo viel hast du nicht. §7Guthaben: §e" + money(balance(p))));
			return;
		}
		setCustomBet(p, v);
		p.sendMessage(Tiles.text("§6[Casino] §7Einsatz gesetzt: §e" + money(v)));
		Fx.chip(p);
		s.reopen();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(io.papermc.paper.event.player.AsyncChatEvent e) {
		Player p = e.getPlayer();
		Session s = sessions.get(p.getUniqueId());
		if (s == null || !s.isAwaitingBet()) return;
		e.setCancelled(true);
		String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.message());
		Bukkit.getScheduler().runTask(this, () -> handleBetInput(p, text));
	}

	// ------------------------------------------------------------------ Resourcepack

	public boolean fancy(Player p) {
		return packInfo != null && packLoaded.contains(p.getUniqueId());
	}

	private void sendPack(Player p) {
		if (packInfo == null) return;
		String prompt = getConfig().getString("resource-pack.prompt", "");
		ResourcePackRequest req = ResourcePackRequest.resourcePackRequest()
				.packs(packInfo)
				.required(getConfig().getBoolean("resource-pack.required", false))
				.replace(false) // vorhandene Server-Packs (z. B. ClansPack) bleiben aktiv
				.prompt(Tiles.text(prompt))
				.build();
		p.sendResourcePacks(req);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		Bukkit.getScheduler().runTaskLater(this, () -> {
			if (p.isOnline()) sendPack(p);
		}, 40L);
	}

	@EventHandler
	public void onPackStatus(PlayerResourcePackStatusEvent e) {
		if (packId == null || !packId.equals(e.getID())) return;
		switch (e.getStatus()) {
			case SUCCESSFULLY_LOADED -> packLoaded.add(e.getPlayer().getUniqueId());
			case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> packLoaded.remove(e.getPlayer().getUniqueId());
			default -> { }
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Session s = sessions.remove(e.getPlayer().getUniqueId());
		if (s != null) s.finishAll();
		packLoaded.remove(e.getPlayer().getUniqueId());
	}

	// ------------------------------------------------------------------ Befehl

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("casino.admin")) {
				sender.sendMessage(Tiles.text("§cDazu hast du keine Rechte."));
				return true;
			}
			loadSettings();
			sender.sendMessage(Tiles.text("§6[Casino] §7Konfiguration neu geladen."));
			return true;
		}
		if (!(sender instanceof Player p)) {
			sender.sendMessage("Nur für Spieler.");
			return true;
		}
		if (args.length > 1 && (args[0].equalsIgnoreCase("einsatz") || args[0].equalsIgnoreCase("bet"))) {
			double v = parseAmount(String.join("", java.util.Arrays.copyOfRange(args, 1, args.length)));
			if (v <= 0) {
				p.sendMessage(Tiles.text("§6[Casino] §cUngültiger Betrag. §7Beispiel: §e/casino einsatz 100k"));
				return true;
			}
			setCustomBet(p, v);
			p.sendMessage(Tiles.text("§6[Casino] §7Eigener Einsatz: §e" + money(v)));
			return true;
		}
		if (args.length > 0 && args[0].equalsIgnoreCase("pack")) {
			if (packInfo == null) p.sendMessage(Tiles.text("§6[Casino] §7Auf diesem Server ist kein Casino-Pack eingestellt."));
			else sendPack(p);
			return true;
		}
		openCasino(p);
		return true;
	}

	public void openCasino(Player p) {
		Session s = sessions.get(p.getUniqueId());
		if (s == null) {
			s = new Session(this, p);
			sessions.put(p.getUniqueId(), s);
		}
		s.cancelAsk();
		s.open(new LobbyScreen(s));
	}

	// ------------------------------------------------------------------ Inventar-Events

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent e) {
		if (!(e.getView().getTopInventory().getHolder(false) instanceof Session s)) return;
		e.setCancelled(true);
		int raw = e.getRawSlot();
		if (raw < 0 || raw >= 54) return;
		ClickType c = e.getClick();
		if (c == ClickType.DOUBLE_CLICK) return;
		boolean right = c.isRightClick();
		if (!right && !c.isLeftClick()) return;
		try {
			s.click(raw, right);
		} catch (Exception ex) {
			getLogger().warning("Klick-Fehler: " + ex);
			ex.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDrag(InventoryDragEvent e) {
		if (e.getView().getTopInventory().getHolder(false) instanceof Session) e.setCancelled(true);
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (!(e.getInventory().getHolder(false) instanceof Session s)) return;
		if (s.isSwitching() || s.isAwaitingBet()) return;
		s.finishAll();
		sessions.remove(s.player().getUniqueId());
	}
}
