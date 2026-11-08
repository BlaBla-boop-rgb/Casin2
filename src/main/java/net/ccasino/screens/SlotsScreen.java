package net.ccasino.screens;

import net.ccasino.Fx;
import net.ccasino.Session;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;

/** Einarmiger Bandit: 3 Walzen (Spalten 3–5), Hebel rechts, LED-Gewinnanzeige. */
public class SlotsScreen extends Screen {
	private static final String[] NAMES = {"§cBeeren", "§4Redstone", "§fEisen", "§6Gold", "§aSmaragd", "§bDiamant", "§dNetherstern"};
	private static final int[] WEIGHT = {30, 24, 18, 12, 8, 5, 2};
	private static final int[] PAY3 = {6, 10, 15, 25, 50, 100, 300};
	private static final double[] PAY2 = {0.5, 1, 1.5, 2, 2, 3, 5};
	private static final int L = 24;

	private final int[][] strip = new int[3][L];
	private final int[] pos = new int[3];
	private final int[] steps = new int[3];
	private final long[] nextMove = new long[3];
	private final int[] result = new int[3];
	private final boolean[] winReel = new boolean[3];
	private boolean spinning;
	private double stake, pendingWin, lastWin = -1;
	private long leverUntil = -1, winUntil = -1;

	public SlotsScreen(Session s) {
		super(s);
		for (int r = 0; r < 3; r++) {
			for (int i = 0; i < L; i++) strip[r][i] = i < 7 ? i : weighted();
			for (int i = L - 1; i > 0; i--) {
				int j = rnd.nextInt(i + 1), t = strip[r][i];
				strip[r][i] = strip[r][j];
				strip[r][j] = t;
			}
			pos[r] = rnd.nextInt(L);
		}
	}

	@Override protected String gui() { return "slots"; }
	@Override protected String plainTitle() { return "§6Craft Casino §8» §dSlots"; }
	@Override public boolean busy() { return spinning; }

	@Override
	protected String[] info() {
		String[] out = new String[NAMES.length + 3];
		out[0] = "§7Gewinnlinie = mittlere Reihe";
		for (int i = NAMES.length - 1, k = 1; i >= 0; i--, k++) {
			out[k] = "§7 3× " + NAMES[i] + " §8→ §e" + PAY3[i] + "x §8| §72× §e" + fmt(PAY2[i]) + "x";
		}
		out[NAMES.length + 1] = "";
		out[NAMES.length + 2] = "§7Hebel oder §cDREH §7klicken";
		return out;
	}

