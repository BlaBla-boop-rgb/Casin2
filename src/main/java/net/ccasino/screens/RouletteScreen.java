package net.ccasino.screens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.ccasino.Fx;
import net.ccasino.Session;
import net.ccasino.Tiles;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;

/**
 * Roulette: Reihe 2 ist ein Band aus Kesselzahlen, das unter dem Zeiger
 * durchläuft und ausrollt. Reihen 3–4 sind das Setzfeld.
 */
public class RouletteScreen extends Screen {
	static final int[] WHEEL = {0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5,
			24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26};

	private record Bet(String key, int row, int col, String model, String name, String covers, int mult) {}

	private static final List<Bet> BETS = List.of(
			new Bet("red", 3, 0, "bet_red", "§c§lRot", "alle roten Zahlen", 2),
			new Bet("black", 3, 1, "bet_black", "§8§lSchwarz", "alle schwarzen Zahlen", 2),
			new Bet("even", 3, 2, "bet_even", "§fGerade", "2, 4, 6 … 36", 2),
			new Bet("odd", 3, 3, "bet_odd", "§fUngerade", "1, 3, 5 … 35", 2),
			new Bet("low", 3, 4, "bet_low", "§f1–18", "die Zahlen 1 bis 18", 2),
			new Bet("high", 3, 5, "bet_high", "§f19–36", "die Zahlen 19 bis 36", 2),
			new Bet("dz:0", 3, 6, "bet_dz1", "§f1. Dutzend", "1 bis 12", 3),
			new Bet("dz:1", 3, 7, "bet_dz2", "§f2. Dutzend", "13 bis 24", 3),
			new Bet("dz:2", 3, 8, "bet_dz3", "§f3. Dutzend", "25 bis 36", 3),
			new Bet("n:0", 4, 0, "bet_zero", "§a§lZero (0)", "nur die 0", 36));

	final Map<String, Double> bets = new LinkedHashMap<>();
	private Map<String, Double> lastBets = new LinkedHashMap<>();
	private final List<Integer> history = new ArrayList<>();
	private int wpos = rnd.nextInt(37);
	private boolean spinning;
	private int stepsLeft;
	private long nextStep;
	private int result = -1;
	private double stakeTotal, lastWin = -1;
	private long resultUntil = -1;

	public RouletteScreen(Session s) {
		super(s);
	}

	@Override protected String gui() { return "roulette"; }
	@Override protected String plainTitle() { return "§6Craft Casino §8» §cRoulette"; }
	@Override public boolean busy() { return spinning; }

	@Override
	protected String[] info() {
		return new String[] {
				"§7Links-Klick aufs Feld: §e+Einsatz",
				"§7Rechts-Klick: §cEinsatz zurücknehmen",
				"",
				"§7Einzelne Zahl §8→ §e36x",
				"§7Dutzend §8→ §e3x",
				"§7Rot/Schwarz, Gerade/Ungerade,",
				"§71–18 / 19–36 §8→ §e2x",
				"§7Bei §a0 §7verlieren alle Außenwetten."};
	}

	// ------------------------------------------------------------------ Logik

	static boolean covers(String key, int n) {
		if (key.startsWith("n:")) return Integer.parseInt(key.substring(2)) == n;
		if (key.startsWith("dz:")) return n >= 1 && (n - 1) / 12 == Integer.parseInt(key.substring(3));
		if (n == 0) return false;
		return switch (key) {
			case "low" -> n <= 18;
			case "high" -> n >= 19;
			case "even" -> n % 2 == 0;
			case "odd" -> n % 2 == 1;
			case "red" -> Tiles.isRed(n);
			case "black" -> !Tiles.isRed(n);
			default -> false;
		};
	}

	static int mult(String key) {
		if (key.startsWith("n:")) return 36;
		if (key.startsWith("dz:")) return 3;
		return 2;
	}

	double total() {
		double t = 0;
		for (double v : bets.values()) t += v;
		return t;
	}

