package net.ccasino.screens;

import java.util.Locale;
import java.util.Random;

import net.ccasino.CasinoPlugin;
import net.ccasino.Fx;
import net.ccasino.Session;
import net.ccasino.Tiles;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Basis für alle Casino-Ansichten (6 Reihen):
 * Reihe 0 = Lauflichter, Reihen 1–4 = Spielfläche, Reihe 5 = Steuerleiste.
 */
public abstract class Screen {
	protected final Session s;
	protected final Player p;
	protected final CasinoPlugin pl;
	protected final Random rnd = new Random();
	protected boolean fancy;

	protected Screen(Session s) {
		this.s = s;
		this.p = s.player();
		this.pl = s.plugin();
	}

	protected abstract String gui();
	protected abstract String plainTitle();
	protected abstract void draw(Inventory inv);
	protected abstract void onClick(int slot, boolean right);

	public void tick() {}
	public boolean busy() { return false; }
	public void forceFinish() {}

	/** Info-Text für den Info-Knopf. */
	protected String[] info() { return new String[0]; }

	/** Wohin führt der Zurück-Knopf? null = Casino schließen */
	protected Screen backTarget() { return new LobbyScreen(s); }

	protected boolean isLobby() { return false; }

	public Component title() {
		fancy = s.fancy();
		return Tiles.title(gui(), fancy, plainTitle());
	}

	public final void render(Inventory inv) {
		fancy = s.fancy();
		for (int i = 9; i < 45; i++) inv.setItem(i, deco("felt"));
		bulbs(inv);
		controls(inv);
		draw(inv);
	}

	public final void click(int slot, boolean right) {
		if (slot >= 45) {
			int c = slot - 45;
			if (c == 0) {
				if (busy()) {
					Fx.error(p);
					return;
				}
				Fx.click(p);
				Screen back = backTarget();
				if (back == null) p.closeInventory();
				else s.switchTo(back);
				return;
			}
			if (c == 7) {
				if (busy()) {
					Fx.error(p);
					return;
				}
				Double custom = pl.customBet(p);
				if (!right && custom != null && s.betIndex() != 5) {
					s.setBetIndex(5);
					Fx.chip(p);
				} else {
					Fx.click(p);
					s.askBet();
				}
				return;
			}
			if (c >= 2 && c <= 6) {
				if (busy()) {
					Fx.error(p);
					return;
				}
				s.setBetIndex(c - 2);
				Fx.chip(p);
				return;
			}
		}
		onClick(slot, right);
	}

	// ------------------------------------------------------------------ Bausteine

	protected ItemStack deco(String model) { return Tiles.deco(model, fancy); }

	protected ItemStack item(String model, String name, String... lore) {
		return Tiles.item(model, fancy, name, lore);
	}

	protected ItemStack item(String model, boolean glint, String name, String... lore) {
		return Tiles.item(model, fancy, glint, name, lore);
	}

	protected static int slot(int row, int col) { return row * 9 + col; }

	private void bulbs(Inventory inv) {
		long t = s.tick();
		boolean party = s.party();
		for (int c = 0; c < 9; c++) {
			String m;
			if (party) m = "bulb_c" + (int) ((c + t / 2) % 4);
			else m = ((c + t / 4) % 3 == 0) ? "bulb_on" : "bulb_off";
			inv.setItem(c, deco(m));
		}
	}

	private void controls(Inventory inv) {
		boolean lobby = isLobby();
		inv.setItem(45, lobby
				? item("btn_close", "§cCasino verlassen")
				: item("btn_back", busy() ? "§7Zurück §8(Runde läuft)" : "§eZurück"));
		String[] inf = info();
		inv.setItem(46, inf.length == 0 ? deco("panel") : item("btn_info", "§bSpielregeln", inf));
		double[] chips = pl.chips();
		int sel = s.betIndex();
		double bal = pl.balance(p);
		for (int i = 0; i < 5; i++) {
			String state = i == sel ? "§aAusgewählt" : (busy() ? "§8Runde läuft…" : "§7Klicken zum Auswählen");
			String name = (i == sel ? "§6▶ " : "§e") + "Einsatz: " + pl.money(chips[i]);
			String model = i == sel ? "chip_" + i + "_sel" : "chip_" + i;
			inv.setItem(47 + i, bal >= chips[i]
					? item(model, i == sel && !fancy, name, state)
					: item(model, i == sel && !fancy, name, state, "§cDafür reicht dein Guthaben nicht"));
		}
		Double custom = pl.customBet(p);
		boolean customSel = sel == 5;
		if (custom == null) {
			inv.setItem(52, item("chip_5", "§6Eigener Einsatz", "§7Klicken und Betrag im Chat eingeben", "§7z. B. §e2500§7, §e100k §7oder §e1.5m"));
		} else {
			String state = customSel ? "§aAusgewählt" : (busy() ? "§8Runde läuft…" : "§7Links: auswählen");
			String cname = (customSel ? "§6▶ " : "§e") + "Eigener Einsatz: " + pl.money(custom);
			String cmodel = customSel ? "chip_5_sel" : "chip_5";
			inv.setItem(52, bal >= custom
					? item(cmodel, customSel && !fancy, cname, state, "§7Rechts: neuen Betrag eingeben")
					: item(cmodel, customSel && !fancy, cname, state, "§7Rechts: neuen Betrag eingeben", "§cDafür reicht dein Guthaben nicht"));
		}
		inv.setItem(53, item("coin", "§6Guthaben: §e" + pl.money(bal)));
	}

	// ------------------------------------------------------------------ LED-Anzeige

	/** Schreibt Text rechtsbündig in LED-Ziffern. color: 'a' = bernstein, 'g' = grün, 'r' = rot */
	protected void led(Inventory inv, int startSlot, int width, String text, char color, String name, String... lore) {
		String t = text.length() > width ? text.substring(text.length() - width) : text;
		while (t.length() < width) t = " " + t;
		for (int i = 0; i < width; i++) {
			char ch = t.charAt(i);
			String key = switch (ch) {
				case '.' -> "dot";
				case 'x' -> "x";
				case 'k', 'K' -> "k";
				case 'm', 'M' -> "m";
				case '+' -> "plus";
				case '-' -> "minus";
				case ' ' -> "blank";
				default -> Character.isDigit(ch) ? String.valueOf(ch) : "blank";
			};
			inv.setItem(startSlot + i, item("led_" + color + "_" + key, name, lore));
		}
	}

	/** Kompakte Zahl für LED-Anzeigen (max. width Zeichen). */
	protected static String compact(double v, int width) {
		long l = (long) Math.floor(v);
		String s = Long.toString(l);
		if (s.length() <= width) return s;
		String[] suf = {"k", "m"};
		double d = l;
		for (String u : suf) {
			d /= 1000;
			String r = d >= 100 ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d);
			if (r.endsWith(".0")) r = r.substring(0, r.length() - 2);
			if ((r + u).length() <= width) return r + u;
		}
		return "9".repeat(width);
	}

	/** 5 Kacheln breites Banner (ban_<name>_0..4), sky = Variante für den Crash-Himmel. */
	protected void banner(Inventory inv, int startSlot, String name, String title) {
		for (int i = 0; i < 5; i++) inv.setItem(startSlot + i, item("ban_" + name + "_" + i, title));
	}
}