	private static String fmt(double d) {
		return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	private int weighted() {
		int sum = 0;
		for (int w : WEIGHT) sum += w;
		int x = rnd.nextInt(sum);
		for (int i = 0; i < WEIGHT.length; i++) {
			x -= WEIGHT[i];
			if (x < 0) return i;
		}
		return 0;
	}

	private int at(int reel, int row) { return strip[reel][Math.floorMod(pos[reel] + row, L)]; }

	// ------------------------------------------------------------------ Logik

	private void spin() {
		if (spinning) return;
		double b = s.bet();
		if (!pl.take(p, b)) {
			Fx.error(p);
			return;
		}
		stake = b;
		spinning = true;
		leverUntil = s.tick() + 8;
		winUntil = -1;
		Fx.sound(p, Sound.BLOCK_LEVER_CLICK, 0.8f, 0.8f);
		for (int r = 0; r < 3; r++) {
			result[r] = weighted();
			winReel[r] = false;
			int idx;
			do {
				idx = rnd.nextInt(L);
			} while (strip[r][idx] != result[r]);
			int finalPos = Math.floorMod(idx - 1, L);
			// pos wird pro Schritt um 1 verringert (Symbole wandern nach unten)
			steps[r] = Math.floorMod(pos[r] - finalPos, L) + 16;
			while (r > 0 && steps[r] < steps[r - 1] + 8) steps[r] += L;
			nextMove[r] = s.tick() + 3;
		}
		pendingWin = payout();
	}

	private double payout() {
		if (result[0] == result[1] && result[1] == result[2]) return stake * PAY3[result[0]];
		int pair = -1;
		if (result[0] == result[1] || result[0] == result[2]) pair = result[0];
		else if (result[1] == result[2]) pair = result[1];
		return pair >= 0 ? Math.floor(stake * PAY2[pair]) : 0;
	}

	@Override
	public void tick() {
		if (!spinning) return;
		long t = s.tick();
		boolean all = true;
		int lastMoving = -1;
		for (int r = 0; r < 3; r++) if (steps[r] > 0) lastMoving = r;
		for (int r = 0; r < 3; r++) {
			if (steps[r] <= 0) continue;
			all = false;
			if (t < nextMove[r]) continue;
			pos[r] = Math.floorMod(pos[r] - 1, L);
			steps[r]--;
			int left = steps[r];
			nextMove[r] = t + (left > 6 ? 1 : 7 - left);
			if (left == 0) {
				Fx.thunk(p);
				Fx.tick(p, 0.7f + r * 0.25f);
			} else if (r == lastMoving && (left <= 6 || t % 2 == 0)) {
				Fx.tick(p, 1.4f + (6 - Math.min(6, left)) * 0.06f);
			}
		}
		if (all) finish();
	}

	private void finish() {
		spinning = false;
		lastWin = pendingWin;
		if (pendingWin > 0) {
			boolean three = result[0] == result[1] && result[1] == result[2];
			int pairSym = (result[0] == result[1] || result[0] == result[2]) ? result[0] : result[1];
			for (int r = 0; r < 3; r++) winReel[r] = result[r] == pairSym;
			pl.give(p, pendingWin);
			winUntil = s.tick() + 60;
			boolean big = pendingWin >= stake * 10;
			s.party(big ? 100 : 40);
			Fx.win(p, big);
			if (three && result[0] == 6) p.sendActionBar(net.ccasino.Tiles.text("§6§l✦ JACKPOT ✦ §e+" + pl.money(pendingWin)));
		} else {
			Fx.lose(p);
		}
	}

	@Override
	public void forceFinish() {
		if (!spinning) return;
		spinning = false;
		if (pendingWin > 0) pl.give(p, pendingWin);
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	protected void draw(Inventory inv) {
		long t = s.tick();
		boolean winning = t < winUntil;
		boolean blink = winning && (t / 4) % 2 == 0;

		// Gehäuse
		for (int r = 1; r <= 4; r++) {
			inv.setItem(slot(r, 1), deco(r == 4 ? "cab_dots" : "cab"));
			inv.setItem(slot(r, 2), deco("cab"));
			inv.setItem(slot(r, 6), deco("cab"));
		}
		inv.setItem(slot(4, 3), deco("cab"));
		inv.setItem(slot(4, 4), deco("cab"));
		inv.setItem(slot(4, 5), deco("cab"));
		inv.setItem(slot(2, 2), deco(blink ? "cab_arrow_l_on" : "cab_arrow_l"));
		inv.setItem(slot(2, 6), deco(blink ? "cab_arrow_r_on" : "cab_arrow_r"));
		inv.setItem(slot(1, 0), item("paytable", "§eGewinntabelle", info()));

		// Walzen
		for (int r = 0; r < 3; r++) {
			for (int row = 0; row < 3; row++) {
				int sym = at(r, row);
				boolean hl = row == 1 && winning && winReel[r] && blink;
				String lore = row == 1 && !spinning ? "§7Gewinnlinie" : "";
				inv.setItem(slot(1 + row, 3 + r), lore.isEmpty()
						? item("sym_" + sym + (hl ? "_win" : ""), NAMES[sym])
						: item("sym_" + sym + (hl ? "_win" : ""), NAMES[sym], lore));
			}
		}

		// Hebel
		boolean down = t < leverUntil;
		inv.setItem(slot(1, 7), item(down ? "lever_top_empty" : "lever_top", "§cHebel ziehen", "§7Dreht die Walzen", "§7Einsatz: §e" + pl.money(s.bet())));
		inv.setItem(slot(2, 7), item(down ? "lever_mid_knob" : "lever_mid", "§cHebel ziehen"));
		inv.setItem(slot(3, 7), item("lever_base", "§cHebel ziehen"));

		// LED: Gewinn oder Einsatz
		if (lastWin > 0 && !spinning) {
			led(inv, slot(4, 3), 3, compact(lastWin, 3), 'g', "§aLetzter Gewinn: §e" + pl.money(lastWin));
		} else {
			led(inv, slot(4, 3), 3, compact(s.bet(), 3), lastWin == 0 && !spinning ? 'r' : 'a',
					"§7Einsatz: §e" + pl.money(s.bet()));
		}

		// Dreh-Knopf
		inv.setItem(slot(4, 7), spinning
				? item("btn_spin_felt_off", "§7Dreht …")
				: item("btn_spin_felt", "§c§lDREHEN", "§7Einsatz: §e" + pl.money(s.bet())));
	}

	@Override
	protected void onClick(int slot, boolean right) {
		int row = slot / 9, col = slot % 9;
		if ((col == 7 && row >= 1 && row <= 4)) spin();
	}
}
