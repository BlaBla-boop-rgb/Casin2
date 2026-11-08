package net.ccasino.screens;

import net.ccasino.Session;
import org.bukkit.inventory.Inventory;

/** Zahlenfeld 0–36 für Einzelwetten (36x). Teilt sich die Einsätze mit dem Roulette-Tisch. */
public class NumberPickerScreen extends Screen {
	private final RouletteScreen table;

	public NumberPickerScreen(Session s, RouletteScreen table) {
		super(s);
		this.table = table;
	}

	@Override protected String gui() { return "picker"; }
	@Override protected String plainTitle() { return "§cRoulette §8» §7Zahl wählen"; }
	@Override protected Screen backTarget() { return table; }

	@Override
	protected String[] info() {
		return new String[] {"§7Links-Klick: §e+Einsatz auf die Zahl", "§7Rechts-Klick: §czurücknehmen",
				"§7Treffer zahlt §e36x", "", "§7Zurück zum Tisch zum Drehen"};
	}

	private static int numberAt(int slot) {
		if (slot == 4) return 0;
		if (slot >= 9 && slot < 45) return slot - 8; // Reihen 1–4: 1..36
		return -1;
	}

	@Override
	protected void draw(Inventory inv) {
		for (int c = 0; c < 9; c++) if (c != 4) inv.setItem(c, deco("felt"));
		for (int slot = 0; slot < 45; slot++) {
			int n = numberAt(slot);
			if (n < 0) continue;
			Double v = table.bets.get("n:" + n);
			inv.setItem(slot, v == null
					? item("rn_" + n, RouletteScreen.numName(n), "§7Auszahlung §e36x", "§eLinks: §7+" + pl.money(s.bet()))
					: item("rn_" + n + "_hl", true, RouletteScreen.numName(n), "§6Gesetzt: §e" + pl.money(v),
							"§eLinks: §7+" + pl.money(s.bet()), "§eRechts: §7zurücknehmen"));
		}
		double t = table.total();
		inv.setItem(0, item("coin", "§7Gesamt gesetzt: §e" + pl.money(t), "§7Gedreht wird am Tisch (Zurück)"));
	}

	@Override
	protected void onClick(int slot, boolean right) {
		int n = numberAt(slot);
		if (n < 0) return;
		if (right) table.remove("n:" + n);
		else table.place("n:" + n);
	}
}
