package net.ccasino.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.ccasino.Fx;
import net.ccasino.Session;
import net.ccasino.Tiles;
import org.bukkit.inventory.Inventory;

/**
 * Crash: links (Spalten 0–3) fliegt die Rakete durch den Sternenhimmel,
 * rechts zeigen LED-Anzeigen Multiplikator und möglichen Gewinn.
 */
public class CrashScreen extends Screen {
	private static final double K = 0.16; // Wachstum pro Sekunde (2x nach ~4,3 s)

	private boolean running, crashed;
	private long startTick, crashTick = -100;
	private double crashPoint, cashedAt, stake;
	private int lastWhole = 1;
	private double starScroll;
	private final List<Double> history = new ArrayList<>();

	public CrashScreen(Session s) {
		super(s);
	}

	@Override protected String gui() { return "crash"; }
	@Override protected String plainTitle() { return "§6Craft Casino §8» §6Crash"; }
	@Override public boolean busy() { return running; }

	@Override
	protected String[] info() {
		return new String[] {"§7Nach dem Start steigt der Multiplikator.", "§7Klick §aCASH§7, um auszuzahlen:",
				"§7Einsatz × aktueller Multiplikator.", "", "§7Explodiert die Rakete vorher,", "§7ist der Einsatz weg.",
				"", "§8Hausvorteil 3%, max. " + fmt(maxMult()) + "x"};
	}

	private double maxMult() { return Math.max(1.5, pl.getConfig().getDouble("crash-max", 50.0)); }

	private static String fmt(double m) {
		return m >= 10 ? String.format(Locale.ROOT, "%.1f", m) : String.format(Locale.ROOT, "%.2f", m);
	}

	private double mult() {
		if (!running && !crashed) return 1.0;
		long end = running ? s.tick() : crashTick;
		double m = Math.exp(K * (end - startTick) / 20.0);
		return Math.min(m, crashPoint);
	}

	// ------------------------------------------------------------------ Logik