	void place(String key) {
		if (spinning) return;
		double b = s.bet();
		if (!pl.take(p, b)) {
			Fx.error(p);
			return;
		}
		bets.merge(key, b, Double::sum);
		resultUntil = -1;
		Fx.chip(p);
	}

	void remove(String key) {
		if (spinning) return;
		Double v = bets.remove(key);
		if (v != null) {
			pl.give(p, v);
			Fx.tick(p, 1.2f);
		}
	}

	private void clear() {
		for (double v : bets.values()) pl.give(p, v);
		bets.clear();
	}

	private void repeat() {
		double sum = 0;
		for (double v : lastBets.values()) sum += v;
		if (!pl.take(p, sum)) {
			Fx.error(p);
			return;
		}
		bets.putAll(lastBets);
		Fx.chip(p);
	}

	private void spin() {
		if (spinning || bets.isEmpty()) return;
		spinning = true;
		stakeTotal = total();
		result = rnd.nextInt(37);
		int idx = 0;
		for (int i = 0; i < 37; i++) if (WHEEL[i] == result) idx = i;
		int target = Math.floorMod(idx - 4, 37); // Zahl soll unter dem Zeiger (Spalte 4) landen
		stepsLeft = Math.floorMod(target - wpos, 37) + 37;
		nextStep = s.tick() + 2;
		resultUntil = -1;
		Fx.sound(p, Sound.BLOCK_NOTE_BLOCK_SNARE, 0.7f, 0.7f);
	}

	@Override
	public void tick() {
		if (!spinning || s.tick() < nextStep) return;
		wpos = (wpos + 1) % 37;
		stepsLeft--;
		int iv = stepsLeft > 20 ? 1 : 1 + (20 - stepsLeft) / 5;
		nextStep = s.tick() + iv;
		Fx.tick(p, 1.2f + Math.max(0, 20 - stepsLeft) * 0.03f);
		if (stepsLeft <= 0) settle();
	}

	private void settle() {
		spinning = false;
		Fx.thunk(p);
		history.add(0, result);
		if (history.size() > 4) history.remove(history.size() - 1);
		double win = 0;
		for (Map.Entry<String, Double> e : bets.entrySet()) {
			if (covers(e.getKey(), result)) win += e.getValue() * mult(e.getKey());
		}
		lastBets = new LinkedHashMap<>(bets);
		bets.clear();
		lastWin = win;
		resultUntil = s.tick() + 80;
		String color = result == 0 ? "§aGrün" : Tiles.isRed(result) ? "§cRot" : "§8Schwarz";
		if (win > 0) {
			pl.give(p, win);
			s.party(win >= stakeTotal * 10 ? 100 : 40);
			Fx.win(p, win >= stakeTotal * 10);
			p.sendActionBar(Tiles.text("§e" + result + " " + color + " §8» §aGewinn +" + pl.money(win)));
		} else {
			Fx.lose(p);
			p.sendActionBar(Tiles.text("§e" + result + " " + color + " §8» §7leider verloren"));
		}
	}

