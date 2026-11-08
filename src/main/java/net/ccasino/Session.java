package net.ccasino;

import net.ccasino.screens.Screen;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Ein Spieler am Casino-Tisch. Hält das offene Inventar und den aktuellen Screen. */
public final class Session implements InventoryHolder {
	private final CasinoPlugin plugin;
	private final Player player;
	private Inventory inv;
	private Screen screen;
	private boolean switching;
	private boolean awaitingBet;
	private long askId;
	private long tick;
	private long partyUntil = -1;
	private final java.util.List<Screen> keepAlive = new java.util.ArrayList<>();

	public Session(CasinoPlugin plugin, Player player) {
		this.plugin = plugin;
		this.player = player;
	}

	public CasinoPlugin plugin() { return plugin; }
	public Player player() { return player; }
	public long tick() { return tick; }
	public boolean fancy() { return plugin.fancy(player); }
	public boolean isSwitching() { return switching; }
	public boolean isAwaitingBet() { return awaitingBet; }

	@Override
	public Inventory getInventory() { return inv; }

	public void open(Screen next) {
		if (screen != null && screen != next && !keepAlive.contains(screen)) screen.forceFinish();
		keepAlive.remove(next);
		screen = next;
		inv = Bukkit.createInventory(this, 54, next.title());
		next.render(inv);
		switching = true;
		try {
			player.openInventory(inv);
		} finally {
			switching = false;
		}
	}

	/** Wechsel aus einem Klick heraus: erst im nächsten Tick öffnen (Bukkit-Empfehlung). */
	public void switchTo(Screen next) {
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player.isOnline() && inv != null && player.getOpenInventory().getTopInventory() == inv) open(next);
		});
	}

	/** Screen, der beim Wechsel nicht abgerechnet werden soll (z. B. Roulette während der Zahlenwahl). */
	public void keep(Screen s, boolean keep) {
		if (keep) keepAlive.add(s);
		else keepAlive.remove(s);
	}

	public void update() {
		tick++;
		if (screen == null || inv == null) return;
		screen.tick();
		screen.render(inv);
	}

	public void click(int slot, boolean right) {
		if (screen != null) screen.click(slot, right);
	}

	public void finishAll() {
		for (Screen s : keepAlive) s.forceFinish();
		keepAlive.clear();
		if (screen != null) screen.forceFinish();
	}

	// ------------------------------------------------------------------ Einsatz

	public double bet() { return plugin.bet(player); }

	/** Inventar schließen und den Einsatz im Chat abfragen. */
	public void askBet() {
		Bukkit.getScheduler().runTask(plugin, () -> {
			awaitingBet = true;
			long id = ++askId;
			player.closeInventory();
			player.sendMessage(Tiles.text("§6[Casino] §7Schreib deinen §eEinsatz §7in den Chat, z. B. §e2500§7, §e100k §7oder §e1.5m§7."));
			player.sendMessage(Tiles.text("§6[Casino] §7Guthaben: §e" + plugin.money(plugin.balance(player)) + " §8· §7„§cabbrechen§7“ zum Zurückgehen"));
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				if (awaitingBet && askId == id && player.isOnline()) {
					awaitingBet = false;
					player.sendMessage(Tiles.text("§6[Casino] §7Eingabe abgelaufen – mit §e/casino §7geht's zurück an den Tisch."));
				}
			}, 20L * 60);
		});
	}

	/** Nach der Chat-Eingabe wieder an den Tisch. */
	public void reopen() {
		awaitingBet = false;
		if (player.isOnline() && screen != null) open(screen);
	}

	/** Eingabe abbrechen, ohne den Tisch wieder zu öffnen (z. B. bei /casino). */
	public void cancelAsk() { awaitingBet = false; }
	public int betIndex() { return plugin.betIndex(player); }
	public void setBetIndex(int i) { plugin.setBetIndex(player, i); }

	// ------------------------------------------------------------------ Party (blinkende Lichter)

	public void party(int ticks) { partyUntil = tick + ticks; }
	public boolean party() { return tick < partyUntil; }
}