	private void launch() {
		if (running) return;
		double b = s.bet();
		if (!pl.take(p, b)) {
			Fx.error(p);
			return;
		}
		stake = b;
		double u = rnd.nextDouble();
		crashPoint = Math.max(1.0, Math.floor(0.97 / (1 - u) * 100) / 100.0);
		crashPoint = Math.min(crashPoint, maxMult());
		running = true;
		crashed = false;
		cashedAt = 0;
		lastWhole = 1;
		startTick = s.tick();
		Fx.sound(p, org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
	}

	private void cashOut() {
		if (!running || cashedAt > 0) return;
		double m = mult();
		cashedAt = m;
		double win = Math.floor(stake * m);
		pl.give(p, win);
		s.party(m >= 10 ? 100 : 40);
		Fx.win(p, m >= 10);
		p.sendActionBar(Tiles.text("§aAusgezahlt bei §e" + fmt(m) + "x §8» §a+" + pl.money(win)));
	}

	private void crash() {
		running = false;
		crashed = true;
		crashTick = startTick + (long) Math.ceil(Math.log(crashPoint) / K * 20);
		history.add(0, crashPoint);
		if (history.size() > 5) history.remove(history.size() - 1);
		Fx.explode(p);
		if (cashedAt <= 0) {
			Fx.lose(p);
			p.sendActionBar(Tiles.text("§c§lCRASH §7bei §e" + fmt(crashPoint) + "x §8» §c-" + pl.money(stake)));
		}
	}

	@Override
	public void tick() {
		if (!running) return;
		double m = Math.exp(K * (s.tick() - startTick) / 20.0);
		if (m >= crashPoint) {
			crash();
			return;
		}
		if ((int) m > lastWhole) {
			lastWhole = (int) m;
			Fx.bell(p, Math.min(2f, 0.8f + lastWhole * 0.12f));
		}
		starScroll += 0.25 + Math.min(1.5, (m - 1) * 0.35);
	}

	@Override
	public void forceFinish() {
		running = false; // nicht ausgezahlt = verloren (Einsatz wurde schon abgezogen)
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	protected void draw(Inventory inv) {
		long t = s.tick();
		double m = mult();
		int scroll = (int) starScroll;

		// Himmel mit scrollenden Sternen
		for (int r = 1; r <= 4; r++) {
			for (int c = 0; c < 4; c++) {
				int h = Math.floorMod((r - scroll) * 31 + c * 17, 11);
				String sky = h < 4 ? "sky_s" + h : "sky";
				inv.setItem(slot(r, c), deco(sky));
			}
		}

		// Rakete: steigt bis Reihe 1, dann "fliegt" sie durch die Sterne
		int row = m < 1.2 ? 4 : m < 1.5 ? 3 : m < 2.0 ? 2 : 1;
		if (crashed && t - crashTick < 30) {
			inv.setItem(slot(row, 1), item("boom", "§c§lBOOM!", "§7Crash bei §e" + fmt(crashPoint) + "x"));
		} else if (running) {
			inv.setItem(slot(row, 1), item("rocket_" + (t / 2) % 2, "§6" + fmt(m) + "x"));
			if (row < 4) inv.setItem(slot(row + 1, 1), deco("flame_" + (t / 2) % 2));
		} else {
			inv.setItem(slot(4, 1), item("rocket_0", "§7Bereit zum Start"));
		}

		// Rechte Seite: Anzeigen
		for (int r = 1; r <= 4; r++) for (int c = 4; c < 9; c++) inv.setItem(slot(r, c), deco("panel"));
		char col = crashed && cashedAt <= 0 ? 'r' : (cashedAt > 0 ? 'g' : 'a');
		String multText = fmt(m) + "x";
		if (crashed) led(inv, slot(1, 4), 5, multText, col, "§7Multiplikator: §e" + fmt(m) + "x", "§cExplodiert bei " + fmt(crashPoint) + "x");
		else led(inv, slot(1, 4), 5, multText, col, "§7Multiplikator: §e" + fmt(m) + "x");

		double shown;
		char wc;
		String wname;
		if (cashedAt > 0) {
			shown = Math.floor(stake * cashedAt);
			wc = 'g';
			wname = "§aAusgezahlt: §e" + pl.money(shown);
		} else if (running) {
			shown = Math.floor(stake * m);
			wc = 'a';
			wname = "§7Möglicher Gewinn: §e" + pl.money(shown);
		} else {
			shown = s.bet();
			wc = crashed ? 'r' : 'a';
			wname = "§7Einsatz: §e" + pl.money(shown);
		}
		led(inv, slot(2, 4), 5, compact(shown, 5), wc, wname);

		// Verlauf
		for (int i = 0; i < 5; i++) {
			if (i < history.size()) {
				double v = history.get(i);
				String model = v >= 10 ? "hist_gold" : v >= 2 ? "hist_green" : "hist_red";
				inv.setItem(slot(3, 4 + i), item(model, (v >= 2 ? "§a" : "§c") + fmt(v) + "x", i == 0 ? "§7Letzter Crash" : "§7vor " + i + " Runden"));
			} else {
				inv.setItem(slot(3, 4 + i), deco("hist_none"));
			}
		}

		// Großer Knopf (3 Kacheln)
		String wb;
		String name;
		String lore;
		if (!running) {
			wb = "wb_launch";
			name = "§6§lSTARTEN";
			lore = "§7Einsatz: §e" + pl.money(s.bet());
		} else if (cashedAt <= 0) {
			wb = (t / 4) % 2 == 0 ? "wb_cash" : "wb_cash2";
			name = "§a§lAUSZAHLEN §7(" + fmt(m) + "x)";
			lore = "§7Jetzt: §a+" + pl.money(Math.floor(stake * m));
		} else {
			wb = "wb_wait";
			name = "§7Ausgezahlt – warte auf die Explosion …";
			lore = "§7Ausgezahlt bei §e" + fmt(cashedAt) + "x";
		}
		for (int i = 0; i < 3; i++) inv.setItem(slot(4, 5 + i), item(wb + "_" + i, name, lore));
	}

	@Override
	protected void onClick(int slot, boolean right) {
		int row = slot / 9, col = slot % 9;
		if (row == 4 && col >= 5 && col <= 7) {
			if (!running) launch();
			else cashOut();
		}
	}
}