	@Override
	public void forceFinish() {
		if (spinning) {
			int idx = 0;
			for (int i = 0; i < 37; i++) if (WHEEL[i] == result) idx = i;
			wpos = Math.floorMod(idx - 4, 37);
			settle();
		} else if (!bets.isEmpty()) {
			clear(); // nicht gedrehte Einsätze zurückgeben
		}
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	protected void draw(Inventory inv) {
		boolean showResult = !spinning && s.tick() < resultUntil;
		boolean blink = (s.tick() / 4) % 2 == 0;

		// Verlauf (links) + Zeiger + LED
		for (int i = 0; i < 4; i++) {
			int col = 3 - i;
			if (i < history.size()) {
				int n = history.get(i);
				inv.setItem(slot(1, col), item("rn_" + n, numName(n), i == 0 ? "§7Letzte Zahl" : "§7vor " + i + " Runden"));
			} else {
				inv.setItem(slot(1, col), deco("felt"));
			}
		}
		inv.setItem(slot(1, 4), deco("pointer"));
		if (showResult && lastWin > 0) {
			led(inv, slot(1, 5), 4, compact(lastWin, 4), 'g', "§aGewinn: §e" + pl.money(lastWin));
		} else {
			double t = total();
			led(inv, slot(1, 5), 4, compact(t, 4), showResult ? 'r' : 'a', "§7Gesetzt: §e" + pl.money(t));
		}

		// Kesselband
		for (int c = 0; c < 9; c++) {
			int n = WHEEL[(wpos + c) % 37];
			boolean hl = c == 4 && (spinning || (showResult && blink));
			inv.setItem(slot(2, c), item("rn_" + n + (hl ? "_hl" : ""), numName(n)));
		}

		// Setzfeld
		for (Bet b : BETS) {
			Double v = bets.get(b.key);
			boolean won = showResult && result >= 0 && covers(b.key, result);
			String[] lore = v == null
					? new String[] {"§7Deckt " + b.covers, "§7Auszahlung §e" + b.mult + "x", "", "§eLinks: §7+" + pl.money(s.bet()), "§eRechts: §7zurücknehmen"}
					: new String[] {"§7Deckt " + b.covers, "§7Auszahlung §e" + b.mult + "x", "", "§6Gesetzt: §e" + pl.money(v), "§eLinks: §7+" + pl.money(s.bet()), "§eRechts: §7zurücknehmen"};
			inv.setItem(slot(b.row, b.col), item(b.model, v != null || (won && blink), b.name, lore));
		}
		long singles = bets.keySet().stream().filter(k -> k.startsWith("n:") && !k.equals("n:0")).count();
		inv.setItem(slot(4, 1), item("bet_pick", singles > 0, "§9§lEinzelne Zahl",
				"§7Öffnet das Zahlenfeld 1–36", "§7Auszahlung §e36x",
				singles > 0 ? "§6" + singles + " Zahl(en) gesetzt" : "§8Noch keine Zahl gesetzt"));

		// Knöpfe
		if (!spinning && bets.isEmpty() && !lastBets.isEmpty()) {
			inv.setItem(slot(4, 5), item("btn_repeat", "§9Nochmal setzen", "§7Setzt deine letzten Einsätze erneut"));
		} else {
			inv.setItem(slot(4, 5), deco("felt"));
		}
		inv.setItem(slot(4, 6), !spinning && !bets.isEmpty()
				? item("btn_clear", "§7Einsätze zurücknehmen")
				: deco("felt"));
		inv.setItem(slot(4, 7), !spinning && !bets.isEmpty()
				? item("btn_spin_felt", "§c§lDREHEN", "§7Gesetzt: §e" + pl.money(total()))
				: item("btn_spin_felt_off", spinning ? "§7Kugel rollt …" : "§7Erst setzen, dann drehen"));
	}

	static String numName(int n) {
		String c = n == 0 ? "§a" : Tiles.isRed(n) ? "§c" : "§8";
		return c + "§l" + n;
	}

	@Override
	protected void onClick(int slot, boolean right) {
		if (spinning) return;
		int row = slot / 9, col = slot % 9;
		for (Bet b : BETS) {
			if (b.row == row && b.col == col) {
				if (right) remove(b.key);
				else place(b.key);
				return;
			}
		}
		if (row == 4 && col == 1) {
			Fx.click(p);
			s.keep(this, true);
			s.switchTo(new NumberPickerScreen(s, this));
		} else if (row == 4 && col == 5 && bets.isEmpty() && !lastBets.isEmpty()) {
			repeat();
		} else if (row == 4 && col == 6 && !bets.isEmpty()) {
			Fx.click(p);
			clear();
		} else if (row == 4 && col == 7) {
			spin();
		}
	}
}
