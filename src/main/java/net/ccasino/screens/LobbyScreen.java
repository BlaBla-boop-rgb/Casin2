package net.ccasino.screens;

import net.ccasino.Fx;
import net.ccasino.Session;
import org.bukkit.inventory.Inventory;

/** Startseite: vier Spieltische, Kontostand als LED-Anzeige. */
public class LobbyScreen extends Screen {
	private static final int[] GAME_SLOTS = {slot(2, 1), slot(2, 3), slot(2, 5), slot(2, 7)};

	public LobbyScreen(Session s) {
		super(s);
	}

	@Override protected String gui() { return "lobby"; }
	@Override protected String plainTitle() { return "§6§lCraft Casino §8» §7Lobby"; }
	@Override protected Screen backTarget() { return null; }
	@Override protected boolean isLobby() { return true; }

	@Override
	protected String[] info() {
		return new String[] {
				"§7Wähle unten deinen §eEinsatz§7,",
				"§7dann einen Tisch.",
				"",
				"§7Gespielt wird mit deinem",
				"§7Server-Guthaben. §cSpiel verantwortungsvoll!"};
	}

	@Override
	protected void draw(Inventory inv) {
		inv.setItem(GAME_SLOTS[0], item("game_slots", true, "§d§lSlots",
				"§73 Walzen, 1 Gewinnlinie", "§7Drei Netherstars zahlen §6300x§7!", "", "§e▶ Klicken zum Spielen"));
		inv.setItem(GAME_SLOTS[1], item("game_roulette", true, "§c§lRoulette",
				"§7Europäischer Kessel", "§7Setze auf Farben, Dutzende", "§7oder einzelne Zahlen (36x)", "", "§e▶ Klicken zum Spielen"));
		inv.setItem(GAME_SLOTS[2], item("game_blackjack", true, "§9§lBlackjack",
				"§7Schlag den Dealer, ohne", "§7über 21 zu kommen", "§7Blackjack zahlt §63:2", "", "§e▶ Klicken zum Spielen"));
		inv.setItem(GAME_SLOTS[3], item("game_crash", true, "§6§lCrash",
				"§7Die Rakete steigt …", "§7Zahl rechtzeitig aus,", "§7bevor sie explodiert!", "", "§e▶ Klicken zum Spielen"));

		double bal = pl.balance(p);
		led(inv, slot(4, 1), 7, compact(bal, 7), 'a', "§6Guthaben: §e" + pl.money(bal));
	}

	@Override
	protected void onClick(int slot, boolean right) {
		Screen next = null;
		if (slot == GAME_SLOTS[0]) next = new SlotsScreen(s);
		else if (slot == GAME_SLOTS[1]) next = new RouletteScreen(s);
		else if (slot == GAME_SLOTS[2]) next = new BlackjackScreen(s);
		else if (slot == GAME_SLOTS[3]) next = new CrashScreen(s);
		if (next != null) {
			Fx.click(p);
			Fx.bell(p, 1.4f);
			s.switchTo(next);
		}
	}
}
